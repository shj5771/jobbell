# jobbell

취업 스터디 디스코드 서버에 붙이는 채용 공고 알림 봇.
스터디가 고른 관심 기업의 공식 채용 공고를 주기적으로 확인하고, 새 공고를 채널에 올린다.

> 개발 중이다. 아직 동작하는 기능이 없다.

## 실행

Java 21 과 Docker 가 필요하다.

```bash
docker compose up -d   # 로컬 MySQL (호스트 포트 3307)
./gradlew bootRun      # 프로파일을 주지 않으면 local
```

스키마는 기동 시 Flyway 가 적용한다(`src/main/resources/db/migration`).
테스트는 Testcontainers 로 MySQL 을 따로 띄우므로 `docker compose` 없이 `./gradlew test` 로 돈다.

## 라이선스

[MIT](./LICENSE)
