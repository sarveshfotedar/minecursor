package dev.cursorlink;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

public class CursorLinkScreen extends Screen {
	private final Screen parent;
	private EditBox prompt;
	private Button send;

	public CursorLinkScreen(Component title, Screen parent) {
		super(title);
		this.parent = parent;
	}

	@Override
	protected void init() {
		int fieldWidth = Math.min(420, this.width - 40);
		int fieldX = (this.width - fieldWidth) / 2;
		int fieldY = this.height - 36;

		this.prompt = new EditBox(this.font, fieldX, fieldY, fieldWidth - 72, 20, Component.translatable("cursorlink.screen.hint"));
		this.prompt.setMaxLength(4000);
		this.prompt.setHint(Component.translatable("cursorlink.screen.hint"));
		this.addRenderableWidget(this.prompt);

		this.send = Button.builder(Component.translatable("cursorlink.screen.send"), button -> this.sendPrompt())
			.bounds(fieldX + fieldWidth - 68, fieldY, 68, 20)
			.build();
		this.addRenderableWidget(this.send);
		this.setInitialFocus(this.prompt);
		BridgeClient.get().refresh();
	}

	@Override
	public void tick() {
		super.tick();
		boolean busy = BridgeClient.get().snapshot().isRunning();
		if (this.send != null) {
			this.send.active = !busy;
		}
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractRenderState(graphics, mouseX, mouseY, delta);

		int panelWidth = Math.min(460, this.width - 32);
		int panelX = (this.width - panelWidth) / 2;
		int panelY = 28;
		int panelBottom = this.height - 48;

		graphics.fill(panelX - 6, panelY - 18, panelX + panelWidth + 6, panelBottom + 8, 0xD0101828);
		graphics.text(this.font, this.title, panelX, panelY - 14, 0xFFFFFFFF, true);

		BridgeSnapshot snapshot = BridgeClient.get().snapshot();
		graphics.text(this.font, this.statusLine(snapshot), panelX, panelY, this.statusColor(snapshot), false);

		int textTop = panelY + 16;
		int textBottom = panelBottom - 6;
		int maxLines = Math.max(1, (textBottom - textTop) / this.font.lineHeight);
		List<String> lines = this.visibleLines(snapshot, panelWidth - 8, maxLines);
		int y = textTop;
		for (String line : lines) {
			graphics.text(this.font, line, panelX, y, 0xFFE8EEF8, false);
			y += this.font.lineHeight;
		}
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (this.prompt != null && this.prompt.isFocused()
			&& (event.key() == InputConstants.KEY_RETURN || event.key() == InputConstants.KEY_NUMPADENTER)) {
			this.sendPrompt();
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public void onClose() {
		if (this.minecraft != null) {
			this.minecraft.gui.setScreen(this.parent);
		}
	}

	private void sendPrompt() {
		if (this.prompt == null) {
			return;
		}
		String text = this.prompt.getValue().strip();
		if (text.isEmpty() || BridgeClient.get().snapshot().isRunning()) {
			return;
		}
		this.prompt.setValue("");
		BridgeClient.get().send(text);
	}

	private Component statusLine(BridgeSnapshot snapshot) {
		if (!snapshot.reachable()) {
			return Component.translatable("cursorlink.status.offline");
		}
		if (snapshot.isRunning()) {
			return Component.translatable("cursorlink.status.running");
		}
		if ("error".equals(snapshot.status()) && !snapshot.error().isBlank()) {
			return Component.literal(snapshot.error());
		}
		if (!snapshot.hasApiKey()) {
			return Component.literal("Helper is running, but it has no Cursor API key yet.");
		}
		if (!snapshot.workspace().isBlank()) {
			return Component.literal("Ready · " + snapshot.workspace());
		}
		return Component.translatable("cursorlink.status.idle");
	}

	private int statusColor(BridgeSnapshot snapshot) {
		if (!snapshot.reachable() || "error".equals(snapshot.status())) {
			return 0xFFFF8A80;
		}
		if (snapshot.isRunning()) {
			return 0xFF9CDCFF;
		}
		return 0xFFB8F0C8;
	}

	private List<String> visibleLines(BridgeSnapshot snapshot, int maxWidth, int maxLines) {
		List<String> wrapped = new ArrayList<>();
		if (snapshot.events().isEmpty()) {
			Component empty = snapshot.reachable()
				? Component.translatable("cursorlink.empty.ready")
				: Component.translatable("cursorlink.empty.offline");
			wrapped.addAll(this.wrap(empty.getString(), maxWidth));
			return wrapped;
		}
		for (TranscriptEvent event : snapshot.events()) {
			String prefix = switch (event.type()) {
				case "user" -> "You: ";
				case "assistant" -> "Cursor: ";
				case "tool" -> "Tool: ";
				case "error" -> "Error: ";
				default -> "";
			};
			wrapped.addAll(this.wrap(prefix + event.text().replace('\n', ' '), maxWidth));
		}
		int from = Mth.clamp(wrapped.size() - maxLines, 0, wrapped.size());
		return wrapped.subList(from, wrapped.size());
	}

	private List<String> wrap(String text, int maxWidth) {
		List<String> lines = new ArrayList<>();
		if (text.isBlank()) {
			return lines;
		}
		String[] words = text.split(" ");
		StringBuilder current = new StringBuilder();
		for (String word : words) {
			String next = current.isEmpty() ? word : current + " " + word;
			if (this.font.width(next) > maxWidth && !current.isEmpty()) {
				lines.add(current.toString());
				current = new StringBuilder(word);
			} else {
				current = new StringBuilder(next);
			}
		}
		if (!current.isEmpty()) {
			lines.add(current.toString());
		}
		return lines;
	}
}
