package dev.cursorlink;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ModConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static String bridgeUrl = "http://127.0.0.1:43147";

	private ModConfig() {
	}

	public static String bridgeUrl() {
		return bridgeUrl;
	}

	public static void load() {
		Path path = FabricLoader.getInstance().getConfigDir().resolve("cursorlink.json");
		try {
			if (Files.notExists(path)) {
				JsonObject generated = new JsonObject();
				generated.addProperty("bridgeUrl", bridgeUrl);
				Files.writeString(path, GSON.toJson(generated), StandardCharsets.UTF_8);
				return;
			}
			JsonObject json = GSON.fromJson(Files.readString(path, StandardCharsets.UTF_8), JsonObject.class);
			if (json != null && json.has("bridgeUrl")) {
				bridgeUrl = json.get("bridgeUrl").getAsString();
			}
		} catch (IOException exception) {
			CursorLink.LOGGER.warn("Could not read Cursor Link config, using {}", bridgeUrl, exception);
		}
	}
}
