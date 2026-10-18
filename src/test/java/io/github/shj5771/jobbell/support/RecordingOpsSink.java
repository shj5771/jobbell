package io.github.shj5771.jobbell.support;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** 운영 채널로 나갔을 내용을 모은다. */
public class RecordingOpsSink implements Consumer<String> {

    private final List<String> messages = new ArrayList<>();

    @Override
    public synchronized void accept(String content) {
        messages.add(content);
    }

    public synchronized List<String> messages() {
        return List.copyOf(messages);
    }

    public synchronized void reset() {
        messages.clear();
    }
}
