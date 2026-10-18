package io.github.shj5771.jobbell.discord.command;

import io.github.shj5771.jobbell.discord.Interaction;
import io.github.shj5771.jobbell.discord.Responses;
import io.github.shj5771.jobbell.guild.GuildCompany;
import io.github.shj5771.jobbell.guild.GuildCompanyRepository;
import io.github.shj5771.jobbell.posting.CatalogLoader;
import io.github.shj5771.jobbell.posting.Company;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/** 카탈로그 안에서 관심 기업을 고른다. 추가 시점 이전부터 열려 있던 공고는 알리지 않는다. */
@Component
public class AddCompanyCommand implements SlashCommand {

    static final int MAX_CHOICES = 25;

    private final CatalogLoader catalog;
    private final GuildCompanyRepository guildCompanies;
    private final Clock clock;

    public AddCompanyCommand(CatalogLoader catalog, GuildCompanyRepository guildCompanies, Clock clock) {
        this.catalog = catalog;
        this.guildCompanies = guildCompanies;
        this.clock = clock;
    }

    @Override
    public String name() {
        return "기업추가";
    }

    @Override
    public Map<String, Object> definition() {
        return Definitions.command(name(), "스터디 관심 기업을 추가합니다", true,
                List.of(Definitions.autocomplete("기업", "추가할 기업")));
    }

    @Override
    public boolean adminOnly() {
        return true;
    }

    @Override
    @Transactional
    public Map<String, Object> handle(long guildId, Interaction interaction) {
        Optional<Company> company = findCatalogCompany(interaction.option("기업").orElse(""));
        if (company.isEmpty()) {
            return Responses.ephemeral("목록에 없는 기업입니다. 입력 중에 뜨는 목록에서 골라 주세요.");
        }
        GuildCompany.Id id = new GuildCompany.Id(guildId, company.get().getId());
        if (guildCompanies.existsById(id)) {
            return Responses.ephemeral(company.get().getName() + " 은(는) 이미 관심 기업입니다.");
        }
        guildCompanies.save(new GuildCompany(guildId, company.get().getId(), clock.instant()));
        return Responses.ephemeral(company.get().getName() + " 을(를) 추가했습니다. 지금부터 올라오는 공고를 알립니다.");
    }

    @Override
    public List<Map<String, String>> autocomplete(long guildId, Interaction interaction) {
        Set<Long> already = guildCompanies.findByIdGuildId(guildId).stream()
                .map(GuildCompany::getCompanyId)
                .collect(Collectors.toSet());
        String typed = interaction.focusedValue().toLowerCase(Locale.ROOT);
        return catalog.catalogCompanies().stream()
                .filter(company -> !already.contains(company.getId()))
                .filter(company -> company.getName().toLowerCase(Locale.ROOT).contains(typed))
                .limit(MAX_CHOICES)
                .map(company -> Map.of("name", company.getName(), "value", String.valueOf(company.getId())))
                .toList();
    }

    /** 자동완성을 안 거치고 아무 글자나 보낼 수 있다. 값은 카탈로그 기업 ID 여야 한다. */
    private Optional<Company> findCatalogCompany(String value) {
        return catalog.catalogCompanies().stream()
                .filter(company -> String.valueOf(company.getId()).equals(value))
                .findFirst();
    }
}
