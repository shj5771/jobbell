package io.github.shj5771.jobbell.support;

import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 통합 테스트는 같은 MySQL 을 공유한다. 테스트마다 이 순서로 비우고 시작한다.
 *
 * <p>테스트마다 자기가 쓰는 테이블만 지우게 했더니, 다른 테스트가 남긴 발송 행이
 * 외래 키로 공고를 붙잡아 공고 삭제가 실패했다. 순서는 외래 키의 역순이다. 카탈로그(company)는 남긴다.
 */
public final class Database {

    private static final String[] TABLES_CHILD_FIRST = {
            "application", "notification", "member_filter", "guild_company", "guild", "collect_run", "posting"};

    public static void clean(JdbcTemplate jdbc) {
        for (String table : TABLES_CHILD_FIRST) {
            jdbc.update("DELETE FROM " + table);
        }
    }

    private Database() {
    }
}
