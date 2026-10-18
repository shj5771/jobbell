package io.github.shj5771.jobbell.discord;

public class DiscordApiException extends RuntimeException {

    private final int status;

    public DiscordApiException(int status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    public int status() {
        return status;
    }
}
