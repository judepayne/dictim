@echo off

if not exist bin\dictim.exe (
    echo Binary not found!
    exit /b 1
)

set /P VERSION=< resources\VERSION

mkdir tmp-release 2>nul
copy bin\dictim.exe tmp-release\

cd tmp-release
jar -cMf dictim-%VERSION%-windows-amd64.zip dictim.exe
cd ..

bb release-artifact --file tmp-release\dictim-%VERSION%-windows-amd64.zip

rmdir /s /q tmp-release
