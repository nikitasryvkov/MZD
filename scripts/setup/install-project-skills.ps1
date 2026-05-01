param(
    [string]$RepoOwner = "msitarzewski",
    [string]$RepoName = "agency-agents",
    [string[]]$Agents = @(
    "engineering/engineering-backend-architect.md",
    "engineering/engineering-frontend-developer.md",
    "engineering/engineering-devops-automator.md",
    "engineering/engineering-data-engineer.md",
    "engineering/engineering-security-engineer.md",
    "engineering/engineering-technical-writer.md",
    "testing/testing-api-tester.md";
    )
)

$ErrorActionPreference = "Stop"

$ProjectRoot = Get-Location
$SkillsRoot = Join-Path $ProjectRoot ".agents\skills"

New-Item -ItemType Directory -Force -Path $SkillsRoot | Out-Null

function Get-Frontmatter {
    param([string]$Content)

    if ($Content -notmatch '(?s)^---\s*\r?\n(.*?)\r?\n---\s*\r?\n?') {
        throw "Could not find YAML frontmatter block."
    }

    $frontmatter = $matches[1]
    $result = @{}

    foreach ($line in ($frontmatter -split "`r?`n")) {
        if ($line -match '^\s*([A-Za-z0-9_-]+)\s*:\s*(.+?)\s*$') {
            $key = $matches[1].Trim()
            $value = $matches[2].Trim().Trim('"').Trim("'")
            $result[$key] = $value
        }
    }

    return $result
}

function Normalize-SkillName {
    param([string]$Name)

    $normalized = $Name.ToLower()
    $normalized = $normalized -replace '[^a-z0-9]+', '-'
    $normalized = $normalized -replace '^-+', ''
    $normalized = $normalized -replace '-+$', ''

    if (-not $normalized.StartsWith("agency-")) {
        $normalized = "agency-" + $normalized
    }

    return $normalized
}

foreach ($agentPath in $Agents) {
    $rawUrl = "https://raw.githubusercontent.com/$RepoOwner/$RepoName/main/$agentPath"
    Write-Host "Downloading $rawUrl"

    $content = Invoke-WebRequest -UseBasicParsing -Uri $rawUrl | Select-Object -ExpandProperty Content

    $fm = Get-Frontmatter -Content $content

    $name = $fm["name"]
    $description = $fm["description"]

    if (-not $name) {
        throw "Could not extract name from $agentPath"
    }

    if (-not $description) {
        $description = "Converted from $agentPath"
    }

    $skillName = Normalize-SkillName -Name $name

    $skillDir = Join-Path $SkillsRoot $skillName
    $skillFile = Join-Path $skillDir "SKILL.md"

    New-Item -ItemType Directory -Force -Path $skillDir | Out-Null

    if ($content -match '(?s)^---\s*\r?\n.*?\r?\n---\s*\r?\n?(.*)$') {
        $body = $matches[1].Trim()
    } else {
        $body = $content.Trim()
    }

    $skillMd = @"
---
name: $skillName
description: $description
---

# Source

Converted from ${RepoOwner}/${RepoName}: ${agentPath}

# Instructions

$body
"@

    Set-Content -Path $skillFile -Value $skillMd -Encoding UTF8
    Write-Host "Installed $skillName -> $skillFile"
}

Write-Host ""
Write-Host "Done."
Write-Host "Skills folder: $SkillsRoot"
Write-Host "Restart Codex completely, then reopen this project."