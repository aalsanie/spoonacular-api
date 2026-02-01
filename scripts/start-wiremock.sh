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