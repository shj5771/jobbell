#!/usr/bin/env bash
# 처음 한 번: Let's Encrypt 인증서를 받는다. 인증서가 없으면 HTTPS 설정으로 Nginx 가 뜨지 못하므로
# HTTP 전용 임시 Nginx 로 도메인 소유 확인만 통과시킨다.
set -euo pipefail
cd "$(dirname "$0")"
set -a; . ./.env; set +a
: "${DOMAIN:?.env 에 DOMAIN 이 필요하다}"
: "${CERT_EMAIL:?CERT_EMAIL=<인증서 만료 안내를 받을 메일> 을 환경변수로 줄 것}"

docker run -d --rm --name jobbell-acme -p 80:80 \
    -v "$(pwd)/nginx/bootstrap.conf:/etc/nginx/conf.d/default.conf:ro" \
    -v jobbell_acme-webroot:/var/www/acme nginx:1.30.6
trap 'docker stop jobbell-acme >/dev/null' EXIT

docker compose -f compose.prod.yaml --env-file .env run --rm certbot \
    certonly --webroot -w /var/www/acme -d "$DOMAIN" --email "$CERT_EMAIL" --agree-tos --no-eff-email
echo "인증서 발급 완료. 이제 ./deploy.sh 로 배포한다."
