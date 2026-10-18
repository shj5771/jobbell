# jobbell

취업 스터디 디스코드 서버에 붙이는 채용 공고 알림 봇.
스터디가 고른 관심 기업의 공식 채용 공고를 10분마다 확인하고, 새 공고를 채널에 올린다.
조건(키워드·경력)을 걸어둔 스터디원은 멘션되고, [지원함] 버튼으로 누가 어디 지원했는지 함께 본다.

## 명령

| 명령 | 권한 | 하는 일 |
| --- | --- | --- |
| `/채널설정` | 서버 관리 | 알림을 올릴 채널을 정한다 |
| `/기업추가` `/기업삭제` | 서버 관리 | 관심 기업을 고른다(자동완성) |
| `/기업목록` | 누구나 | 관심 기업과 마지막 수집 시각, 수집이 실패 중인지 |
| `/내조건` | 누구나 | 제목 키워드·경력 조건. 맞는 공고가 오면 멘션 |
| `/지원현황` | 누구나 | 스터디원별 지원 기록 |

## 구조

```
디스코드 명령 ─HTTP→ [서명 검증 필터: traceId 발급] → 명령 처리 ──────────────┐
수집 스케줄러(10분) [runId, 기업마다 traceId 발급] → 출처 어댑터 → 새 공고 판단 ─┼→ MySQL
                                       └→ 비동기 발송 [traceId 이어받음] → 디스코드 채널
모든 로그(JSON) → Alloy → Loki → Grafana
```

요청이 HTTP 한 곳으로만 들어오지 않는다. 명령은 필터가, 수집은 스케줄러가 직접 traceId 를 발급하고,
발송은 수집의 traceId 를 물려받는다. 알림 하단의 추적 코드(traceId 앞 8자리) 하나로
그 공고를 가져온 수집부터 발송까지 로그를 찾는다.

## 조용한 실패를 막은 곳

예외도 경고도 없이 "알림이 안 오는" 상태가 이 서비스에서 가장 위험하다. 그런 경로마다 장치를 두고 테스트로 고정했다.

| 실패 | 막는 방법 |
| --- | --- |
| 출처 장애를 "새 공고 0건" 으로 착각 | 출처는 실패를 빈 목록이 아니라 예외로 알린다([`PostingSource`](src/main/java/io/github/shj5771/jobbell/posting/source/PostingSource.java)). 수집 1회마다 성공/실패와 응답 공고 수를 기록한다 |
| 출처 응답 형식이 바뀌어 0건 | 열린 공고 수가 양수에서 0 으로 떨어지면 운영 경보([`CollectMonitor`](src/main/java/io/github/shj5771/jobbell/monitor/CollectMonitor.java)) |
| 스케줄러가 멈춤 | 마지막 수집 성공이 30분 넘으면 `/actuator/health` 503, 서버 밖 감시가 잡는다 |
| 발송이 밀림·실패 | PENDING 을 먼저 쓰고 보낸 뒤 SENT. 남은 PENDING 은 다시 보내고, 3회 실패나 10분 지연은 경보 |
| 프로파일 오타·운영 토큰으로 개발 실행 | 모르는 프로파일, prod 가 아닌데 운영 봇 ID 면 기동 실패 |
| 디스코드 3초 제한 초과 | 사용자에게만 "상호작용 실패" 가 뜨고 서버엔 흔적이 없다. 처리 시간을 재서 2초를 넘으면 WARN |
| 로그에 비밀값 | 봇 토큰·웹훅 URL 을 콘솔과 JSON 양쪽에서 가린다 |

## 로컬 실행

Java 21 과 Docker 가 필요하다. local 프로파일은 외부 API 를 부르지 않는다 — 공고는 가짜 출처, 디스코드 발송은 로그로만 남는다.

```bash
docker compose up -d   # 로컬 MySQL (호스트 포트 3307)
./gradlew bootRun      # 프로파일을 주지 않으면 local
```

local 은 서명 검증이 꺼져 있어 curl 로 명령을 흉내 낼 수 있다.

```bash
G='"guild_id":"42","member":{"user":{"id":"7"},"permissions":"32"}'
curl -s localhost:8080/discord/interactions -H 'Content-Type: application/json' \
  -d '{"type":2,'"$G"',"data":{"name":"채널설정","options":[{"name":"채널","value":"555"}]}}'
```

스키마는 기동 시 Flyway 가 적용한다(`src/main/resources/db/migration`).

## 테스트

```bash
./gradlew test
```

통합 테스트는 Testcontainers 로 MySQL 을 띄우므로 `docker compose` 없이 돈다.
디스코드 요청은 테스트용 Ed25519 키로 실제로 서명해 서명 검증 경로를 그대로 탄다.

## 배포

서버 1대 + Docker Compose. 배포는 blue/green 이고, 새 컨테이너가 healthy 일 때만 Nginx 를 넘긴다.
준비부터 운영까지는 [docs/operations.md](docs/operations.md).

## 라이선스

[MIT](./LICENSE)
