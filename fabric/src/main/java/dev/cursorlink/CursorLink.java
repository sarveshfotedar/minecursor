package dev.cursorlink;

import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CursorLink {
	public static final String MOD_ID = "cursorlink";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private CursorLink() {
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
