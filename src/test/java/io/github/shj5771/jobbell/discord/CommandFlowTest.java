package io.github.shj5771.jobbell.discord;

import ch.qos.logback.classic.spi.ILoggingEvent;
import io.github.shj5771.jobbell.IntegrationTest;
import io.github.shj5771.jobbell.common.MdcKeys;
import io.github.shj5771.jobbell.discord.command.ApplyButton;
import io.github.shj5771.jobbell.guild.ApplicationRepository;
import io.github.shj5771.jobbell.guild.GuildCompanyRepository;
import io.github.shj5771.jobbell.guild.GuildRepository;
import io.github.shj5771.jobbell.guild.MemberFilterRepository;
import io.github.shj5771.jobbell.posting.CatalogLoader;
import io.github.shj5771.jobbell.posting.CollectService;
import io.github.shj5771.jobbell.posting.Company;
import io.github.shj5771.jobbell.posting.PostingRepository;
import io.github.shj5771.jobbell.support.LogCapture;
import io.github.shj5771.jobbell.support.MutableClock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static io.github.shj5771.jobbell.discord.DiscordRequests.signed;
import static io.github.shj5771.jobbell.discord.Interactions.ADMIN;
import static io.github.shj5771.jobbell.discord.Interactions.GUILD;
import static io.github.shj5771.jobbell.discord.Interactions.MEMBER;
import static io.github.shj5771.jobbell.discord.Interactions.autocomplete;
import static io.github.shj5771.jobbell.discord.Interactions.button;
import static io.github.shj5771.jobbell.discord.Interactions.command;
import static io.github.shj5771.jobbell.discord.Interactions.option;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class CommandFlowTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    MutableClock clock;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    CatalogLoader catalog;

    @Autowired
    CollectService collectService;

    @Autowired
    GuildRepository guilds;

    @Autowired
    GuildCompanyRepository guildCompanies;

    @Autowired
    MemberFilterRepository filters;

    @Autowired
    ApplicationRepository applications;

    @Autowired
    PostingRepository postings;

    LogCapture logs;

    @BeforeEach
    void setUp() {
        for (String table : new String[]{"application", "notification", "member_filter", "guild_company", "guild",
                "collect_run", "posting"}) {
            jdbc.update("DELETE FROM " + table);
        }
        logs = LogCapture.attachTo(InteractionDispatcher.class.getName());
    }

    @AfterEach
    void tearDown() {
        logs.close();
    }

    @Test
    @DisplayName("/채널설정 은 관리자만 되고, 알림 채널이 저장된다")
    void 채널설정() throws Exception {
        perform(command(MEMBER, "채널설정", option("채널", "123")))
                .andExpect(jsonPath("$.data.content").value(containsString("권한")));
        assertThat(guilds.findById(GUILD).orElseThrow().getAlertChannelId()).isNull();

        perform(command(ADMIN, "채널설정", option("채널", "123")))
                .andExpect(jsonPath("$.data.content").value(containsString("<#123>")));
        assertThat(guilds.findById(GUILD).orElseThrow().getAlertChannelId()).isEqualTo(123L);
    }

    @Test
    @DisplayName("/기업추가 자동완성은 카탈로그에서 이름으로 거르고, 이미 추가한 기업은 빼고 보여준다")
    void 기업추가_자동완성() throws Exception {
        Company acme = company("acme");

        perform(autocomplete(ADMIN, "기업추가", "기업", "가짜"))
                .andExpect(jsonPath("$.type").value(8))
                .andExpect(jsonPath("$.data.choices[*].name").value(hasItem("가짜전자")))
                .andExpect(jsonPath("$.data.choices[*].name").value(hasItem("가짜게임즈")));

        perform(command(ADMIN, "기업추가", option("기업", acme.getId().toString())));

        perform(autocomplete(ADMIN, "기업추가", "기업", "가짜"))
                .andExpect(jsonPath("$.data.choices[*].name").value(not(hasItem("가짜전자"))));
    }

    @Test
    @DisplayName("/기업추가 는 한 번만 들어가고, /기업삭제 로 뺀다")
    void 기업추가_삭제() throws Exception {
        String acmeId = company("acme").getId().toString();

        perform(command(ADMIN, "기업추가", option("기업", acmeId)))
                .andExpect(jsonPath("$.data.content").value(containsString("추가했습니다")));
        perform(command(ADMIN, "기업추가", option("기업", acmeId)))
                .andExpect(jsonPath("$.data.content").value(containsString("이미")));
        assertThat(guildCompanies.findByIdGuildId(GUILD)).hasSize(1);

        perform(command(ADMIN, "기업삭제", option("기업", acmeId)))
                .andExpect(jsonPath("$.data.content").value(containsString("뺐습니다")));
        assertThat(guildCompanies.findByIdGuildId(GUILD)).isEmpty();
    }

    @Test
    @DisplayName("자동완성을 거치지 않은 아무 값은 거절한다")
    void 목록_밖의_값() throws Exception {
        perform(command(ADMIN, "기업추가", option("기업", "아무거나")))
                .andExpect(jsonPath("$.data.content").value(containsString("목록에 없는")));
    }

    @Test
    @DisplayName("/기업목록 은 마지막 수집 시각과, 실패 중인 기업을 보여준다")
    void 기업목록() throws Exception {
        perform(command(ADMIN, "기업추가", option("기업", company("acme").getId().toString())));
        perform(command(ADMIN, "기업추가", option("기업", company("broken").getId().toString())));

        perform(command(MEMBER, "기업목록")).andExpect(jsonPath("$.data.content").value(containsString("아직 수집 전")));

        collectService.collectAll();
        perform(command(MEMBER, "기업목록"))
                .andExpect(jsonPath("$.data.content").value(containsString("가짜전자 — 마지막 수집")))
                .andExpect(jsonPath("$.data.content").value(containsString("장애상사 — ⚠ 최근 수집 실패 중")));
    }

    @Test
    @DisplayName("/내조건 으로 저장하고 바꾸고, 키워드를 비우면 지운다")
    void 내조건() throws Exception {
        perform(command(MEMBER, "내조건", option("키워드", "백엔드"), option("경력", "신입")));
        assertThat(filters.findByIdGuildId(GUILD)).singleElement()
                .satisfies(f -> assertThat(f.getKeyword()).isEqualTo("백엔드"));

        perform(command(MEMBER, "내조건", option("키워드", "데이터")));
        assertThat(filters.findByIdGuildId(GUILD)).singleElement()
                .satisfies(f -> assertThat(f.getKeyword()).isEqualTo("데이터"))
                .satisfies(f -> assertThat(f.getCareer()).isNull());

        perform(command(MEMBER, "내조건")).andExpect(jsonPath("$.data.content").value(containsString("지웠습니다")));
        assertThat(filters.findByIdGuildId(GUILD)).isEmpty();
    }

    @Test
    @DisplayName("[지원함] 은 두 번 눌러도 한 건이고, /지원현황 에 사람별로 나온다")
    void 지원() throws Exception {
        collectService.collectAll();
        long postingId = postings.findAll().getFirst().getId();

        perform(button(MEMBER, ApplyButton.customId(postingId)))
                .andExpect(jsonPath("$.data.content").value(containsString("남겼습니다")));
        perform(button(MEMBER, ApplyButton.customId(postingId)))
                .andExpect(jsonPath("$.data.content").value(containsString("이미")));
        assertThat(applications.count()).isEqualTo(1);

        perform(command(ADMIN, "지원현황"))
                .andExpect(jsonPath("$.data.content").value(containsString("<@" + MEMBER + "> 1건")))
                .andExpect(jsonPath("$.data.allowed_mentions.parse").isEmpty());
    }

    @Test
    @DisplayName("명령 로그에 traceId·guildId·userId·command 가 붙는다")
    void 명령_로그() throws Exception {
        perform(command(MEMBER, "기업목록"));

        ILoggingEvent handled = logs.events().stream()
                .filter(e -> e.getFormattedMessage().startsWith("명령 처리"))
                .findFirst().orElseThrow();
        assertThat(handled.getMDCPropertyMap())
                .containsKey(MdcKeys.TRACE_ID)
                .containsEntry(MdcKeys.GUILD_ID, String.valueOf(GUILD))
                .containsEntry(MdcKeys.USER_ID, String.valueOf(MEMBER))
                .containsEntry(MdcKeys.COMMAND, "기업목록");
    }

    private ResultActions perform(String body) throws Exception {
        return mockMvc.perform(signed(body, clock.instant())).andExpect(status().isOk());
    }

    private Company company(String key) {
        return catalog.catalogCompanies().stream().filter(c -> c.getSourceKey().equals(key)).findFirst().orElseThrow();
    }
}
