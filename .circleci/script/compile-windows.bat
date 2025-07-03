@echo off

if "%GRAALVM_HOME%"=="" (
    echo Please set GRAALVM_HOME
    exit /b 1
)

set JAVA_HOME=%GRAALVM_HOME%
set PATH=%GRAALVM_HOME%\bin;%PATH%

set /P VERSION=< resources\VERSION
echo Building version %VERSION%

bb uber

call %GRAALVM_HOME%\bin\native-image.cmd ^
  "-jar" "bin\dictim_jvm.jar" ^
  "-H:Name=bin\dict" ^
  "-H:+ReportExceptionStackTraces" ^
  "--features=clj_easy.graal_build_time.InitClojureClasses" ^
  "--report-unsupported-elements-at-runtime" ^
  "--verbose" ^
  "--no-fallback" ^
  "--no-server" ^
  "-J-Xmx4g"

if %errorlevel% neq 0 exit /b %errorlevel%
