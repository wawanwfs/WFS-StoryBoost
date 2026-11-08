# WFS StoryBoost Android debug APK builder, for Windows 10/11.
# Requirements: Android Studio SDK Platform 36 & Build Tools 35, JDK 17.
$ErrorActionPreference = "Stop"
Set-Location (Resolve-Path (Join-Path $PSScriptRoot ".."))

$SdkCandidates = @($env:ANDROID_SDK_ROOT, $env:ANDROID_HOME,
  (Join-Path $env:LOCALAPPDATA "Android\Sdk")) | Where-Object { $_ -and (Test-Path $_) }
if (-not $SdkCandidates) {
  throw "Android SDK tidak ditemukan. Instal Android Studio, lalu SDK Platform 35 dan Build Tools 35."
}
$env:ANDROID_HOME = $SdkCandidates[0]
$env:ANDROID_SDK_ROOT = $SdkCandidates[0]
Write-Host "Android SDK: $env:ANDROID_HOME" -ForegroundColor Cyan

if (-not $env:JAVA_HOME) {
  $JdkCandidates = @(
    (Join-Path $env:ProgramFiles "Android\Android Studio\jbr"),
    (Join-Path $env:ProgramFiles "Java\jdk-17"))
  foreach ($j in $JdkCandidates) {
    if (Test-Path (Join-Path $j "bin\java.exe")) { $env:JAVA_HOME = $j; break }
  }
}
if ($env:JAVA_HOME) { $env:PATH = (Join-Path $env:JAVA_HOME "bin") + ";" + $env:PATH }
try { & java -version } catch { throw "JDK 17 belum ditemukan. Instal JDK 17 atau Android Studio." }

$GradleVersion = "8.13"
$ToolsDir = Join-Path (Get-Location) ".tools"
$GradleDir = Join-Path $ToolsDir "gradle-$GradleVersion"
$GradleExe = Join-Path $GradleDir "bin\gradle.bat"
if (-not (Test-Path $GradleExe)) {
  New-Item -ItemType Directory -Force -Path $ToolsDir | Out-Null
  $ZipPath = Join-Path $ToolsDir "gradle-$GradleVersion-bin.zip"
  $Url = "https://services.gradle.org/distributions/gradle-$GradleVersion-bin.zip"
  Write-Host "Mengunduh Gradle $GradleVersion ..." -ForegroundColor Yellow
  Invoke-WebRequest -Uri $Url -OutFile $ZipPath -UseBasicParsing
  Expand-Archive -Path $ZipPath -DestinationPath $ToolsDir -Force
  Remove-Item $ZipPath
}
Write-Host "Building WFS StoryBoost APK..." -ForegroundColor Cyan
& $GradleExe --no-daemon :app:assembleDebug
if ($LASTEXITCODE -ne 0) { throw "Gradle build gagal. Lihat pesan error di atas." }
$Apk = Join-Path (Get-Location) "app\build\outputs\apk\debug\app-debug.apk"
if (-not (Test-Path $Apk)) { throw "Build selesai tanpa file APK." }
$Dist = Join-Path (Get-Location) "dist"
New-Item -ItemType Directory -Force -Path $Dist | Out-Null
Copy-Item $Apk (Join-Path $Dist "WFS-StoryBoost-v2.6.0-debug.apk") -Force
Write-Host "SELESAI: $(Join-Path $Dist 'WFS-StoryBoost-v2.6.0-debug.apk')" -ForegroundColor Green
