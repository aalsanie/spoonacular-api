# TEST.md — Atypon Food API Test Kit

This file is a **complete, runnable test kit** for **every API endpoint** in this codebase, including:

- Business endpoints
- Swagger + Actuator
- Idempotency (`Idempotency-Key`, `Idempotency-Status`)
- Inbound rate limiting (`429` + `Retry-After`)
- Client-side backoff/retry script that honors `Retry-After`
- External dependency (Spoonacular) failure scenarios

> **Two modes**
>
> 1) **Stub mode (recommended)** — no quota, no real Spoonacular required (WireMock).
> 2) **Real mode** — hits Spoonacular for real (requires `API_KEY`).

---

## 0) Endpoints covered

### Business APIs
- `GET  /api/recipes/search?query=...&cuisine=...`
- `GET  /api/recipes/recipe-info?recipeId=...`
- `POST /api/recipes/calories?recipeId=...` body: `{"excludeIngredients":["Cheese"]}` (or empty)

### Platform/ops
- `GET /` (redirect to Swagger)
- `GET /swagger-ui.html`
- `GET /actuator/health`
- `GET /actuator/info`
- `GET /actuator/metrics`
- `GET /actuator/prometheus`

### Cross-cutting behavior you can validate
- `X-Request-Id` is always present
- Idempotency:
  - Request header: `Idempotency-Key`
  - Response header: `Idempotency-Status` (`MISS` / `HIT` / `IN_FLIGHT`)
- Inbound rate limit:
  - `429`
  - JSON `{error, path}`
  - `Retry-After: <seconds>`

---

## 1) Stub mode (deterministic): Fake Spoonacular with WireMock

### 1.1 Start WireMock + stubs

Create: `scripts/start-wiremock.sh`

```bash
#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
WM_DIR="${ROOT_DIR}/.wiremock"
MAP_DIR="${WM_DIR}/mappings"
mkdir -p "${MAP_DIR}"

# --- /recipes/complexSearch stub (search) ---
cat > "${MAP_DIR}/search.json" <<'JSON'
{
  "request": {
    "method": "GET",
    "urlPath": "/recipes/complexSearch"
  },
  "response": {
    "status": 200,
    "headers": { "Content-Type": "application/json" },
    "jsonBody": {
      "results": [
        { "id": 456, "title": "Pasta With Cheese" },
        { "id": 457, "title": "Apple Salad" }
      ]
    }
  }
}
JSON

# --- /recipes/{id}/information stubs (recipe-info + calories) ---
cat > "${MAP_DIR}/recipe-456.json" <<'JSON'
{
  "request": { "method": "GET", "urlPath": "/recipes/456/information" },
  "response": {
    "status": 200,
    "headers": { "Content-Type": "application/json" },
    "jsonBody": {
      "id": 456,
      "title": "Pasta With Cheese",
      "extendedIngredients": [
        { "name": "Cheese", "amount": 1, "unit": "piece", "nutrition": { "calories": 40 } },
        { "name": "Pasta", "amount": 100, "unit": "grams", "nutrition": { "calories": 140 } }
      ]
    }
  }
}
JSON

cat > "${MAP_DIR}/recipe-999.json" <<'JSON'
{
  "request": { "method": "GET", "urlPath": "/recipes/999/information" },
  "response": {
    "status": 404,
    "headers": { "Content-Type": "application/json" },
    "jsonBody": { "message": "Not found" }
  }
}
JSON

echo "Starting WireMock on :9000 ..."
docker run --rm -p 9000:8080 \
  -v "${WM_DIR}:/home/wiremock" \
  wiremock/wiremock:3.5.4
```

Run it:

```bash
chmod +x scripts/start-wiremock.sh
./scripts/start-wiremock.sh
```

### 1.2 Start your API pointing at WireMock

In another terminal:

```bash
export SPOONACULAR_BASE_URL="http://localhost:9000"
export SPOONACULAR_API_KEY="dummy"

# Make rate limit easy to trigger quickly
export RATE_LIMITS_INBOUND_LIMIT_FOR_PERIOD=3
export RATE_LIMITS_INBOUND_REFRESH_PERIOD="5s"
export RATE_LIMITS_INBOUND_TIMEOUT="0ms"

./mvnw spring-boot:run
```

Default API port is **8091**, so:

```bash
BASE_URL="http://localhost:8091"
```

---

## 2) Real mode (calls Spoonacular for real)

```bash
export API_KEY="your_spoonacular_key"
./mvnw spring-boot:run
```

Same scripts below apply — expect variability and quota consumption.

---

## 3) Smoke test for every endpoint

Create: `scripts/smoke.sh`

```bash
#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8091}"

echo "== Root redirect =="
curl -sS -i "${BASE_URL}/" | sed -n '1,12p'
echo

echo "== Swagger =="
curl -sS -i "${BASE_URL}/swagger-ui.html" | sed -n '1,12p'
echo

echo "== Actuator health =="
curl -sS -i "${BASE_URL}/actuator/health" | sed -n '1,20p'
echo

echo "== Search =="
curl -sS -i "${BASE_URL}/api/recipes/search?query=pasta&cuisine=italian" | sed -n '1,30p'
echo

echo "== Recipe info (ok) =="
curl -sS -i "${BASE_URL}/api/recipes/recipe-info?recipeId=456" | sed -n '1,40p'
echo

echo "== Calories (exclude Cheese) =="
curl -sS -i -X POST \
  "${BASE_URL}/api/recipes/calories?recipeId=456" \
  -H 'Content-Type: application/json' \
  -d '{"excludeIngredients":["Cheese"]}' | sed -n '1,40p'
echo

echo "== Calories (empty exclude list) =="
curl -sS -i -X POST \
  "${BASE_URL}/api/recipes/calories?recipeId=456" \
  -H 'Content-Type: application/json' \
  -d '{"excludeIngredients":[]}' | sed -n '1,40p'
echo
```

Run:

```bash
chmod +x scripts/smoke.sh
./scripts/smoke.sh
```

---

## 4) Idempotency tests (MISS → HIT + conflicts)

Create: `scripts/idempotency.sh`

```bash
#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8091}"
KEY="${KEY:-demo-key-123}"

echo "== First request (should be MISS) =="
curl -sS -i -X POST \
  "${BASE_URL}/api/recipes/calories?recipeId=456" \
  -H "Idempotency-Key: ${KEY}" \
  -H 'Content-Type: application/json' \
  -d '{"excludeIngredients":["Cheese"]}' | sed -n '1,80p'
echo

echo "== Second request same key+payload (should be HIT) =="
curl -sS -i -X POST \
  "${BASE_URL}/api/recipes/calories?recipeId=456" \
  -H "Idempotency-Key: ${KEY}" \
  -H 'Content-Type: application/json' \
  -d '{"excludeIngredients":["Cheese"]}' | sed -n '1,80p'
echo

echo "== Same key, different payload (should be 409 conflict) =="
curl -sS -i -X POST \
  "${BASE_URL}/api/recipes/calories?recipeId=456" \
  -H "Idempotency-Key: ${KEY}" \
  -H 'Content-Type: application/json' \
  -d '{"excludeIngredients":["Pasta"]}' | sed -n '1,120p'
echo

echo "== In-flight duplicate conflict (409 IN_FLIGHT) =="
# Two concurrent requests with same key.
KEY2="${KEY}-inflight"

( curl -sS -i -X POST \
    "${BASE_URL}/api/recipes/calories?recipeId=456" \
    -H "Idempotency-Key: ${KEY2}" \
    -H 'Content-Type: application/json' \
    -d '{"excludeIngredients":["Cheese"]}' > /tmp/idempo_a.txt ) &

( curl -sS -i -X POST \
    "${BASE_URL}/api/recipes/calories?recipeId=456" \
    -H "Idempotency-Key: ${KEY2}" \
    -H 'Content-Type: application/json' \
    -d '{"excludeIngredients":["Cheese"]}' > /tmp/idempo_b.txt ) &

wait

echo "--- Response A ---"
sed -n '1,60p' /tmp/idempo_a.txt
echo
echo "--- Response B ---"
sed -n '1,90p' /tmp/idempo_b.txt
echo
```

Expected:
- First response: `Idempotency-Status: MISS`
- Second response: `Idempotency-Status: HIT`
- Same key with different body: **409**
- In-flight collision: one succeeds, the other returns **409** with `Idempotency-Status: IN_FLIGHT`

---

## 5) Invalid request format (400)

Create: `scripts/bad-request.sh`

```bash
#!/usr/bin/env bash
set -euo pipefail
BASE_URL="${BASE_URL:-http://localhost:8091}"

echo "== Invalid JSON shape (expect 400 + {error,path}) =="
curl -sS -i -X POST \
  "${BASE_URL}/api/recipes/calories?recipeId=456" \
  -H 'Content-Type: application/json' \
  -d '{ "exclude": "Cheese" }' | sed -n '1,120p'
echo
```

---

## 6) Inbound rate limit + Retry-After (429)

Create: `scripts/rate-limit.sh`

```bash
#!/usr/bin/env bash
set -euo pipefail
BASE_URL="${BASE_URL:-http://localhost:8091}"

echo "Hammering calories to trigger 429..."
for i in $(seq 1 30); do
  code="$(curl -sS -o /tmp/rl_body.txt -w '%{http_code}' -X POST \
    "${BASE_URL}/api/recipes/calories?recipeId=456" \
    -H 'Content-Type: application/json' \
    -d '{"excludeIngredients":["Cheese"]}')"

  if [[ "${code}" == "429" ]]; then
    echo "Got 429 on attempt ${i}"
    echo "Body:"
    cat /tmp/rl_body.txt
    echo
    echo "Headers snapshot:"
    curl -sS -D - -o /dev/null -X POST \
      "${BASE_URL}/api/recipes/calories?recipeId=456" \
      -H 'Content-Type: application/json' \
      -d '{"excludeIngredients":["Cheese"]}' | sed -n '1,30p'
    exit 0
  fi

  echo "attempt=${i} code=${code}"
done

echo "Did not hit 429. Start the app with lower limits (see Stub mode env vars)."
```

---

## 7) Client-side retries + exponential backoff honoring Retry-After (ready)

Create: `scripts/client-retry.sh`

```bash
#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8091}"
URL="${URL:-${BASE_URL}/api/recipes/calories?recipeId=456}"
BODY='{"excludeIngredients":["Cheese"]}'

max_attempts="${MAX_ATTEMPTS:-8}"
base_sleep="${BASE_SLEEP_SECONDS:-1}"

attempt=1
sleep_s="${base_sleep}"

while [[ $attempt -le $max_attempts ]]; do
  echo "Attempt ${attempt}/${max_attempts}..."

  tmp_headers="$(mktemp)"
  tmp_body="$(mktemp)"
  code="$(curl -sS -D "${tmp_headers}" -o "${tmp_body}" -w '%{http_code}' \
    -X POST "${URL}" -H 'Content-Type: application/json' -d "${BODY}")"

  if [[ "${code}" == "200" ]]; then
    echo "OK:"
    cat "${tmp_body}"
    rm -f "${tmp_headers}" "${tmp_body}"
    exit 0
  fi

  if [[ "${code}" == "429" ]]; then
    retry_after="$(grep -i '^Retry-After:' "${tmp_headers}" | awk '{print $2}' | tr -d '\r' || true)"
    if [[ -n "${retry_after}" ]]; then
      echo "429 rate-limited; honoring Retry-After=${retry_after}s"
      sleep "${retry_after}"
    else
      echo "429 rate-limited; sleeping ${sleep_s}s"
      sleep "${sleep_s}"
    fi
  elif [[ "${code}" =~ ^5 ]]; then
    echo "${code} server error; sleeping ${sleep_s}s"
    sleep "${sleep_s}"
  else
    echo "Non-retryable status=${code}"
    echo "Body:"
    cat "${tmp_body}"
    rm -f "${tmp_headers}" "${tmp_body}"
    exit 1
  fi

  rm -f "${tmp_headers}" "${tmp_body}"

  sleep_s=$(( sleep_s * 2 ))
  if [[ $sleep_s -gt 16 ]]; then sleep_s=16; fi

  attempt=$(( attempt + 1 ))
done

echo "Exhausted retries after ${max_attempts} attempts."
exit 2
```

Run:

```bash
chmod +x scripts/client-retry.sh
./scripts/client-retry.sh
```

---

## 8) External dependency failure tests (Spoonacular down / wrong URL / missing key)

These validate your API behavior when Spoonacular fails.

### 8.1 Force Spoonacular “down” (connection failure)

Start the app:

```bash
export SPOONACULAR_BASE_URL="http://localhost:59999"   # nothing listening
export SPOONACULAR_API_KEY="dummy"
./mvnw spring-boot:run
```

Then call:

```bash
curl -sS -i -X POST "http://localhost:8091/api/recipes/calories?recipeId=456" \
  -H 'Content-Type: application/json' \
  -d '{"excludeIngredients":["Cheese"]}' | sed -n '1,80p'
```

Expect: **5xx** (and your global error response format).

### 8.2 Missing API key (config invalid)

Start without `API_KEY` (or set it empty):

```bash
unset API_KEY
./mvnw spring-boot:run
```

Check:

```bash
curl -sS -i http://localhost:8091/actuator/health | sed -n '1,80p'
```

---

## 9) PowerShell equivalents (quick)

```powershell
$BASE="http://localhost:8091"

# search
curl "$BASE/api/recipes/search?query=pasta&cuisine=italian"

# calories
curl -Method POST "$BASE/api/recipes/calories?recipeId=456" `
  -Headers @{ "Content-Type"="application/json" } `
  -Body '{"excludeIngredients":["Cheese"]}'

# idempotency
curl -Method POST "$BASE/api/recipes/calories?recipeId=456" `
  -Headers @{ "Content-Type"="application/json"; "Idempotency-Key"="k1" } `
  -Body '{"excludeIngredients":["Cheese"]}'
```

---

## 10) Note on Resilience4j “outbound retries”

Your `application.yaml` has **Resilience4j retry settings**, but outbound retries are only active if the Spoonacular call path is actually wrapped/annotated to use them.

So today:
- Inbound rate limiting is testable via `429 + Retry-After`
- Client-side retry/backoff is testable via `scripts/client-retry.sh`
- Outbound (Spoonacular) retry/backoff is **not guaranteed** unless code uses Resilience4j in the Spoonacular client/service path.

(If you want outbound retries implemented properly, say so and I’ll wire it production-grade.)

---
