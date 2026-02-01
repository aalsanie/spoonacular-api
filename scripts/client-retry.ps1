$Url = "http://localhost:8091/api/recipes/calories?recipeId=456"
$Body = '{"excludeIngredients":["Cheese"]}'
$MaxAttempts = 8
$Sleep = 1

for ($attempt = 1; $attempt -le $MaxAttempts; $attempt++) {
    Write-Host "Attempt $attempt/$MaxAttempts..."

    try {
        $resp = Invoke-WebRequest -Method POST -Uri $Url `
      -Headers @{ "Content-Type"="application/json" } `
      -Body $Body -UseBasicParsing

        Write-Host "OK"
        $resp.Content
        break
    }
    catch {
        $r = $_.Exception.Response
        if (-not $r) { throw } # no HTTP response -> rethrow

        $code = [int]$r.StatusCode
        Write-Host "HTTP $code"

        if ($code -eq 429 -or ($code -ge 500 -and $code -le 599)) {
            $retryAfter = $r.Headers["Retry-After"]
            if ($retryAfter) {
                Write-Host "Retry-After=$retryAfter seconds"
                Start-Sleep -Seconds ([int]$retryAfter)
            } else {
                Write-Host "Backoff sleep=$Sleep seconds"
                Start-Sleep -Seconds $Sleep
                $Sleep = [Math]::Min($Sleep * 2, 16)
            }
            continue
        }

        # non-retryable
        throw
    }
}