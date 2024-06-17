Write-Host 'checking large downloads in cache'

if (Test-Path -Path 'clojure.msi' -PathType Leaf) {
  Write-Host 'Clojure available from cache'
} else {
  Write-Host 'downloading Clojure..'
  Invoke-WebRequest 'https://github.com/casselc/clj-msi/releases/download/v1.11.3.1463/clojure-1.11.3.1463.msi' -OutFile 'clojure.msi'
}

if (Test-Path -Path 'graalvm') {
  Write-Host 'graalvm available from cache'
} else {
  Write-Host 'downloading graalvm..'
  Invoke-WebRequest 'https://github.com/graalvm/graalvm-ce-builds/releases/download/vm-22.3.0/graalvm-ce-java17-windows-amd64-22.3.0.zip'  -OutFile 'graalvm.zip'
  Write-Host 'expanding graalvm archive'
  Expand-Archive graalvm.zip graalvm
}

if (Test-Path -Path 'bb.zip' -PathType Leaf) {
  Write-Host 'Babashka available from cache'
} else {
  Write-Host 'downloading Babashka..'
  Invoke-WebRequest 'https://github.com/borkdude/babashka/releases/download/v1.3.190/babashka-1.3.190-windows-amd64.zip' -OutFile 'bb.zip'
}
  Invoke-WebRequest 'https://github.com/borkdude/babashka/releases/download/v1.3.190/babashka-1.3.190-windows-amd64.zip' -OutFile 'bb.zip'