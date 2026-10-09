#Requires -Version 5.1
param(
    [string]$Database = 'geosoa',
    [string]$AppUser = 'soauser',
    [string]$AppPassword = 'soapass',
    [int]$Port = 3306
)

$ErrorActionPreference = 'Stop'

function Write-Step($Message) { Write-Host "==> $Message" -ForegroundColor Cyan }
function Write-Ok($Message) { Write-Host "    $Message" -ForegroundColor Green }

function Test-Admin {
    $identity = [Security.Principal.WindowsIdentity]::GetCurrent()
    ([Security.Principal.WindowsPrincipal]$identity).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)
}

function Restart-AsAdmin {
    if (Test-Admin) { return }
    Write-Host '    Se necesitan permisos de administrador, se abrira una nueva ventana.' -ForegroundColor Yellow
    $arguments = "-NoProfile -ExecutionPolicy Bypass -NoExit -File `"$PSCommandPath`" -Database $Database -AppUser $AppUser -AppPassword $AppPassword -Port $Port"
    Start-Process powershell -Verb RunAs -ArgumentList $arguments
    exit
}

function Find-MySqlBin {
    $command = Get-Command mysql.exe -ErrorAction SilentlyContinue
    if ($command) { return Split-Path $command.Source }
    $client = Get-ChildItem "$env:ProgramFiles\MySQL\MySQL Server *\bin\mysql.exe" -ErrorAction SilentlyContinue |
        Sort-Object FullName -Descending | Select-Object -First 1
    if ($client) { return $client.DirectoryName }
    return $null
}

function Install-MySql {
    if (-not (Get-Command winget.exe -ErrorAction SilentlyContinue)) {
        throw 'winget no esta disponible. Instala "App Installer" desde Microsoft Store o MySQL manualmente.'
    }
    Restart-AsAdmin
    winget install --exact --id Oracle.MySQL --silent --accept-package-agreements --accept-source-agreements
    if ($LASTEXITCODE -ne 0) { throw "winget fallo con codigo $LASTEXITCODE." }
}

function Get-MySqlService {
    Get-Service -ErrorAction SilentlyContinue | Where-Object { $_.Name -like 'MySQL*' } | Select-Object -First 1
}

# El MSI de winget solo copia binarios: hay que inicializar datos y registrar el servicio.
# --initialize-insecure deja root sin contrasena, igual que una instalacion por defecto.
function Register-MySqlService($BinDir) {
    Restart-AsAdmin
    $baseDir = Split-Path $BinDir
    $dataRoot = Join-Path $env:ProgramData "MySQL\$(Split-Path $baseDir -Leaf)"
    $dataDir = Join-Path $dataRoot 'Data'
    $iniPath = Join-Path $dataRoot 'my.ini'
    New-Item -ItemType Directory -Force -Path $dataRoot | Out-Null

    @(
        '[mysqld]'
        "basedir=`"$($baseDir -replace '\\', '/')`""
        "datadir=`"$($dataDir -replace '\\', '/')`""
        "port=$Port"
    ) | Set-Content -Path $iniPath -Encoding ASCII

    $mysqld = Join-Path $BinDir 'mysqld.exe'
    if (-not (Test-Path $dataDir)) {
        & $mysqld "--defaults-file=$iniPath" --initialize-insecure --console | Out-Host
        if ($LASTEXITCODE -ne 0) { throw 'No se pudo inicializar el directorio de datos.' }
    }
    & $mysqld --install MySQL "--defaults-file=$iniPath" | Out-Host
    if ($LASTEXITCODE -ne 0) { throw 'No se pudo registrar el servicio MySQL.' }
    Get-Service MySQL
}

function Invoke-MySql($Credential, $Sql) {
    $env:MYSQL_PWD = $Credential.Password
    $previous = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    try {
        $output = & $script:MySqlExe --protocol=TCP -h 127.0.0.1 -P $Port -u $Credential.User -N -B -e $Sql 2>&1
        [pscustomobject]@{ Success = ($LASTEXITCODE -eq 0); Output = ($output | Out-String).Trim() }
    }
    finally {
        $ErrorActionPreference = $previous
        Remove-Item Env:MYSQL_PWD -ErrorAction SilentlyContinue
    }
}

function Wait-MySql {
    $probe = @{ User = 'root'; Password = '' }
    for ($i = 0; $i -lt 30; $i++) {
        $result = Invoke-MySql $probe 'SELECT 1'
        if ($result.Success -or $result.Output -match 'Access denied') { return }
        Start-Sleep -Seconds 1
    }
    throw "MySQL no responde en el puerto $Port."
}

function Get-AdminCredential {
    $credential = @{ User = 'root'; Password = '' }
    if ((Invoke-MySql $credential 'SELECT 1').Success) {
        Write-Ok 'Conectado como root sin contrasena.'
        return $credential
    }
    Write-Host '    Las credenciales por defecto (root sin contrasena) no funcionaron.' -ForegroundColor Yellow
    for ($attempt = 1; $attempt -le 3; $attempt++) {
        $user = Read-Host '    Usuario administrador de MySQL [root]'
        if (-not $user) { $user = 'root' }
        $secure = Read-Host '    Contrasena' -AsSecureString
        $plain = [Runtime.InteropServices.Marshal]::PtrToStringAuto(
            [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure))
        $credential = @{ User = $user; Password = $plain }
        $result = Invoke-MySql $credential 'SELECT 1'
        if ($result.Success) {
            Write-Ok "Conectado como $user."
            return $credential
        }
        Write-Host "    $($result.Output)" -ForegroundColor Red
    }
    throw 'No se pudo autenticar en MySQL.'
}

Write-Step 'Verificando instalacion de MySQL'
$binDir = Find-MySqlBin
if (-not $binDir) {
    Write-Host '    MySQL no esta instalado, instalando con winget...' -ForegroundColor Yellow
    Install-MySql
    $binDir = Find-MySqlBin
    if (-not $binDir) { throw 'MySQL se instalo pero no se encontro mysql.exe.' }
}
$script:MySqlExe = Join-Path $binDir 'mysql.exe'
Write-Ok "Encontrado en $binDir"

Write-Step 'Verificando servicio de MySQL'
$service = Get-MySqlService
if (-not $service) {
    Write-Host '    No hay servicio registrado, configurandolo...' -ForegroundColor Yellow
    $service = Register-MySqlService $binDir
}
if ($service.Status -ne 'Running') {
    Restart-AsAdmin
    Start-Service $service.Name
}
Wait-MySql
Write-Ok "Servicio $($service.Name) en ejecucion."

Write-Step 'Autenticando'
$admin = Get-AdminCredential

Write-Step "Verificando base de datos '$Database'"
$exists = Invoke-MySql $admin "SELECT SCHEMA_NAME FROM information_schema.SCHEMATA WHERE SCHEMA_NAME = '$Database'"
if (-not $exists.Success) { throw $exists.Output }
if ($exists.Output -eq $Database) {
    Write-Ok 'La base de datos ya existe.'
}
else {
    $created = Invoke-MySql $admin "CREATE DATABASE ``$Database`` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci"
    if (-not $created.Success) { throw $created.Output }
    Write-Ok 'Base de datos creada.'
}

Write-Step "Verificando usuario '$AppUser' del backend"
$grant = Invoke-MySql $admin (
    "CREATE USER IF NOT EXISTS '$AppUser'@'localhost' IDENTIFIED BY '$AppPassword'; " +
    "GRANT ALL PRIVILEGES ON ``$Database``.* TO '$AppUser'@'localhost'; FLUSH PRIVILEGES;")
if (-not $grant.Success) { throw $grant.Output }
Write-Ok 'Usuario listo con permisos sobre la base de datos.'

Write-Host ''
Write-Host "Listo: MySQL en localhost:$Port, base '$Database', usuario '$AppUser'." -ForegroundColor Green
