# 운영 가이드

서버 1대에 처음 올리는 것부터, 로그를 찾고 장애에 대응하는 것까지.

## 1. 디스코드 애플리케이션

[Discord Developer Portal](https://discord.com/developers/applications) 에서 애플리케이션을 만든다.

| 값 | 위치 | `.env` 키 |
| --- | --- | --- |
| Application ID | General Information | `DISCORD_APPLICATION_ID` |
| Public Key | General Information | `DISCORD_PUBLIC_KEY` |
| Bot Token | Bot → Reset Token | `DISCORD_BOT_TOKEN` |

봇을 서버에 초대한다. OAuth2 → URL Generator 에서 scope 는 `bot`, `applications.commands`,
권한은 `View Channels`, `Send Messages`, `Embed Links`.

개발용과 운영용 애플리케이션을 **따로** 만든다. 운영 봇의 Application ID 를
`application.yml` 의 `app.discord.production-application-id` 에 적어두면, prod 가 아닌 프로파일이 그 ID 로 뜨는 순간 기동이 멈춘다.

**Interactions Endpoint URL** 은 첫 배포가 끝난 뒤에 넣는다: `https://<도메인>/discord/interactions`.
디스코드가 저장 시점에 서명이 맞는 요청과 틀린 요청을 보내 각각 200 과 401 이 오는지 확인한다.

## 2. 서버

- Ubuntu VM 1대, Docker Engine + Compose 플러그인
- 80·443 만 연다. Grafana(3000)는 `127.0.0.1` 에만 묶여 있어 열 필요가 없다
- 도메인의 A 레코드를 서버 IP 로

```bash
git clone https://github.com/shj5771/jobbell.git ~/jobbell
cd ~/jobbell/deploy
cp .env.example .env    # 채운다
CERT_EMAIL=<메일> ./init-cert.sh
```

## 3. 배포

**GitHub 설정**

- Settings → Environments → `production`
- Secrets: `DEPLOY_HOST`, `DEPLOY_USER`, `DEPLOY_SSH_KEY`(서버에 등록한 배포용 키의 개인키)
- Variables: `DEPLOY_ENABLED=true` — 이걸 켜야 main 머지가 배포까지 간다

첫 배포는 Actions → Deploy → Run workflow 로 수동 실행한다.
이후로는 main 에 머지될 때마다 테스트 → 이미지(`ghcr.io/shj5771/jobbell:<커밋 SHA>`) → 서버 배포가 이어진다.

**`deploy.sh` 가 하는 일**

1. 쓰지 않는 색(blue/green)을 새 이미지로 띄운다
2. 컨테이너 헬스체크가 healthy 가 될 때까지 기다린다. 안 되면 **아무것도 바꾸지 않고** 실패로 끝난다
3. Nginx upstream 을 새 색으로 바꾸고 reload
4. 5초 뒤 옛 색을 내린다

**롤백**은 이전 커밋 SHA 로 다시 배포하면 된다.

```bash
cd ~/jobbell && git checkout --detach <이전 SHA> && ./deploy/deploy.sh <이전 SHA>
```

## 4. 감시

| 무엇 | 어떻게 |
| --- | --- |
| 서버·앱이 통째로 죽음, 스케줄러 멈춤 | 서버 **밖**의 업타임 감시 서비스가 `https://<도메인>/actuator/health` 를 5분마다 본다. 마지막 수집 성공이 30분 넘으면 503 |
| 수집 실패, 열린 공고 수 급락, 발송 지연·포기 | 앱이 `OPS_WEBHOOK_URL` 디스코드 채널로 알린다. 같은 경보는 풀릴 때까지 한 번만 |

운영 경보 채널은 사용자 채널과 분리한다. 웹훅은 봇 토큰과 별개라 봇이 고장나도 경보는 나간다.

## 5. 로그 찾기

```bash
ssh -L 3000:localhost:3000 <서버>   # 브라우저에서 http://localhost:3000 → Explore → Loki
```

| 상황 | 쿼리 |
| --- | --- |
| 알림 하단 추적 코드 `21862e44` | `{service="jobbell"} \| json \| traceId=~"21862e44.*"` |
| 특정 공고가 왜 안 왔나 | `{service="jobbell"} \| json \| postingId="17"` — 수집 → "알리지 않음: 이유" → 발송 순으로 보인다 |
| 에러만 | `{service="jobbell", level="ERROR"}` |
| 배포 중 두 인스턴스 비교 | `{service="jobbell", instance="green"}` |

traceId·postingId 는 Loki 라벨이 아니라 본문 필드다. 값이 매번 달라 라벨로 만들면 인덱스가 커진다.

## 6. 인증서 갱신

Let's Encrypt 인증서는 90일이다. 서버 crontab 에 둔다.

```cron
0 4 * * 1 cd ~/jobbell/deploy && docker compose -f compose.prod.yaml --env-file .env run --rm certbot renew --quiet && docker compose -f compose.prod.yaml --env-file .env exec nginx nginx -s reload
```

## 7. 운영 기간 점검 — 일부러 넣을 장애

각 장애는 `docs/incidents/` 에 *증상 → 처음 의심한 것 → 실제 원인 → 로그에서 찾은 방법 → 고친 것* 으로 한 건씩 남긴다.

| 넣을 장애 | 확인할 것 |
| --- | --- |
| 출처 응답을 빈 목록으로 바꾼다 | 열린 공고 수 급락 경보가 오는가 |
| 스케줄러를 멈춘다 | 외부 감시가 30분 안에 잡는가 |
| 봇 토큰을 무효화한다 | 발송 지연·포기 경보가 오는가 |
| DB 를 끊는다 | 복구 후 놓친 공고가 늦게라도 가는가 |
| 디스크를 채운다 | 로그 보관 상한이 동작하는가, 앱이 살아 있는가 |
| 배포 중 명령을 연타한다 | 실패한 명령이 0건인가 |
