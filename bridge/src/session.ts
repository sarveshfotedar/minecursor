import { Agent } from "@cursor/sdk";
import type { EventType, SessionState, TranscriptEvent } from "./types.js";

type ActiveRun = {
  cancel: () => Promise<void>;
};

export class CursorSession {
  private nextId = 1;
  private events: TranscriptEvent[] = [];
  private status: SessionState["status"] = "idle";
  private error: string | null = null;
  private agent: Awaited<ReturnType<typeof Agent.create>> | null = null;
  private run: ActiveRun | null = null;

  constructor(
    private readonly apiKey: string | undefined,
    private readonly workspace: string,
    private readonly model: string
  ) {}

  snapshot(): SessionState {
    return {
      status: this.status,
      hasApiKey: Boolean(this.apiKey),
      workspace: this.workspace,
      error: this.error,
      events: this.events
    };
  }

  async prompt(text: string): Promise<void> {
    const trimmed = text.trim();
    if (!trimmed) {
      throw new Error("Type a message first.");
    }
    if (this.status === "running") {
      throw new Error("Cursor is still working on the last message.");
    }
    if (!this.apiKey) {
      throw new Error("The helper has no CURSOR_API_KEY. Add one and restart it.");
    }

    this.error = null;
    this.status = "running";
    this.push("user", trimmed);

    try {
      if (!this.agent) {
        this.agent = await Agent.create({
          apiKey: this.apiKey,
          model: { id: this.model },
          local: { cwd: this.workspace }
        });
      }

      const assistantId = this.reserve("assistant");
      const run = await this.agent.send(trimmed, {
        onDelta: ({ update }) => {
          if (update.type === "text-delta") {
            this.append(assistantId, update.text);
          }
        }
      });
      this.run = { cancel: () => run.cancel() };

      for await (const event of run.stream()) {
        if (event.type === "tool_call") {
          const label = event.status === "running"
            ? `Using ${event.name}...`
            : event.status === "error"
              ? `${event.name} failed`
              : `Finished ${event.name}`;
          this.push("tool", label);
        } else if (event.type === "assistant") {
          const text = event.message.content
            .filter((block) => block.type === "text")
            .map((block) => ("text" in block ? block.text : ""))
            .join("");
          if (text) {
            this.replace(assistantId, text);
          }
        }
      }

      const result = await run.wait();
      if (result.status === "error") {
        this.error = result.error?.message ?? "Cursor reported an error.";
        this.push("error", this.error);
        this.status = "error";
        return;
      }
      if (result.status === "cancelled") {
        this.push("status", "Stopped.");
      } else if (result.result) {
        this.replace(assistantId, result.result);
      }
      this.status = "idle";
    } catch (error) {
      this.error = error instanceof Error ? error.message : String(error);
      this.push("error", this.error);
      this.status = "error";
    } finally {
      this.run = null;
      if (this.status === "running") {
        this.status = "idle";
      }
    }
  }

  async cancel(): Promise<void> {
    if (this.run) {
      await this.run.cancel();
    }
  }

  private reserve(type: EventType): number {
    const event: TranscriptEvent = {
      id: this.nextId++,
      type,
      text: "",
      at: Date.now()
    };
    this.events.push(event);
    return event.id;
  }

  private push(type: EventType, text: string): void {
    this.events.push({
      id: this.nextId++,
      type,
      text,
      at: Date.now()
    });
  }

  private append(id: number, chunk: string): void {
    const event = this.events.find((item) => item.id === id);
    if (event) {
      event.text += chunk;
    }
  }

  private replace(id: number, text: string): void {
    const event = this.events.find((item) => item.id === id);
    if (event && text) {
      event.text = text;
    }
  }
}
