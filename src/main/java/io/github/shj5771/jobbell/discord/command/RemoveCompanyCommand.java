package io.github.shj5771.jobbell.discord.command;

import io.github.shj5771.jobbell.discord.Interaction;
import io.github.shj5771.jobbell.discord.Responses;
import io.github.shj5771.jobbell.guild.GuildCompany;
import io.github.shj5771.jobbell.guild.GuildCompanyRepository;
import io.github.shj5771.jobbell.posting.Company;
import io.github.shj5771.jobbell.posting.CompanyRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class RemoveCompanyCommand implements SlashCommand {

    private final GuildCompanyRepository guildCompanies;
    private final CompanyRepository companies;

    public RemoveCompanyCommand(GuildCompanyRepository guildCompanies, CompanyRepository companies) {
        this.guildCompanies = guildCompanies;
        this.companies = companies;
    }

    @Override
    public String name() {
        return "기업삭제";
    }

    @Override
    public Map<String, Object> definition() {
        return Definitions.command(name(), "스터디 관심 기업에서 뺍니다", true,
                List.of(Definitions.autocomplete("기업", "뺄 기업")));
    }

    @Override
    public boolean adminOnly() {
        return true;
    }

    @Override
    @Transactional
    public Map<String, Object> handle(long guildId, Interaction interaction) {
        String value = interaction.option("기업").orElse("");
        if (!value.matches("\\d+")) {
            return Responses.ephemeral("입력 중에 뜨는 목록에서 골라 주세요.");
        }
        GuildCompany.Id id = new GuildCompany.Id(guildId, Long.parseLong(value));
        if (!guildCompanies.existsById(id)) {
            return Responses.ephemeral("관심 기업이 아닙니다.");
        }
        guildCompanies.deleteById(id);
        String name = companies.findById(id.companyId()).map(Company::getName).orElse("기업");
        return Responses.ephemeral(name + " 을(를) 뺐습니다.");
    }

    @Override
    public List<Map<String, String>> autocomplete(long guildId, Interaction interaction) {
        String typed = interaction.focusedValue().toLowerCase(Locale.ROOT);
        List<Long> ids = guildCompanies.findByIdGuildId(guildId).stream().map(GuildCompany::getCompanyId).toList();
        return companies.findAllById(ids).stream()
                .filter(company -> company.getName().toLowerCase(Locale.ROOT).contains(typed))
                .limit(AddCompanyCommand.MAX_CHOICES)
                .map(company -> Map.of("name", company.getName(), "value", String.valueOf(company.getId())))
                .toList();
    }
}
