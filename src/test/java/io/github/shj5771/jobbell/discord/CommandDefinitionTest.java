package io.github.shj5771.jobbell.discord;

import io.github.shj5771.jobbell.IntegrationTest;
import io.github.shj5771.jobbell.discord.command.SlashCommand;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 디스코드는 명령 정의 하나라도 규칙을 어기면 일괄 등록 요청 전체를 거절한다.
 * 그러면 이전 등록본이 그대로 남아, 새 명령이 "안 보이는" 조용한 실패가 된다. 규칙을 여기서 미리 확인한다.
 */
@IntegrationTest
class CommandDefinitionTest {

    /** 디스코드 문서의 이름 규칙에서 이 앱이 쓰는 문자(한글·영문·숫자·-_')만 옮겼다. */
    private static final Pattern NAME = Pattern.compile("^[-_'\\p{L}\\p{N}]{1,32}$");

    @Autowired
    List<SlashCommand> commands;

    @Test
    @DisplayName("명령 6개가 모두 등록 대상이고 이름이 겹치지 않는다")
    void 명령_목록() {
        assertThat(commands).extracting(SlashCommand::name)
                .containsExactlyInAnyOrder("채널설정", "기업추가", "기업삭제", "기업목록", "내조건", "지원현황");
    }

    @Test
    @DisplayName("이름·설명·옵션이 디스코드 규칙을 지킨다")
    @SuppressWarnings("unchecked")
    void 디스코드_규칙() {
        for (SlashCommand command : commands) {
            Map<String, Object> definition = command.definition();
            assertValidName((String) definition.get("name"));
            assertValidDescription((String) definition.get("description"));
            assertThat(definition.get("name")).isEqualTo(command.name());

            List<Map<String, Object>> options = (List<Map<String, Object>>) definition.get("options");
            assertThat(options).hasSizeLessThanOrEqualTo(25);
            boolean optionalSeen = false;
            for (Map<String, Object> option : options) {
                assertValidName((String) option.get("name"));
                assertValidDescription((String) option.get("description"));
                boolean required = Boolean.TRUE.equals(option.get("required"));
                assertThat(required && optionalSeen).as("필수 옵션은 선택 옵션보다 앞에 와야 한다: " + command.name()).isFalse();
                optionalSeen |= !required;
            }
        }
    }

    @Test
    @DisplayName("관리 명령은 등록 단계에서도 서버 관리 권한으로 가린다")
    void 관리_명령_권한() {
        for (SlashCommand command : commands) {
            assertThat(command.definition().containsKey("default_member_permissions"))
                    .as(command.name()).isEqualTo(command.adminOnly());
        }
    }

    private static void assertValidName(String name) {
        assertThat(name).matches(NAME);
        assertThat(name).as("대소문자가 있는 문자는 소문자여야 한다").isEqualTo(name.toLowerCase(Locale.ROOT));
    }

    private static void assertValidDescription(String description) {
        assertThat(description).isNotBlank().hasSizeLessThanOrEqualTo(100);
    }
}
