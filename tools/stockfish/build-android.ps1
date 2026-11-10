# Compila Stockfish 17.1 per Android (NDK r27) senza rete NNUE incorporata: l'app scarica
# le reti al primo uso e le passa al motore con "setoption name EvalFile/EvalFileSmall".
# Uscita: jniLibs/<abi>/libstockfish.so (eseguibile: Android lo estrae in nativeLibraryDir,
# l'unica cartella da cui un'app può eseguire un programma).
param([string]$OutDir = "C:\Projects\ChessoraAndroid\app\src\main\jniLibs")
$ErrorActionPreference = 'Stop'
$ndk = "C:\Android\Sdk\ndk\27.2.12479018\toolchains\llvm\prebuilt\windows-x86_64\bin"
$src = "C:\Projects\stockfish-build\Stockfish-sf_17.1\src"
$sources = Get-ChildItem $src -Recurse -Filter *.cpp | ForEach-Object { $_.FullName }

$common = @('-O3', '-std=c++17', '-DNDEBUG', '-DNNUE_EMBEDDING_OFF', '-DUSE_POPCNT', '-fPIE', '-pthread',
    '-fno-exceptions', '-funroll-loops', '-stdlib=libc++', '-Wno-everything')
$targets = @{
    'arm64-v8a'   = @('--target=aarch64-linux-android26', '-DIS_64BIT', '-DUSE_NEON=8')
    'armeabi-v7a' = @('--target=armv7a-linux-androideabi26', '-mthumb', '-march=armv7-a', '-mfloat-abi=softfp', '-mfpu=neon', '-DUSE_NEON=7')
    'x86_64'      = @('--target=x86_64-linux-android26', '-DIS_64BIT', '-msse4.1', '-mssse3', '-msse2', '-mpopcnt',
                      '-DUSE_SSE41', '-DUSE_SSSE3', '-DUSE_SSE2')
}
foreach ($abi in $targets.Keys) {
    $dir = Join-Path $OutDir $abi
    New-Item -ItemType Directory -Force $dir | Out-Null
    $out = Join-Path $dir 'libstockfish.so'
    Write-Host "== $abi"
    # -z max-page-size=16384: segmenti allineati a 16 KB, richiesto da Google Play (dispositivi
    # Android 15+ con pagine di memoria da 16 KB) - senza, Play Console rifiuta il bundle.
    & "$ndk\clang++.exe" @($targets[$abi]) @common @sources '-static-libstdc++' '-pie' '-lm' `
        '-Wl,-z,max-page-size=16384' '-Wl,-z,common-page-size=16384' '-o' $out
    if ($LASTEXITCODE -ne 0) { throw "compilazione $abi fallita" }
    & "$ndk\llvm-strip.exe" $out
    "{0}: {1:N2} MB" -f $abi, ((Get-Item $out).Length / 1MB)
}
