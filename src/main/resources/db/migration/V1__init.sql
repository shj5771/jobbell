-- 디스코드 ID(서버·채널·사용자·메시지)는 64비트 snowflake 라 BIGINT 로 담는다.

CREATE TABLE guild
(
    guild_id         BIGINT      NOT NULL,
    -- NULL 이면 /채널설정 전이다. 이 상태에서는 어떤 알림도 보내지 않는다.
    alert_channel_id BIGINT      NULL,
    created_at       DATETIME(6) NOT NULL,
    PRIMARY KEY (guild_id)
);

CREATE TABLE company
(
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    name       VARCHAR(100) NOT NULL,
    source_id  VARCHAR(30)  NOT NULL,
    -- 출처 안에서 이 기업을 가리키는 값(채용 보드 이름, 기업 코드 등)
    source_key VARCHAR(100) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_company_source (source_id, source_key)
);

CREATE TABLE guild_company
(
    guild_id   BIGINT      NOT NULL,
    company_id BIGINT      NOT NULL,
    added_at   DATETIME(6) NOT NULL,
    PRIMARY KEY (guild_id, company_id),
    CONSTRAINT fk_guild_company_guild FOREIGN KEY (guild_id) REFERENCES guild (guild_id),
    CONSTRAINT fk_guild_company_company FOREIGN KEY (company_id) REFERENCES company (id)
);

CREATE TABLE posting
(
    id            BIGINT        NOT NULL AUTO_INCREMENT,
    source_id     VARCHAR(30)   NOT NULL,
    external_id   VARCHAR(100)  NOT NULL,
    company_id    BIGINT        NOT NULL,
    title         VARCHAR(300)  NOT NULL,
    career        VARCHAR(50)   NULL,
    deadline      DATE          NULL,
    url           VARCHAR(1000) NOT NULL,
    first_seen_at DATETIME(6)   NOT NULL,
    PRIMARY KEY (id),
    -- 새 공고 판단 기준. 제목이 바뀌어도 같은 공고다.
    UNIQUE KEY uk_posting_source (source_id, external_id),
    CONSTRAINT fk_posting_company FOREIGN KEY (company_id) REFERENCES company (id)
);

CREATE TABLE member_filter
(
    guild_id BIGINT       NOT NULL,
    user_id  BIGINT       NOT NULL,
    keyword  VARCHAR(100) NOT NULL,
    career   VARCHAR(50)  NULL,
    PRIMARY KEY (guild_id, user_id),
    CONSTRAINT fk_member_filter_guild FOREIGN KEY (guild_id) REFERENCES guild (guild_id)
);

CREATE TABLE notification
(
    id                 BIGINT      NOT NULL AUTO_INCREMENT,
    guild_id           BIGINT      NOT NULL,
    posting_id         BIGINT      NOT NULL,
    -- PENDING 을 먼저 쓰고 보낸 뒤 SENT 로 바꾼다. 보낸 직후 서버가 죽으면 PENDING 이 남는다.
    status             VARCHAR(10) NOT NULL,
    attempts           INT         NOT NULL DEFAULT 0,
    discord_message_id BIGINT      NULL,
    -- 메시지 하단 추적 코드와 이 발송을 만든 로그를 잇는다.
    trace_id           CHAR(32)    NOT NULL,
    created_at         DATETIME(6) NOT NULL,
    sent_at            DATETIME(6) NULL,
    PRIMARY KEY (id),
    -- 중복 발송 방지는 코드가 아니라 이 제약이 책임진다.
    UNIQUE KEY uk_notification_guild_posting (guild_id, posting_id),
    KEY idx_notification_status_created (status, created_at),
    CONSTRAINT fk_notification_guild FOREIGN KEY (guild_id) REFERENCES guild (guild_id),
    CONSTRAINT fk_notification_posting FOREIGN KEY (posting_id) REFERENCES posting (id)
);

CREATE TABLE application
(
    guild_id   BIGINT      NOT NULL,
    user_id    BIGINT      NOT NULL,
    posting_id BIGINT      NOT NULL,
    applied_at DATETIME(6) NOT NULL,
    -- [지원함] 버튼을 두 번 눌러도 1건
    PRIMARY KEY (guild_id, user_id, posting_id),
    CONSTRAINT fk_application_guild FOREIGN KEY (guild_id) REFERENCES guild (guild_id),
    CONSTRAINT fk_application_posting FOREIGN KEY (posting_id) REFERENCES posting (id)
);

-- 수집 1회 = 1행. "새 공고 0건" 과 "수집 실패" 를 구분하는 근거다.
CREATE TABLE collect_run
(
    id            BIGINT        NOT NULL AUTO_INCREMENT,
    run_id        CHAR(32)      NOT NULL,
    source_id     VARCHAR(30)   NOT NULL,
    company_id    BIGINT        NOT NULL,
    status        VARCHAR(10)   NOT NULL,
    -- 응답에 담긴 열린 공고 수. 양수에서 0 으로 떨어지면 출처 형식 변경을 의심한다.
    fetched_count INT           NULL,
    new_count     INT           NULL,
    error         VARCHAR(1000) NULL,
    started_at    DATETIME(6)   NOT NULL,
    finished_at   DATETIME(6)   NULL,
    PRIMARY KEY (id),
    KEY idx_collect_run_company_started (company_id, started_at),
    CONSTRAINT fk_collect_run_company FOREIGN KEY (company_id) REFERENCES company (id)
);
