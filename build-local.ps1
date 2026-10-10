param(
  [switch]$Release
)

# Builds the app without Android Studio.
# Works on any machine: JDK 17+ and the Android SDK are auto-detected, and you
# can always override them by setting JAVA_HOME / ANDROID_HOME beforehand.
#
#   .\build-local.ps1            -> debug APK   (lale-debug.apk)
#   .\build-local.ps1 -Release   -> signed APK  (lale-release.apk, needs signing.properties)

$root = $PSScriptRoot

function Find-JavaHome {
  $candidates = @()
  if ($env:JAVA_HOME) { $candidates += $env:JAVA_HOME }
  $candidates += 'C:\Android\jdk-17'
  if (Test-Path 'C:\Android') {
    $candidates += (Get-ChildItem 'C:\Android' -Directory -ErrorAction SilentlyContinue |
      Where-Object { $_.Name -like 'jdk*' } | ForEach-Object { $_.FullName })
  }
  $candidates += 'L:\OPENJDK'
  foreach ($base in @('C:\Program Files\Eclipse Adoptium', 'C:\Program Files\Java',
      'C:\Program Files\Microsoft', 'C:\Program Files\Android\Android Studio\jbr')) {
    if (Test-Path $base) {
      $candidates += (Get-ChildItem $base -Directory -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -like 'jdk*' -or $_.Name -like '*jbr*' } |
        ForEach-Object { $_.FullName })
    }
  }
  foreach ($c in $candidates) {
    if ($c -and (Test-Path (Join-Path $c 'bin\java.exe'))) { return $c }
  }
  return $null
}

function Find-AndroidHome {
  $candidates = @()
  if ($env:ANDROID_HOME) { $candidates += $env:ANDROID_HOME }
  if ($env:ANDROID_SDK_ROOT) { $candidates += $env:ANDROID_SDK_ROOT }
  $candidates += 'C:\Android\Sdk'
  if ($env:LOCALAPPDATA) { $candidates += (Join-Path $env:LOCALAPPDATA 'Android\Sdk') }
  foreach ($c in $candidates) {
    if ($c -and (Test-Path (Join-Path $c 'platform-tools'))) { return $c }
  }
  return $null
}

$javaHome = Find-JavaHome
if (-not $javaHome) {
  Write-Error "JDK not found. Install a JDK 17, then set JAVA_HOME or place it at C:\Android\jdk-17."
  exit 1
}

$androidHome = Find-AndroidHome
if (-not $androidHome) {
  Write-Error "Android SDK not found. Set ANDROID_HOME or install the SDK to C:\Android\Sdk."
  exit 1
}

$env:JAVA_HOME = $javaHome
$env:ANDROID_HOME = $androidHome
$env:ANDROID_SDK_ROOT = $androidHome
$env:Path = "$env:JAVA_HOME\bin;$env:ANDROID_HOME\platform-tools;$env:Path"

Write-Host "JAVA_HOME    = $env:JAVA_HOME"
Write-Host "ANDROID_HOME = $env:ANDROID_HOME"

# Runs one Gradle task. Normally gradlew.bat does the job; some restricted
# hosts refuse to start .bat/.cmd files, so fall back to launching the wrapper
# jar with java directly - same wrapper, same distribution, same cache.
function Invoke-GradleTask {
  param([string]$Task)

  $gradlew = Join-Path $root 'gradlew.bat'
  if (Test-Path $gradlew) {
    $global:LASTEXITCODE = $null
    & $gradlew $Task
    if ($null -ne $LASTEXITCODE) { return $LASTEXITCODE }
    Write-Host "gradlew.bat could not be started here - using the wrapper jar instead."
  }

  $wrapperJar = Join-Path $root 'gradle\wrapper\gradle-wrapper.jar'
  if (-not (Test-Path $wrapperJar)) {
    Write-Error "Gradle wrapper jar missing: $wrapperJar"
    return 1
  }
  & (Join-Path $env:JAVA_HOME 'bin\java.exe') `
    -classpath $wrapperJar org.gradle.wrapper.GradleWrapperMain $Task
  return $LASTEXITCODE
}

Push-Location $root
try {
  if ($Release) {
    $code = Invoke-GradleTask ':app:assembleRelease'
    if ($code -ne 0) { exit $code }
    $src = Join-Path $root 'app\build\outputs\apk\release\app-release.apk'
    $dst = Join-Path $root 'lale-release.apk'
  } else {
    $code = Invoke-GradleTask ':app:assembleDebug'
    if ($code -ne 0) { exit $code }
    $src = Join-Path $root 'app\build\outputs\apk\debug\app-debug.apk'
    $dst = Join-Path $root 'lale-debug.apk'
  }

  if (-not (Test-Path $src)) {
    Write-Error "Gradle finished but produced no APK at $src. Run the build with -i for details."
    exit 1
  }
  Copy-Item $src $dst -Force
  $size = [math]::Round((Get-Item $dst).Length / 1MB, 1)
  Write-Host "APK built OK: $dst ($size MB)"
} finally {
  Pop-Location
}
