export type EventType = "user" | "assistant" | "status" | "tool" | "error" | "graphic";

export interface TranscriptEvent {
  id: number;
  type: EventType;
  text: string;
  at: number;
}

export interface SessionState {
  status: "idle" | "running" | "error";
  hasApiKey: boolean;
  workspace: string;
  error: string | null;
  events: TranscriptEvent[];
}
