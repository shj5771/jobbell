package io.github.shj5771.jobbell.discord;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param applicationId           디스코드 애플리케이션 ID
 * @param publicKey               요청 서명 검증용 공개키(hex 64자)
 * @param botToken                REST API 호출용 봇 토큰. 로그에 남기지 않는다
 * @param apiBaseUrl              디스코드 REST API 주소
 * @param registerCommands        기동 시 슬래시 명령을 등록할지
 * @param verifySignature         요청 서명을 검증할지. local 에서만 끌 수 있다
 * @param productionApplicationId 운영 봇의 애플리케이션 ID. prod 가 아닌데 이 ID 로 뜨면 기동을 막는다
 */
@ConfigurationProperties("app.discord")
public record DiscordProperties(String applicationId, String publicKey, String botToken, String apiBaseUrl,
                                boolean registerCommands, boolean verifySignature, String productionApplicationId) {
}
