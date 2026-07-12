# ============================================
#  Teste de Webhook GitHub - DevStats (PowerShell)
#  Simula um evento "push" do GitHub
# ============================================

param(
    [string]$WebhookUrl = "http://localhost:8081/webhook/github",
    [string]$Secret = "bb37dc704f3581f94c83c7b4c469a4c4b683a64089d6388c7d72553a31f5f27c",
    [string]$GithubUser = "Beno-Goulart"
)

$payload = @{
    ref = "refs/heads/main"
    repository = @{
        name = "DevStats"
        full_name = "$GithubUser/DevStats"
    }
    sender = @{
        login = $GithubUser
    }
    head_commit = @{
        message = "test commit from webhook simulator"
    }
} | ConvertTo-Json -Depth 5

$hmac = [System.Security.Cryptography.HMACSHA256]::new(
    [System.Text.Encoding]::UTF8.GetBytes($Secret)
)
$hash = $hmac.ComputeHash([System.Text.Encoding]::UTF8.GetBytes($payload))
$signature = "sha256=" + (($hash | ForEach-Object { '{0:x2}' -f $_ }) -join '')

Write-Host "============================================" -ForegroundColor Cyan
Write-Host " Enviando webhook de teste..." -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan
Write-Host "URL:       $WebhookUrl"
Write-Host "Event:     push"
Write-Host "User:      $GithubUser"
Write-Host "Signature: $signature"
Write-Host ""

try {
    $webRequest = [System.Net.HttpWebRequest]::Create($WebhookUrl)
    $webRequest.Method = "POST"
    $webRequest.ContentType = "application/json"
    $webRequest.Headers.Add("X-GitHub-Event", "push")
    $webRequest.Headers.Add("X-Hub-Signature-256", $signature)
    $webRequest.Timeout = 10000

    $bytes = [System.Text.Encoding]::UTF8.GetBytes($payload)
    $webRequest.ContentLength = $bytes.Length
    $reqStream = $webRequest.GetRequestStream()
    $reqStream.Write($bytes, 0, $bytes.Length)
    $reqStream.Close()

    $webResponse = $webRequest.GetResponse()
    $reader = [System.IO.StreamReader]::new($webResponse.GetResponseStream())
    $body = $reader.ReadToEnd()
    $reader.Close()
    $webResponse.Close()

    Write-Host "Status: $([int]$webResponse.StatusCode)" -ForegroundColor Green
    Write-Host "Body:   $body" -ForegroundColor Green
} catch {
    $ex = $_.Exception
    Write-Host "Erro: $($ex.Message)" -ForegroundColor Red
    if ($ex.Response) {
        $errResponse = $ex.Response
        Write-Host "Status: $([int]$errResponse.StatusCode)" -ForegroundColor Yellow
        $errStream = $errResponse.GetResponseStream()
        if ($errStream) {
            $errReader = [System.IO.StreamReader]::new($errStream)
            $errBody = $errReader.ReadToEnd()
            $errReader.Close()
            Write-Host "Body:   $errBody" -ForegroundColor Yellow
        }
    } else {
        Write-Host "Tipo:   $($ex.GetType().FullName)" -ForegroundColor Yellow
    }
}

Write-Host ""
Write-Host "============================================" -ForegroundColor Cyan
Write-Host " Verifique os logs do bot." -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan
