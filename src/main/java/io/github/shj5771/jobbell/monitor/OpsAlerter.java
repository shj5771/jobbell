package io.github.shj5771.jobbell.monitor;

/**
 * 운영자에게 알린다. 사용자 채널이 아니라 운영 전용 채널로 간다.
 *
 * <p>같은 {@code key} 의 경보는 {@link #resolve} 될 때까지 한 번만 보낸다. 10분마다 도는 수집이
 * 같은 실패를 10분마다 알리면 운영 채널이 금방 무시되는 채널이 된다.
 */
public interface OpsAlerter {

    void alert(String key, String message);

    /** 문제가 풀렸다. 경보를 보낸 적이 있으면 복구 알림을 보낸다. */
    void resolve(String key, String message);
}
