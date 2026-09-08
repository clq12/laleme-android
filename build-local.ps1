param(
  [switch]$Release
)

$env:JAVA_HOME = 'L:\OPENJDK'
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
$env:Path = "$env:JAVA_HOME\bin;$env:ANDROID_HOME\platform-tools;$env:Path"

if ($Release) {
  .\gradlew.bat :app:assembleRelease
  if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
  Copy-Item "app\build\outputs\apk\release\app-release.apk" ".\lale-release.apk" -Force
  Write-Host "Release APK: .\lale-release.apk"
} else {
  .\gradlew.bat :app:assembleDebug
  if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
  Copy-Item "app\build\outputs\apk\debug\app-debug.apk" ".\lale-debug.apk" -Force
  Write-Host "Debug APK: .\lale-debug.apk"
}
