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

bb uber

rem call %GRAALVM_HOME%\bin\gu.cmd install native-image

call %GRAALVM_HOME%\bin\native-image.cmd ^
  "-jar" "bin\dictim_jvm.jar" ^
  "-H:Name=bin\dictim" ^
  "-H:+ReportExceptionStackTraces" ^
  "--features=clj_easy.graal_build_time.InitClojureClasses" ^
  "--report-unsupported-elements-at-runtime" ^
  "--verbose" ^
  "--no-fallback" ^
  "--no-server" ^
  "-J-Xmx4g"

if %errorlevel% neq 0 exit /b %errorlevel%

echo Creating zip archive
jar -cMf dictim-%VERSION%-windows-amd64.zip bin\dictim.exe
