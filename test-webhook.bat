@echo off
REM ============================================
REM  Teste de Webhook GitHub - DevStats
REM  Simula um evento "push" do GitHub
REM ============================================

setlocal

set WEBHOOK_SECRET=bb37dc704f3581f94c83c7b4c469a4c4b683a64089d6388c7d72553a31f5f27c
set WEBHOOK_URL=http://localhost:8081/webhook/github
set GITHUB_USER=Beno-Goulart

REM Gerar payload JSON
set PAYLOAD={"ref":"refs/heads/main","repository":{"name":"DevStats","full_name":"%GITHUB_USER%/DevStats"},"sender":{"login":"%GITHUB_USER%"},"head_commit":{"message":"test commit"}}

REM Gerar assinatura HMAC-SHA256 usando PowerShell
for /f "delims=" %%i in ('powershell -Command "$payload = '%PAYLOAD%'; $secret = '%WEBHOOK_SECRET%'; $hmac = [System.Security.Cryptography.HMACSHA256]::new([System.Text.Encoding]::UTF8.GetBytes($secret)); $hash = $hmac.ComputeHash([System.Text.Encoding]::UTF8.GetBytes($payload)); 'sha256=' + ($hash | ForEach-Object { '{0:x2}' -f $_ }) -join ''"') do set SIGNATURE=%%i

echo ============================================
echo  Enviando webhook de teste...
echo ============================================
echo URL:       %WEBHOOK_URL%
echo Event:     push
echo User:      %GITHUB_USER%
echo Signature: %SIGNATURE%
echo.

curl -X POST "%WEBHOOK_URL%" ^
  -H "Content-Type: application/json" ^
  -H "X-GitHub-Event: push" ^
  -H "X-Hub-Signature-256: %SIGNATURE%" ^
  -d "%PAYLOAD%"

echo.
echo ============================================
echo  Resposta enviada. Verifique os logs do bot.
echo ============================================

endlocal
