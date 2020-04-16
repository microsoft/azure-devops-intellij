param (
    $ClientArchiveUrl = 'https://github.com/microsoft/team-explorer-everywhere/releases/download/14.134.0/TEE-CLC-14.134.0.zip',
    $ClientArchiveHash = 'AF4B7123A09475FF03A3F5662DF3DE614DF2F4ACC33DF16CDAB307B5FB6D7DC7',
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
