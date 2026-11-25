# Shared helpers for loading backend/.env into the CURRENT PowerShell process only.
#
#   . "$PSScriptRoot\lib\DotEnv.ps1"
#   $saved = Import-DotEnv -Path $envFile     # sets variables, prints names only
#   ...
#   Restore-DotEnv $saved                     # removes / restores them afterwards
#
# Values are never printed. Nothing is written to the Windows user or system environment.

function Import-DotEnv {
    # -Only: load just these variable names (e.g. tests must not inherit local app settings).
    param([Parameter(Mandatory)][string]$Path, [string[]]$Only)

    $previous = @{}
    if (-not (Test-Path -LiteralPath $Path)) {
        Write-Host "No $Path file found - using variables already set in this shell."
        return $previous
    }

    $loaded = @()
    foreach ($line in Get-Content -LiteralPath $Path) {
        $trimmed = $line.Trim()
        if ($trimmed -eq '' -or $trimmed.StartsWith('#')) { continue }
        $eq = $trimmed.IndexOf('=')
        if ($eq -lt 1) { continue }
        $name = $trimmed.Substring(0, $eq).Trim()
        $value = $trimmed.Substring($eq + 1).Trim()
        # Optional surrounding quotes: KEY="value" or KEY='value'
        if ($value.Length -ge 2 -and (($value.StartsWith('"') -and $value.EndsWith('"')) -or ($value.StartsWith("'") -and $value.EndsWith("'")))) {
            $value = $value.Substring(1, $value.Length - 2)
        }
        if ($name -notmatch '^[A-Za-z_][A-Za-z0-9_]*$') {
            Write-Warning "Skipping a line with an invalid variable name in $Path"
            continue
        }
        if ($Only -and ($Only -notcontains $name)) { continue }
        if ($value -ne '') {
            Set-TemporaryEnv -Saved $previous -Name $name -Value $value
            $loaded += $name
        }
    }
    Write-Host "Loaded from $(Split-Path -Leaf $Path): $($loaded -join ', ')"
    return $previous
}

function Set-TemporaryEnv {
    param([hashtable]$Saved, [string]$Name, [string]$Value)
    if (-not $Saved.ContainsKey($Name)) {
        $Saved[$Name] = [Environment]::GetEnvironmentVariable($Name, 'Process')
    }
    [Environment]::SetEnvironmentVariable($Name, $Value, 'Process')
}

function Restore-DotEnv {
    param([hashtable]$Saved)
    foreach ($name in $Saved.Keys) {
        [Environment]::SetEnvironmentVariable($name, $Saved[$name], 'Process')
    }
    Write-Host "Restored environment: removed or reset $($Saved.Count) variable(s) set by this script."
}
