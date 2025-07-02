@echo off

if not exist bin\dictim.exe (
    echo Binary not found!
    exit /b 1
)

set /P VERSION=< resources\VERSION

mkdir tmp-release 2>nul
copy bin\dictim.exe tmp-release\

cd tmp-release
REM Create zip file
jar -cMf dictim-%VERSION%-windows-amd64.zip dictim.exe

REM Create properly named exe file for WinGet
copy dictim.exe dictim-%VERSION%-windows-amd64.exe
cd ..

REM Upload both zip and exe formats
bb.exe release-artifact --file tmp-release\dictim-%VERSION%-windows-amd64.zip
bb.exe release-artifact --file tmp-release\dictim-%VERSION%-windows-amd64.exe

rmdir /s /q tmp-release
