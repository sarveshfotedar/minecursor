package dev.cursorlink;

public record TranscriptEvent(int id, String type, String text, long at) {
	public boolean isUser() {
		return "user".equals(this.type);
	}

	public boolean isError() {
		return "error".equals(this.type);
	}

	public boolean isStatus() {
		return "status".equals(this.type) || "tool".equals(this.type);
	}
}
