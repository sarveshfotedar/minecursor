package dev.cursorlink;

import java.util.List;

public record BridgeSnapshot(
	boolean reachable,
	boolean hasApiKey,
	String workspace,
	String status,
	String error,
	List<TranscriptEvent> events
) {
	public static BridgeSnapshot offline() {
		return new BridgeSnapshot(false, false, "", "offline", "The helper is not running on this computer.", List.of());
	}

	public boolean isRunning() {
		return "running".equals(this.status);
	}

	public boolean isIdle() {
		return "idle".equals(this.status) || "ready".equals(this.status);
	}
}
