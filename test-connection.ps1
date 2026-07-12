try {
    $r = Invoke-WebRequest -Uri 'http://localhost:8081/webhook/github' -Method POST -ContentType 'application/json' -Headers @{"X-GitHub-Event"="ping"} -Body '{}' -ErrorAction Stop
    Write-Host "Status: $($r.StatusCode)"
    Write-Host "Body: $($r.Content)"
} catch {
    Write-Host "Exception: $($_.Exception.GetType().FullName)"
    Write-Host "Message: $($_.Exception.Message)"
    if ($_.Exception.Response) {
        $stream = $_.Exception.Response.GetResponseStream()
        $reader = New-Object System.IO.StreamReader($stream)
        Write-Host "ResponseBody: $($reader.ReadToEnd())"
    } else {
        Write-Host "No response object"
    }
}
