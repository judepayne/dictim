rem @echo off

if "%GRAALVM_HOME%"=="" (
    echo Please set GRAALVM_HOME
    exit /b
)

set JAVA_HOME=%GRAALVM_HOME%
set PATH=%GRAALVM_HOME%\bin;%PATH%
rem add location of C++ compiler, cl.exe, to PATH. Needed by Native Build
set PATH=C:\Program Files (x86)\Microsoft Visual Studio 14.0\VC\bin\amd64_x86;%PATH%

set /P VERSION=< resources\VERSION
echo Building version %VERSION%

if "%GRAALVM_HOME%"=="" (
echo Please set GRAALVM_HOME
exit /b
)

clojure.exe -T:build uber


call %GRAALVM_HOME%\bin\gu.cmd install native-image

call %GRAALVM_HOME%\bin\native-image.cmd ^
  "-jar" "bin\dictim_jvm.jar" ^
  "-H:Name=bin\dictim.exe" ^
  "-H:+ReportExceptionStackTraces" ^
  "--features=clj_easy.graal_build_time.InitClojureClasses" ^
  "--report-unsupported-elements-at-runtime" ^
  "--verbose" ^
  "--no-fallback" ^
  "--no-server" ^
  "-J-Xmx3g"

if %errorlevel% neq 0 exit /b %errorlevel%

echo Creating zip archive
jar -cMf dictim-%VERSION%-windows-amd64.zip bin\dictim.exe
