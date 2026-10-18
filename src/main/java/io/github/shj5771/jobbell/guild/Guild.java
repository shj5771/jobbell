package io.github.shj5771.jobbell.guild;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import java.time.Instant;

/** 봇이 들어가 있는 디스코드 서버. 명령이 처음 들어올 때 만들어진다. */
@Entity
public class Guild {

    @Id
    private Long guildId;

    /** null 이면 /채널설정 전이다. 이 상태에서는 어떤 알림도 보내지 않는다. */
    private Long alertChannelId;

    @Column(nullable = false)
    private Instant createdAt;

    protected Guild() {
    }

    public Guild(Long guildId, Instant createdAt) {
        this.guildId = guildId;
        this.createdAt = createdAt;
    }

    public void setAlertChannel(Long channelId) {
        this.alertChannelId = channelId;
    }

    public Long getGuildId() {
        return guildId;
    }

    public Long getAlertChannelId() {
        return alertChannelId;
    }
}
