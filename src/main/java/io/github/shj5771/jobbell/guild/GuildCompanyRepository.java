package io.github.shj5771.jobbell.guild;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GuildCompanyRepository extends JpaRepository<GuildCompany, GuildCompany.Id> {

    List<GuildCompany> findByIdGuildId(Long guildId);

    List<GuildCompany> findByIdCompanyId(Long companyId);
}
