# Corre todos los tests desde cero y, si pasan, abre en el navegador
# el reporte de tests y el de cobertura (JaCoCo).
# Uso, desde backend/:  .\tests.ps1

$backend = $PSScriptRoot

# Usar un JDK 25 configurado o la copia portable del proyecto, sin cambiar el Java del sistema.
$candidatosJdk = @($env:JAVA_HOME) + @(Get-ChildItem "$backend\.gradle\toolchains" -Directory -Filter 'jdk-25*' -ErrorAction SilentlyContinue |
	Sort-Object Name -Descending | Select-Object -ExpandProperty FullName)
$jdk25 = $null
foreach ($candidatoJdk in $candidatosJdk) {
	if (-not $candidatoJdk) { continue }
	$candidatoJdk = $candidatoJdk.Trim('"')
	if ((Test-Path "$candidatoJdk\bin\java.exe" -PathType Leaf) -and
		(Test-Path "$candidatoJdk\bin\javac.exe" -PathType Leaf) -and
		(Test-Path "$candidatoJdk\release" -PathType Leaf) -and
		(Select-String -Path "$candidatoJdk\release" -Pattern '^JAVA_VERSION="25([.+"]|$)' -Quiet)) {
		$jdk25 = $candidatoJdk
		break
	}
}
if (-not $jdk25) {
	Write-Host "No se encontró JDK 25. Configurá JAVA_HOME con un JDK 25 o colocá la copia portable en backend/.gradle/toolchains/jdk-25*/." -ForegroundColor Red
	exit 1
}

$javaHomeAnterior = $env:JAVA_HOME
try {
	$env:JAVA_HOME = $jdk25
	Write-Host "Ejecutando los tests con JDK 25: $jdk25"
	& "$backend\gradlew.bat" -p $backend cleanTest test
	$resultadoTests = $LASTEXITCODE
} finally {
	$env:JAVA_HOME = $javaHomeAnterior
}

if ($resultadoTests -eq 0) {
	Start-Process "$backend\build\reports\tests\test\index.html"
	Start-Process "$backend\build\reports\jacoco\test\html\index.html"
} else {
	Write-Host "Gradle no pudo completar las pruebas." -ForegroundColor Red
	if (Test-Path "$backend\build\reports\tests\test\index.html") {
		Start-Process "$backend\build\reports\tests\test\index.html"
	}
	exit $resultadoTests
}
