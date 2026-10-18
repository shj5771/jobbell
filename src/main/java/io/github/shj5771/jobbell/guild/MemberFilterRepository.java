package io.github.shj5771.jobbell.guild;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MemberFilterRepository extends JpaRepository<MemberFilter, MemberFilter.Id> {

    List<MemberFilter> findByIdGuildId(Long guildId);
}
