param (
    $ClientArchiveUrl = 'https://github.com/JetBrains/team-explorer-everywhere/releases/download/14.135.2/TEE-CLC-14.135.2.zip',
    $ClientArchiveHash = '27B3346D1CDCD3E9431D21942BAB5C6EFBBB0AFC945B71BF332485FE214CB338',
    $ClientArchiveStorage = "$PSScriptRoot/.download-cache",
    $ClientInstallPath = "$PSScriptRoot/.installed/tfs-clc"
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$fileName = [IO.Path]::GetFileName(([Uri] $ClientArchiveUrl).LocalPath)
$filePath = [IO.Path]::Join($ClientArchiveStorage, $fileName)

if (!(Test-Path $filePath)) {
    Write-Output "Downloading $ClientArchiveUrl"
    New-Item -Type Directory $ClientArchiveStorage -ErrorAction SilentlyContinue | Out-Null
    Invoke-WebRequest -UseBasicParsing $ClientArchiveUrl -OutFile $filePath
}

Write-Output "Verifying hash of file $filePath"
$actualHash = (Get-FileHash $filePath -Algorithm SHA256).Hash
if ($actualHash -ne $ClientArchiveHash) {
    Write-Output "Hashes don't correspond, removing partially downloaded file"
    Remove-Item $filePath -Force
    throw "Actual downloaded file hash ($actualHash) doesn't correspond to expected hash ($ClientArchiveHash)"
}

Write-Output "Expanding archive to $ClientInstallPath"
New-Item -Type Directory $ClientInstallPath  -ErrorAction SilentlyContinue | Out-Null
if ($IsWindows) { # Expand-Archive doesn't preserve the Unix permissions, so could only be used on Windows
    Expand-Archive $filePath $ClientInstallPath
} else {
    unzip -d $ClientInstallPath $filePath
    if (!$?) {
        throw "unzip exited with code $LASTEXITCODE"
    }
}
