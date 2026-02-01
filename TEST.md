# API Test Kit

This file is a **complete, runnable test kit** for **every API endpoint** in this codebase, including:

- spoonacular endpoints
- Swagger + Actuator
- Idempotency (`Idempotency-Key`, `Idempotency-Status`)
- Inbound rate limiting (`429` + `Retry-After`)
- Client-side backoff/retry script that honors `Retry-After`
- External dependency (Spoonacular) failure scenarios

> **Two modes**
>
> 1) **Stub mode (recommended)** — no quota, no real spoonacular required (WireMock).
> 2) **Real mode** — hits spoonacular for real (requires `API_KEY`).

---

## 0) Endpoints covered

### APIs
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

### Cross-cutting behavior
- `X-Request-Id` is always present
- Idempotency:
  - Request header: `Idempotency-Key`
  - Response header: `Idempotency-Status` (`MISS` / `HIT` / `IN_FLIGHT`)
- Inbound rate limit:
  - `429`
  - JSON `{error, path}`
  - `Retry-After: <seconds>`

---

## 1) Stub mode: spoonacular with wireMock

### 1.1 Start WireMock + stubs

#### Run

- linux:
```bash
chmod +x scripts/start-wiremock.sh
./scripts/start-wiremock.sh
```
- windows:
```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\start-wiremock.ps1
```

### 1.2 Start your API pointing at WireMock

In another terminal:

```bash
export SPOONACULAR_BASE_URL="http://localhost:9000"
export SPOONACULAR_API_KEY="SPOONACULAR_API_KEY"

export RATE_LIMITS_INBOUND_LIMIT_FOR_PERIOD=3
export RATE_LIMITS_INBOUND_REFRESH_PERIOD="5s"
export RATE_LIMITS_INBOUND_TIMEOUT="0ms"

./mvnw spring-boot:run
```

#### Try it

In another terminal

1) Happy path
  ```shell
  curl http://localhost:9000/__admin/mappings
  
   curl "http://localhost:8091/api/recipes/search?query=pasta&cuisine=italian"
  ```

2) Idempotency Miss (first request):
```shell
curl.exe -i -X POST "http://localhost:8091/api/recipes/calories?recipeId=456" `
  -H "Content-Type: application/json" `
  -H "Idempotency-Key: demo-key-123" `
  -d "{\"excludeIngredients\":[\"Cheese\"]}"
```

3) Idempotency Hit (second matching request):
```shell
curl.exe -i -X POST "http://localhost:8091/api/recipes/calories?recipeId=456" `
  -H "Content-Type: application/json" `
  -H "Idempotency-Key: demo-key-123" `
  -d "{\"excludeIngredients\":[\"Cheese\"]}"
```
---

4) Invalid request format (400)
```shell
# Bad JSON wrong field
curl.exe -i -X POST "http://localhost:8091/api/recipes/calories?recipeId=456" `
  -H "Content-Type: application/json" `
  -d "{\"exclude\":\"Cheese\"}"

# Invalid JSON syntax error
curl.exe -i -X POST "http://localhost:8091/api/recipes/calories?recipeId=456" `
  -H "Content-Type: application/json" `
  -d "{"

# Missing required param
curl.exe -i -X POST "http://localhost:8091/api/recipes/calories" `
  -H "Content-Type: application/json" `
  -d "{\"excludeIngredients\":[\"Cheese\"]}"

```

5) Inbound rate limit + Retry-After (429)
```shell
export RATE_LIMITS_INBOUND_LIMIT_FOR_PERIOD = "3"
export RATE_LIMITS_INBOUND_REFRESH_PERIOD   = "5s"
export RATE_LIMITS_INBOUND_TIMEOUT          = "0ms"
./mvnw spring-boot:run

## Hammer the devil out of it - linux
for i in $(seq 1 50); do
  headers="$(curl -sS -D - -o /dev/null \
    -X POST "http://localhost:8091/api/recipes/calories?recipeId=456" \
    -H "Content-Type: application/json" \
    -d '{"excludeIngredients":["Cheese"]}')"

  http_line="$(printf '%s\n' "$headers" | sed -n '1p')"
  retry_after="$(printf '%s\n' "$headers" | grep -i '^Retry-After:' || true)"

  echo "attempt=$i"
  echo "$http_line"
  [[ -n "$retry_after" ]] && echo "$retry_after"
  echo

  if echo "$http_line" | grep -q ' 429 '; then
    exit 0
  fi
done

```

```powershell
# powershell hammer
1..20 | % {
  curl.exe -sS -D - -o NUL -X POST "http://localhost:8091/api/recipes/calories?recipeId=456" `
    -H "Content-Type: application/json" `
    -d "{\"excludeIngredients\":[\"Cheese\"]}" |
    findstr /I "HTTP/ Retry-After"
  ""
}
```

6) Client-side retries + exponential backoff honoring Retry-After
```shell
# Linux
export RATE_LIMITS_INBOUND_LIMIT_FOR_PERIOD="1"
export RATE_LIMITS_INBOUND_REFRESH_PERIOD="30s"
export RATE_LIMITS_INBOUND_TIMEOUT="0ms"

./mvnw spring-boot:run

chmod +x client-retry.sh
./client-retry.sh
```
```powershell
# powershell back off
# Always set values so you can actually trigger the inbound backoff
$env:RATE_LIMITS_INBOUND_LIMIT_FOR_PERIOD = "1"
$env:RATE_LIMITS_INBOUND_REFRESH_PERIOD   = "30s"
$env:RATE_LIMITS_INBOUND_TIMEOUT          = "0ms"
./mvnw spring-boot:run
# execute 2 times to trigger
powershell -ExecutionPolicy Bypass -File .\scripts\client-retry.ps1
```

7) Note on Resilience4j “outbound retries”

**Resilience4j retry settings**: outbound retries are only active if the `Spoonacular` call path is actually wrapped/annotated to use them.

- Inbound rate limiting is testable via `429 + Retry-After`
- Client-side retry/backoff is testable via `scripts/client-retry.sh`
- Outbound (Spoonacular) retry/backoff is **not guaranteed** unless code uses `Resilience4j` in the `Spoonacular` client/service path.
