# Runs the backend locally with the dev profile.
#
# How settings reach Spring Boot:
#   1. backend/.env (git-ignored) is read by scripts/lib/DotEnv.ps1.
#   2. Each KEY=VALUE becomes an environment variable of THIS PowerShell process only.
#   3. `mvn spring-boot:run` starts Maven, which starts the application JVM; both are child
#      processes and inherit these variables. Spring Boot resolves ${DB_URL}, ${JWT_SECRET}, …
#   4. When the backend stops, every variable set here is restored/removed.
# Values are never printed – only variable names.
#
# Usage (from the backend folder):
#   .\scripts\run-dev.ps1                      # uses backend/.env
#   .\scripts\run-dev.ps1 -EnvFile other.env   # uses another file (e.g. for testing)

param(
    [string]$EnvFile
)

$ErrorActionPreference = 'Stop'
. "$PSScriptRoot\lib\DotEnv.ps1"

$backend = Split-Path -Parent $PSScriptRoot
if (-not $EnvFile) { $EnvFile = Join-Path $backend '.env' }

$saved = Import-DotEnv -Path $EnvFile
if (-not $env:SPRING_PROFILES_ACTIVE) { Set-TemporaryEnv -Saved $saved -Name 'SPRING_PROFILES_ACTIVE' -Value 'dev' }
# Use the Windows certificate store so Maven works behind antivirus HTTPS scanning (checks stay on).
if (-not $env:MAVEN_OPTS) { Set-TemporaryEnv -Saved $saved -Name 'MAVEN_OPTS' -Value '-Djavax.net.ssl.trustStoreType=Windows-ROOT' }

$mavenExit = 1
Push-Location $backend
try {
    # Maven on newer JDKs prints harmless warnings to stderr; don't treat them as script errors.
    $ErrorActionPreference = 'Continue'
    & mvn -B spring-boot:run
    $mavenExit = $LASTEXITCODE
} finally {
    Pop-Location
    Restore-DotEnv $saved
}
exit $mavenExit
