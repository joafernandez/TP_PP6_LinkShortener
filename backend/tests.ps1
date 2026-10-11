# Corre todos los tests desde cero y, si pasan, abre en el navegador
# el reporte de tests y el de cobertura (JaCoCo).
# Uso, desde backend/:  .\tests.ps1

$backend = $PSScriptRoot

& "$backend\gradlew.bat" -p $backend cleanTest test

if ($LASTEXITCODE -eq 0) {
	Start-Process "$backend\build\reports\tests\test\index.html"
	Start-Process "$backend\build\reports\jacoco\test\html\index.html"
} else {
	Write-Host "Hay tests que fallaron. Se abre el reporte para ver cuáles." -ForegroundColor Red
	Start-Process "$backend\build\reports\tests\test\index.html"
	exit $LASTEXITCODE
}
