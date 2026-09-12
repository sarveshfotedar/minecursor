package dev.cursorlink;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class CursorLinkClient implements ClientModInitializer {
	private final KeyMapping.Category category = KeyMapping.Category.register(CursorLink.id("main"));
	private final KeyMapping openKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
		"key.cursorlink.open",
		InputConstants.Type.KEYSYM,
		InputConstants.KEY_K,
		this.category
	));

	@Override
	public void onInitializeClient() {
		ModConfig.load();
		HudElementRegistry.attachElementBefore(
			VanillaHudElements.CHAT,
			CursorLink.id("status"),
			CursorLinkHud::render
		);

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (this.openKey.consumeClick()) {
				openScreen(client);
			}
			BridgeClient.get().tick(client);
		});

		CursorLink.LOGGER.info("Cursor Link ready. Press K in-game, or rebind it in Controls.");
	}

	static void openScreen(Minecraft client) {
		if (client.gui.screen() instanceof CursorLinkScreen) {
			return;
		}
		client.gui.setScreen(new CursorLinkScreen(Component.translatable("cursorlink.screen.title"), client.gui.screen()));
	}
}
