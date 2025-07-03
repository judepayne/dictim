@echo off

if not exist bin\dict.exe (
    echo Binary not found!
    exit /b 1
)

set /P VERSION=< resources\VERSION

mkdir tmp-release 2>nul
copy bin\dict.exe tmp-release\

cd tmp-release
REM Create zip file
jar -cMf dict-%VERSION%-windows-amd64.zip dict.exe

REM Create properly named exe file for WinGet
copy dict.exe dict-%VERSION%-windows-amd64.exe
cd ..

REM Upload both zip and exe formats
bb.exe release-artifact --file tmp-release\dict-%VERSION%-windows-amd64.zip
bb.exe release-artifact --file tmp-release\dict-%VERSION%-windows-amd64.exe

rmdir /s /q tmp-release
