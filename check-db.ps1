Add-Type -Path "C:\Users\Windows\Downloads\DevStats\DevStats-Backend\target\dependency\sqlite-jdbc*.jar" -ErrorAction SilentlyContinue

$connStr = "Data Source=C:\Users\Windows\Downloads\DevStats\devstats.db;Version=3"
try {
    $conn = New-Object System.Data.SQLite.SQLiteConnection($connStr)
    $conn.Open()
    $cmd = $conn.CreateCommand()
    $cmd.CommandText = "PRAGMA table_info(users)"
    $reader = $cmd.ExecuteReader()
    Write-Host "=== Schema da tabela users ==="
    while ($reader.Read()) {
        $name = $reader["name"]
        $type = $reader["type"]
        $pk = $reader["pk"]
        Write-Host "  $name $type pk=$pk"
    }
    $reader.Close()

    $cmd2 = $conn.CreateCommand()
    $cmd2.CommandText = "SELECT * FROM DATABASECHANGELOG"
    $reader2 = $cmd2.ExecuteReader()
    Write-Host ""
    Write-Host "=== Liquibase Changelog ==="
    while ($reader2.Read()) {
        Write-Host "  ID=$($reader2['ID']) Author=$($reader2['AUTHOR']) Filename=$($reader2['FILENAME'])"
    }
    $reader2.Close()
    $conn.Close()
} catch {
    Write-Host "Erro: $($_.Exception.Message)"
}
