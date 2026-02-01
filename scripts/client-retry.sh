#!/usr/bin/env bash
set -euo pipefail

URL="http://localhost:8091/api/recipes/calories?recipeId=456"
BODY='{"excludeIngredients":["Cheese"]}'

MAX_ATTEMPTS=8
SLEEP=1

for attempt in $(seq 1 "$MAX_ATTEMPTS"); do
  echo "Attempt ${attempt}/${MAX_ATTEMPTS}..."

  headers_file="$(mktemp)"
  body_file="$(mktemp)"

  # -D writes headers, -o writes body, -w prints status code only
  code="$(curl -sS -D "$headers_file" -o "$body_file" -w '%{http_code}' \
    -X POST "$URL" \
    -H "Content-Type: application/json" \
    -d "$BODY")"

  if [[ "$code" == "200" ]]; then
    echo "OK"
    cat "$body_file"
    rm -f "$headers_file" "$body_file"
    exit 0
  fi

  echo "HTTP $code"

  if [[ "$code" == "429" || "$code" =~ ^5 ]]; then
    retry_after="$(grep -i '^Retry-After:' "$headers_file" | awk '{print $2}' | tr -d '\r' || true)"

    if [[ -n "$retry_after" ]]; then
      echo "Retry-After=${retry_after}s"
      sleep "$retry_after"
    else
      echo "Backoff sleep=${SLEEP}s"
      sleep "$SLEEP"
      SLEEP=$(( SLEEP * 2 ))
      [[ "$SLEEP" -gt 16 ]] && SLEEP=16
    fi

    rm -f "$headers_file" "$body_file"
    continue
  fi

  echo "Non-retryable response:"
  cat "$body_file"
  rm -f "$headers_file" "$body_file"
  exit 1
done

echo "Exhausted retries after ${MAX_ATTEMPTS} attempts."
exit 2
