#requires -Version 5.1
<#
WFS StoryBoost synthetic AI memory QA on physical ARM64, Windows 10/11.
Does NOT install/uninstall APKs, change photos, sign releases or upload data.
Default is diagnostics-only. -RunInstrumented force-stops the StoryBoost app.
Both app and corresponding androidTest APK must be installed already.
Do NOT run legacy XNNPACK-on on a low-RAM phone.
#>
[CmdletBinding()]
param(
    [string]$Serial = '',
    [int]$Repetitions = 5,
    [string]$OutputDirectory = '.\qa-physical-results',
    [switch]$RunInstrumented
)
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
if ($Repetitions -lt 1 -or $Repetitions -gt 15) {
    throw 'Repetitions must be 1 through 15'
}
if (-not (Get-Command adb -ErrorAction SilentlyContinue)) {
    throw 'adb.exe not found. Add Android SDK Platform-Tools to PATH.'
}
$attached = @(& adb devices | Select-Object -Skip 1 |
    Where-Object { $_ -match '^\S+\s+device(\s|$)' })
if (-not $Serial) {
    if ($attached.Count -ne 1) {
        throw "Supply -Serial when more than one device is connected."
    }
    $Serial = ($attached[0] -split '\s+')[0]
}
if (-not (@($attached | Where-Object { ($_ -split '\s+')[0] -eq $Serial }).Count -eq 1)) {
    throw "ADB device $Serial is not authorized or not connected"
}
function Invoke-Device([string[]]$CommandArgs) {
    $r = @(& adb -s $Serial @CommandArgs 2>&1)
    if ($LASTEXITCODE -ne 0) {
        throw ("adb failed: " + ($CommandArgs -join ' ') + [Environment]::NewLine +
            ($r -join [Environment]::NewLine))
    }
    return $r
}
$apiText = (Invoke-Device -CommandArgs @('shell','getprop','ro.build.version.sdk') | Select-Object -Last 1).Trim()
$abi = (Invoke-Device -CommandArgs @('shell','getprop','ro.product.cpu.abi') | Select-Object -Last 1).Trim()
if ($apiText -notmatch '^\d+$') { throw "Unrecognized Android SDK: $apiText" }
$api = [int]$apiText
if ($api -lt 29 -or $abi -ne 'arm64-v8a') {
    throw "ARM64 Android10+ required; observed API=$api, ABI=$abi"
}
$utc = (Get-Date).ToUniversalTime().ToString('yyyyMMddTHHmmssZ')
$folder = Join-Path $OutputDirectory ("api{0}-{1}" -f $api,$utc)
New-Item -ItemType Directory -Force -Path $folder | Out-Null
@(
    'WFS StoryBoost Physical QA - synthetic DnCNN',
    "UTC=$utc", "AndroidAPI=$api", "ABI=$abi",
    "Instrumentation=$RunInstrumented", "Repetitions=$Repetitions",
    'Only consumer-default XNNPACK-OFF; no legacy ON comparison',
    'No physical low-RAM guarantee from a single device',
    'WARNING: -RunInstrumented force-stops StoryBoost; finish edits first'
) | Set-Content (Join-Path $folder 'scope.txt')
Invoke-Device -CommandArgs @('shell','dumpsys','meminfo','com.wfs.storyboost.studio') |
    Out-File (Join-Path $folder 'initial-meminfo.txt')
Invoke-Device -CommandArgs @('shell','dumpsys','battery') |
    Out-File (Join-Path $folder 'battery.txt')
try {
    Invoke-Device -CommandArgs @('shell','dumpsys','thermalservice') |
        Out-File (Join-Path $folder 'thermal.txt')
} catch {
    $_.Exception.Message | Out-File (Join-Path $folder 'thermal.txt')
}
if (-not $RunInstrumented) {
    Write-Host "Diagnostics only: $folder"
    Write-Host 'Install matching debug app + androidTest APKs, then use -RunInstrumented to exercise synthetic AI.'
    exit 0
}
$packages = @(Invoke-Device -CommandArgs @('shell','pm','list','packages','com.wfs.storyboost.studio'))
if (-not ($packages | Where-Object { $_.Trim() -eq 'package:com.wfs.storyboost.studio' })) {
    throw 'StoryBoost app missing. This script never automatically installs or uninstalls.'
}
$runner = 'com.wfs.storyboost.studio.test/androidx.test.runner.AndroidJUnitRunner'
$test = 'com.wfs.storyboost.DncnnTilingQaTest#colorNoiseTiledDenoiseReportsPairedQualityAndSeamContinuity'
$csv = New-Object 'System.Collections.Generic.List[string]'
$csv.Add('api,repeat,pid,sampledPeakPssKB,pssBeforeKB,pssAfterKB,inferMs,pipelineMs,pairedPsnr,pairedSsim')
$prevPid = ''
for ($n=1; $n -le $Repetitions; $n++) {
    Invoke-Device -CommandArgs @('shell','am','force-stop','com.wfs.storyboost.studio') | Out-Null
    $result = @(Invoke-Device -CommandArgs @(
        'shell','am','instrument','-w','-r','-e','class',$test,
        '-e','wfs.ai.threads','2','-e','wfs.ai.xnnpack','default',$runner))
    $result | Set-Content (Join-Path $folder ("instrument-$n.txt"))
    if (($result -join ' ') -notmatch 'OK \(1 test\)') {
        throw "Run $n did not pass. Review instrumentation logs; low-RAM guard refusal is NOT an inference pass."
    }
    $log = @(Invoke-Device -CommandArgs @('logcat','-d','-s','WfsDncnnTileQA:I','*:S'))
    $record = $log | Where-Object { $_ -match 'qaCpuThreads=2 qaXnnpack=default' } |
        Select-Object -Last 1
    if (-not $record) { throw "Missing real inference memory report at run $n" }
    $record | Set-Content (Join-Path $folder ("memory-$n.txt"))
    $values = @{}
    foreach ($key in @('pid','sampledPeakPssKB','pssBeforeKB',
        'pssAfterKB','inferMs','pipelineMs','pairedPsnr','pairedSsim')) {
        $regex = '(?:^|\s)' + $key + '=(\d+(?:\.\d+)?)'
        $match = [regex]::Match($record,$regex)
        if (-not $match.Success) { throw "Missing memory/quality field $key in run $n" }
        $values[$key] = $match.Groups[1].Value
    }
    if ($prevPid -eq $values['pid']) { throw 'PID reused across consecutive runs' }
    $prevPid = $values['pid']
    if ([double]$values['pairedPsnr'] -lt 28.55 -or
        [double]$values['pairedSsim'] -lt 0.730) {
        throw "Paired synthetic model quality regression on run $n"
    }
    $row = @($api,$n,$values['pid'],$values['sampledPeakPssKB'],
        $values['pssBeforeKB'],$values['pssAfterKB'],
        $values['inferMs'],$values['pipelineMs'],
        $values['pairedPsnr'],$values['pairedSsim']) -join ','
    $csv.Add($row)
    $csv | Set-Content (Join-Path $folder 'measurements.csv')
}
("PASS " + $Repetitions + " ARM64 real pinned DnCNN inferences with distinct processes") |
    Set-Content (Join-Path $folder 'summary.txt')
Write-Host "Physical synthetic QA passed. Evidence: $folder"
Write-Host 'No Google Photos-owned URI, owner-signed APK or real-person portrait proof is implied.'
