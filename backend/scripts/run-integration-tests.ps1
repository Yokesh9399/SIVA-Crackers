# Runs all backend tests including the MySQL integration tests.
#
# Uses TEST_DB_URL / TEST_DB_USERNAME / TEST_DB_PASSWORD from backend/.env. The tests REBUILD
# their database from scratch, so they refuse to run unless TEST_DB_URL points to a localhost
# database whose name ends in _test (siva_crackers_test). The development database is never touched.
#
# Usage (from the backend folder):   .\scripts\run-integration-tests.ps1

param(
    [string]$EnvFile
)

$ErrorActionPreference = 'Stop'
. "$PSScriptRoot\lib\DotEnv.ps1"

$backend = Split-Path -Parent $PSScriptRoot
if (-not $EnvFile) { $EnvFile = Join-Path $backend '.env' }

# Only the test-database connection is loaded: the tests use their own fixed settings and must not
# be influenced by local development values such as APP_COOKIE_SECURE=false.
$saved = Import-DotEnv -Path $EnvFile -Only 'TEST_DB_URL', 'TEST_DB_USERNAME', 'TEST_DB_PASSWORD'
foreach ($name in 'TEST_DB_URL', 'TEST_DB_USERNAME', 'TEST_DB_PASSWORD') {
    if (-not [Environment]::GetEnvironmentVariable($name, 'Process')) {
        Write-Host "$name is not set in $EnvFile - integration tests need TEST_DB_URL, TEST_DB_USERNAME and TEST_DB_PASSWORD." -ForegroundColor Red
        Restore-DotEnv $saved
        exit 1
    }
}
if (-not $env:MAVEN_OPTS) { Set-TemporaryEnv -Saved $saved -Name 'MAVEN_OPTS' -Value '-Djavax.net.ssl.trustStoreType=Windows-ROOT' }

$mavenExit = 1
Push-Location $backend
try {
    $ErrorActionPreference = 'Continue'
    & mvn -B clean verify -Pintegration
    $mavenExit = $LASTEXITCODE
} finally {
    Pop-Location
    Restore-DotEnv $saved
}
exit $mavenExit
