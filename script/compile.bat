rem @echo off

if "%GRAALVM_HOME%"=="" (
    echo Please set GRAALVM_HOME
    exit /b
)

set JAVA_HOME=%GRAALVM_HOME%
set PATH=%GRAALVM_HOME%\bin;%PATH%


set /P VERSION=< resources\VERSION
echo Building version %VERSION%

if "%GRAALVM_HOME%"=="" (
echo Please set GRAALVM_HOME
exit /b
)

clojure.exe -T:build uber

rem call %GRAALVM_HOME%\bin\gu.cmd install native-image

rem call %GRAALVM_HOME%\bin\native-image.cmd ^
rem   "-jar" "bin\dictim_jvm.jar" ^
rem   "-H:Name=bin\dictim.exe" ^
rem   "-H:+ReportExceptionStackTraces" ^
rem   "-H:-CheckToolchain" ^
rem   "--features=clj_easy.graal_build_time.InitClojureClasses" ^
rem   "--report-unsupported-elements-at-runtime" ^
rem   "--verbose" ^
rem   "--no-fallback" ^
rem   "--no-server" ^
rem   "-J-Xmx3g"

rem if %errorlevel% neq 0 exit /b %errorlevel%

rem echo Creating zip archive
rem jar -cMf dictim-%VERSION%-windows-amd64.zip bin\dictim.exe
