$ErrorActionPreference = "Stop"

$RootDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$BuildDir = Join-Path $RootDir ".verify-build"
$ClassesDir = Join-Path $BuildDir "classes"
$SourceList = Join-Path $BuildDir "sources.txt"

if (Test-Path $BuildDir) {
    Remove-Item -Recurse -Force $BuildDir
}

New-Item -ItemType Directory -Force $ClassesDir | Out-Null

$SourceRoots = @(
    "message-api/src/main/java",
    "message-spi/src/main/java",
    "message-core/src/main/java",
    "message-testkit/src/main/java",
    "message-demo/src/main/java"
)

$Sources = foreach ($SourceRoot in $SourceRoots) {
    Get-ChildItem -Recurse -Filter *.java (Join-Path $RootDir $SourceRoot) |
        ForEach-Object { $_.FullName }
}

$Sources | Sort-Object | Set-Content -Encoding UTF8 $SourceList

javac --release 17 -Xlint:all -Werror -d $ClassesDir "@$SourceList"
java -cp $ClassesDir com.xjtu.iron.message.demo.InMemoryMessageDemo
java -cp $ClassesDir com.xjtu.iron.message.demo.MessageModelContractVerifier
