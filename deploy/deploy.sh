#!/usr/bin/env bash
# blue/green 배포. 쓰지 않는 색을 새 이미지로 띄우고, 헬스체크가 통과해야만 Nginx 를 넘긴 뒤 옛 색을 내린다.
# 새 색이 healthy 가 되지 않으면 아무것도 바꾸지 않고 실패로 끝난다 — 지금 돌고 있는 쪽은 그대로다.
#
# 사용: ./deploy.sh [이미지 태그]   (태그를 안 주면 .env 의 TAG)
set -euo pipefail
cd "$(dirname "$0")"

[ -f .env ] || { echo ".env 가 없다. .env.example 을 복사해 채울 것."; exit 1; }
set -a; . ./.env; set +a
export TAG="${1:-${TAG:-latest}}"

compose() { docker compose -f compose.prod.yaml --env-file .env "$@"; }

active=$(cat .active-color 2>/dev/null || echo none)
if [ "$active" = "blue" ]; then next=green; else next=blue; fi
echo "현재: $active → 새로 띄울 색: $next (이미지 ${IMAGE}:${TAG})"

# 앱보다 DB 가 먼저 준비돼 있어야 한다. 아래에서 앱은 --no-deps 로 띄우므로 depends_on 이 기다려주지 않는다.
compose up -d --wait mysql
compose up -d loki alloy grafana

# --no-deps: 지금 돌고 있는 색을 건드리지 않는다. --pull missing: 이 태그가 서버에 없을 때만 받는다.
compose up -d --no-deps --pull missing --force-recreate "app-$next"

container=$(compose ps -q "app-$next")
status=starting
for _ in $(seq 1 60); do
    status=$(docker inspect -f '{{.State.Health.Status}}' "$container")
    [ "$status" = "healthy" ] || [ "$status" = "unhealthy" ] && break
    sleep 3
done
if [ "$status" != "healthy" ]; then
    echo "app-$next 가 healthy 가 되지 않았다($status). 전환하지 않는다. 마지막 로그:"
    compose logs --tail 50 "app-$next"
    compose stop "app-$next"
    exit 1
fi

# 파일을 새로 만들지 않고(mv 금지) 내용만 덮어쓴다. 단일 파일 bind mount 는 inode 를 따라가서,
# 새 파일로 바꾸면 컨테이너 안에서는 옛 내용이 그대로 보인다.
printf 'upstream jobbell_app {\n    server app-%s:8080;\n}\n' "$next" > nginx/upstream.conf

if [ -n "$(compose ps -q --status running nginx)" ]; then
    compose exec nginx nginx -t
    compose exec nginx nginx -s reload
else
    compose up -d nginx
fi
echo "$next" > .active-color
echo "트래픽 전환: app-$next"

if [ "$active" != "none" ]; then
    # 이미 받은 요청(최대 3초)이 끝나도록 잠깐 둔다. 내려가는 쪽은 AsyncAppender 큐를 비우고 종료한다.
    sleep 5
    compose stop "app-$active"
    echo "app-$active 정지"
fi
