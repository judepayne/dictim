@echo off

set /P VERSION=< resources\VERSION
echo Creating ARM64 release for version %VERSION%

if not exist "bin\dict-arm64.exe" (
    echo ARM64 binary not found!
    exit /b 1
)

echo Creating zip archive
jar -cMf dict-%VERSION%-windows-arm64.zip bin\dict-arm64.exe

echo Releasing to GitHub
gh release upload v%VERSION% dict-%VERSION%-windows-arm64.zip --clobber