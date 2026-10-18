package io.github.shj5771.jobbell.posting.source;

public class PostingSourceException extends RuntimeException {

    public PostingSourceException(String message) {
        super(message);
    }

    public PostingSourceException(String message, Throwable cause) {
        super(message, cause);
    }
}
