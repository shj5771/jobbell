package io.github.shj5771.jobbell.discord.command;

import io.github.shj5771.jobbell.discord.Interaction;
import io.github.shj5771.jobbell.discord.Responses;
import io.github.shj5771.jobbell.guild.GuildCompany;
import io.github.shj5771.jobbell.guild.GuildCompanyRepository;
import io.github.shj5771.jobbell.posting.CollectRun;
import io.github.shj5771.jobbell.posting.CollectRunRepository;
import io.github.shj5771.jobbell.posting.Company;
import io.github.shj5771.jobbell.posting.CompanyRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 관심 기업과 각 기업의 마지막 수집 상태.
 *
 * <p>마지막 수집 성공 시각을 보여주는 것은 일부러다. 사용자가 "요즘 알림이 없네" 를
 * 스스로 확인할 수 있는 유일한 창이다. 수집이 실패 중이면 그 사실도 그대로 보여준다.
 */
@Component
public class ListCompaniesCommand implements SlashCommand {

    private final GuildCompanyRepository guildCompanies;
    private final CompanyRepository companies;
    private final CollectRunRepository collectRuns;

    public ListCompaniesCommand(GuildCompanyRepository guildCompanies, CompanyRepository companies,
                                CollectRunRepository collectRuns) {
        this.guildCompanies = guildCompanies;
        this.companies = companies;
        this.collectRuns = collectRuns;
    }

    @Override
    public String name() {
        return "기업목록";
    }

    @Override
    public Map<String, Object> definition() {
        return Definitions.command(name(), "관심 기업과 마지막 수집 시각을 봅니다", false, List.of());
    }

    @Override
    public Map<String, Object> handle(long guildId, Interaction interaction) {
        List<Long> ids = guildCompanies.findByIdGuildId(guildId).stream().map(GuildCompany::getCompanyId).toList();
        if (ids.isEmpty()) {
            return Responses.ephemeral("아직 관심 기업이 없습니다. /기업추가 로 추가하세요.");
        }
        StringBuilder content = new StringBuilder("**관심 기업**\n");
        for (Company company : companies.findAllById(ids)) {
            content.append("• ").append(company.getName()).append(" — ").append(status(company)).append('\n');
        }
        return Responses.ephemeral(Format.clip(content.toString()));
    }

    private String status(Company company) {
        Optional<CollectRun> lastSuccess =
                collectRuns.findTopByCompanyIdAndStatusOrderByStartedAtDesc(company.getId(), CollectRun.Status.SUCCESS);
        Optional<CollectRun> last = collectRuns.findTopByCompanyIdOrderByStartedAtDesc(company.getId());

        if (last.isEmpty()) {
            return "아직 수집 전";
        }
        String success = lastSuccess.map(run -> "마지막 수집 " + Format.dateTime(run.getFinishedAt())).orElse("수집 성공 기록 없음");
        if (last.get().getStatus() == CollectRun.Status.FAILED) {
            return "⚠ 최근 수집 실패 중 (" + success + ")";
        }
        return success;
    }
}
