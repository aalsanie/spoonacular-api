$ErrorActionPreference = "Stop"

$root = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$wmDir = Join-Path $root ".wiremock"
$mapDir = Join-Path $wmDir "mappings"

New-Item -ItemType Directory -Force -Path $mapDir | Out-Null

@'
{
  "request": { "method": "GET", "urlPath": "/recipes/complexSearch" },
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
'@ | Set-Content -Encoding utf8 (Join-Path $mapDir "search.json")

@'
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
'@ | Set-Content -Encoding utf8 (Join-Path $mapDir "recipe-456.json")

@'
{
  "request": { "method": "GET", "urlPath": "/recipes/999/information" },
  "response": {
    "status": 404,
    "headers": { "Content-Type": "application/json" },
    "jsonBody": { "message": "Not found" }
  }
}
'@ | Set-Content -Encoding utf8 (Join-Path $mapDir "recipe-999.json")

$mount = "${wmDir}:/home/wiremock"
docker run --rm -p 9000:8080 -v $mount wiremock/wiremock:3.5.4

