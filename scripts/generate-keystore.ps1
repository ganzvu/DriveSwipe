<#
.SYNOPSIS
    Generates the DriveSwipe release keystore and writes keystore.properties.

.DESCRIPTION
    Run this once on a new machine before building a signed release APK.
    The keystore file and keystore.properties are both .gitignored — they
    must NEVER be committed to the repository.

    Prerequisites: Java (keytool) must be on your PATH.
    Android Studio ships keytool at:
      C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe

.EXAMPLE
    .\scripts\generate-keystore.ps1
    .\scripts\generate-keystore.ps1 -StorePassword "s3cret" -KeyPassword "s3cret"
#>
param(
    [string]$KeystoreFile  = "release-keystore.jks",
    [string]$KeyAlias      = "driveswipe",
    [string]$StorePassword = "",
    [string]$KeyPassword   = "",
    [string]$Dname         = "CN=DriveSwipe, OU=Mobile, O=DriveSwipe, L=Unknown, ST=Unknown, C=US",
    [int]   $Validity      = 10000
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

# ── Locate keytool ────────────────────────────────────────────────────────────
$keytool = Get-Command keytool -ErrorAction SilentlyContinue
if (-not $keytool) {
    $candidate = "C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe"
    if (Test-Path $candidate) { $keytool = $candidate } else {
        Write-Error "keytool not found. Add Java/JDK bin to PATH or install Android Studio."
        exit 1
    }
} else { $keytool = $keytool.Source }

# ── Prompt for passwords if not supplied ─────────────────────────────────────
if (-not $StorePassword) {
    $sp = Read-Host "Enter keystore store password" -AsSecureString
    $StorePassword = [Runtime.InteropServices.Marshal]::PtrToStringAuto(
        [Runtime.InteropServices.Marshal]::SecureStringToBSTR($sp))
}
if (-not $KeyPassword) {
    $kp = Read-Host "Enter key password (leave blank to reuse store password)" -AsSecureString
    $KeyPassword = [Runtime.InteropServices.Marshal]::PtrToStringAuto(
        [Runtime.InteropServices.Marshal]::SecureStringToBSTR($kp))
    if (-not $KeyPassword) { $KeyPassword = $StorePassword }
}

# ── Generate keystore ─────────────────────────────────────────────────────────
$root = Split-Path $PSScriptRoot -Parent
$jksPath = Join-Path $root $KeystoreFile

if (Test-Path $jksPath) {
    Write-Warning "$KeystoreFile already exists — skipping keytool generation."
} else {
    Write-Host "Generating keystore at $jksPath ..."
    & $keytool -genkeypair `
        -v `
        -keystore $jksPath `
        -alias $KeyAlias `
        -keyalg RSA `
        -keysize 2048 `
        -validity $Validity `
        -storepass $StorePassword `
        -keypass $KeyPassword `
        -dname $Dname
    if ($LASTEXITCODE -ne 0) { Write-Error "keytool failed."; exit 1 }
    Write-Host "Keystore created." -ForegroundColor Green
}

# ── Write keystore.properties ─────────────────────────────────────────────────
$propsPath = Join-Path $root "keystore.properties"
@"
storeFile=$KeystoreFile
storePassword=$StorePassword
keyAlias=$KeyAlias
keyPassword=$KeyPassword
"@ | Set-Content -Encoding UTF8 $propsPath

Write-Host "keystore.properties written to $propsPath" -ForegroundColor Green
Write-Host ""
Write-Host "IMPORTANT:" -ForegroundColor Yellow
Write-Host "  - Back up $KeystoreFile securely (password manager, encrypted drive)."
Write-Host "  - NEVER commit the .jks file or keystore.properties to Git."
Write-Host "  - Use the SAME keystore for every future release of this app."
Write-Host ""
Write-Host "To build the signed release APK, run from the project root:"
Write-Host "  .\gradlew assembleRelease" -ForegroundColor Cyan
