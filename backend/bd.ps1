# Abre el administrador visual de HSQLDB conectado a la base del proyecto (database/linkshortener).
# Solo para consultar: escribir  SELECT * FROM link;  y tocar Execute SQL.
# Uso, desde backend/:  .\bd.ps1
#
# HSQLDB en modo archivo admite un solo proceso a la vez: el servidor tiene que estar apagado,
# y hay que cerrar el administrador antes de volver a levantarlo.

$database = (Resolve-Path "$PSScriptRoot\..\database").Path -replace '\\', '/'

# 1. El Link Shortener no tiene que estar corriendo.
$running = Get-CimInstance Win32_Process -Filter "Name = 'java.exe'" |
	Where-Object { $_.CommandLine -like '*app.LinkShortenerApplication*' }
if ($running) {
	Write-Host "El servidor del Link Shortener está levantado (PID $($running.ProcessId -join ', '))." -ForegroundColor Red
	Write-Host "Apagalo primero: la base no se puede abrir mientras el servidor la usa." -ForegroundColor Red
	exit 1
}

# 2. Buscar el jar de HSQLDB que descargó Gradle (la versión más reciente).
$jar = Get-ChildItem "$env:USERPROFILE\.gradle\caches\modules-2\files-2.1\org.hsqldb\hsqldb" -Recurse -Filter 'hsqldb-*.jar' -ErrorAction SilentlyContinue |
	Where-Object { $_.Name -notmatch 'sources|javadoc' } |
	Sort-Object { [version]($_.Name -replace '^hsqldb-|\.jar$', '') } -Descending |
	Select-Object -First 1
if (-not $jar) {
	Write-Host "No se encontró HSQLDB en la caché de Gradle. Ejecutá primero .\gradlew.bat build" -ForegroundColor Red
	exit 1
}

# 3. Abrir el administrador ya conectado.
Write-Host "Abriendo la base $database/linkshortener con $($jar.Name)..."
Start-Process java -ArgumentList '-cp', "`"$($jar.FullName)`"", 'org.hsqldb.util.DatabaseManagerSwing',
	'--url', "jdbc:hsqldb:file:$database/linkshortener", '--user', 'sa'
