package dev.cursorlink;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

public final class CursorLinkHud {
	private CursorLinkHud() {
	}

	public static void render(GuiGraphicsExtractor graphics, DeltaTracker delta) {
		Minecraft client = Minecraft.getInstance();
		if (client.gui.screen() instanceof CursorLinkScreen) {
			return;
		}
		BridgeSnapshot snapshot = BridgeClient.get().snapshot();
		if (!snapshot.isRunning()) {
			return;
		}
		Component label = Component.translatable("cursorlink.hud.working");
		int width = client.font.width(label) + 12;
		graphics.fill(6, 6, 6 + width, 22, 0xC0182438);
		graphics.text(client.font, label, 12, 10, 0xFFB8D4FF, false);
	}
}
