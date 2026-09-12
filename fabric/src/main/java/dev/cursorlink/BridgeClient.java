package dev.cursorlink;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public final class BridgeClient {
	private static final BridgeClient INSTANCE = new BridgeClient();
	private static final Duration TIMEOUT = Duration.ofSeconds(8);

	private final HttpClient http = HttpClient.newBuilder()
		.connectTimeout(Duration.ofSeconds(2))
		.build();
	private final ExecutorService io = Executors.newSingleThreadExecutor(thread -> {
		Thread created = new Thread(thread, "cursorlink-bridge");
		created.setDaemon(true);
		return created;
	});
	private final AtomicReference<BridgeSnapshot> snapshot = new AtomicReference<>(BridgeSnapshot.offline());
	private final AtomicBoolean requestInFlight = new AtomicBoolean(false);
	private int ticksUntilPoll;

	private BridgeClient() {
	}

	public static BridgeClient get() {
		return INSTANCE;
	}

	public BridgeSnapshot snapshot() {
		return this.snapshot.get();
	}

	public void tick(Minecraft client) {
		if (this.ticksUntilPoll-- > 0) {
			return;
		}
		boolean screenOpen = client.gui.screen() instanceof CursorLinkScreen;
		boolean running = this.snapshot.get().isRunning();
		this.ticksUntilPoll = running || screenOpen ? 8 : 40;
		this.refresh();
	}

	public void refresh() {
		if (!this.requestInFlight.compareAndSet(false, true)) {
			return;
		}
		this.io.execute(() -> {
			try {
				this.snapshot.set(this.fetchSession());
			} catch (Exception exception) {
				this.snapshot.set(BridgeSnapshot.offline());
			} finally {
				this.requestInFlight.set(false);
			}
		});
	}

	public void send(String text) {
		String trimmed = text.strip();
		if (trimmed.isEmpty()) {
			return;
		}
		this.io.execute(() -> {
			try {
				JsonObject body = new JsonObject();
				body.addProperty("text", trimmed);
				this.post("/v1/prompt", body.toString());
				this.snapshot.set(this.fetchSession());
			} catch (Exception exception) {
				this.snapshot.set(new BridgeSnapshot(
					true,
					this.snapshot.get().hasApiKey(),
					this.snapshot.get().workspace(),
					"error",
					exception.getMessage(),
					this.snapshot.get().events()
				));
			}
		});
	}

	private BridgeSnapshot fetchSession() throws Exception {
		String raw = this.get("/v1/session");
		JsonObject json = JsonParser.parseString(raw).getAsJsonObject();
		List<TranscriptEvent> events = new ArrayList<>();
		JsonArray array = json.has("events") ? json.getAsJsonArray("events") : new JsonArray();
		for (JsonElement element : array) {
			JsonObject item = element.getAsJsonObject();
			events.add(new TranscriptEvent(
				item.get("id").getAsInt(),
				item.get("type").getAsString(),
				item.has("text") ? item.get("text").getAsString() : "",
				item.has("at") ? item.get("at").getAsLong() : 0L
			));
		}
		return new BridgeSnapshot(
			true,
			json.has("hasApiKey") && json.get("hasApiKey").getAsBoolean(),
			json.has("workspace") ? json.get("workspace").getAsString() : "",
			json.has("status") ? json.get("status").getAsString() : "idle",
			json.has("error") && !json.get("error").isJsonNull() ? json.get("error").getAsString() : "",
			List.copyOf(events)
		);
	}

	private String get(String path) throws Exception {
		HttpRequest request = HttpRequest.newBuilder(this.uri(path))
			.timeout(TIMEOUT)
			.GET()
			.header("Accept", "application/json")
			.build();
		HttpResponse<String> response = this.http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
		if (response.statusCode() >= 400) {
			throw new IllegalStateException("Helper returned HTTP " + response.statusCode());
		}
		return response.body();
	}

	private void post(String path, String json) throws Exception {
		HttpRequest request = HttpRequest.newBuilder(this.uri(path))
			.timeout(Duration.ofSeconds(20))
			.header("Content-Type", "application/json")
			.POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
			.build();
		HttpResponse<String> response = this.http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
		if (response.statusCode() >= 400) {
			String body = response.body();
			throw new IllegalStateException(body == null || body.isBlank()
				? "Helper returned HTTP " + response.statusCode()
				: body);
		}
	}

	private URI uri(String path) {
		String base = ModConfig.bridgeUrl();
		if (base.endsWith("/")) {
			base = base.substring(0, base.length() - 1);
		}
		return URI.create(base + path);
	}
}
