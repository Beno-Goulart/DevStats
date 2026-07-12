[System.Net.ServicePointManager]::ServerCertificateValidationCallback = { $true }
try {
    $r = Invoke-WebRequest -Uri 'https://reminder-bonded-diffusion.ngrok-free.dev/webhook/github' -Method POST -ContentType 'application/json' -Headers @{"X-GitHub-Event"="ping"; "ngrok-skip-browser-warning"="true"} -Body '{}' -UseBasicParsing -ErrorAction Stop
    Write-Host "Status: $($r.StatusCode)"
    Write-Host "Body: $($r.Content)"
} catch {
    $ex = $_.Exception
    Write-Host "Error: $($ex.Message)"
    Write-Host "Type: $($ex.GetType().FullName)"
    if ($ex.InnerException) { Write-Host "Inner: $($ex.InnerException.Message)" }
    if ($ex.Response) {
        $stream = $ex.Response.GetResponseStream()
        $reader = New-Object System.IO.StreamReader($stream)
        Write-Host "Status: $([int]$ex.Response.StatusCode)"
        Write-Host "Body: $($reader.ReadToEnd())"
    }
}
