if (Test-Path 'clojure.msi') {
  Invoke-WebRequest 'https://github.com/casselc/clj-msi/releases/download/v1.11.3.1463/clojure-1.11.3.1463.msi' -OutFile 'clojure.msi'
}

if (Test-Path -Path 'graalvm') {
  Invoke-WebRequest 'https://github.com/graalvm/graalvm-ce-builds/releases/download/vm-22.3.0/graalvm-ce-java17-windows-amd64-22.3.0.zip'  -OutFile 'graalvm.zip'
  Expand-Archive graalvm.zip graalvm
}

if (Test-Path 'bb.zip') {
  Invoke-WebRequest 'https://github.com/borkdude/babashka/releases/download/v1.0.169/babashka-1.0.169-windows-amd64.zip' -OutFile 'bb.zip'
}
