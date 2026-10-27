# ========================================================================
# Script d'exécution automatisée de test de charge JMeter pour EduStream
# ========================================================================

param (
    [string]$TargetHost = "edustream-0814.onrender.com",
    [int]$Port = 443,
    [string]$Protocol = "https",
    [int]$Users = 50,
    [int]$RampUp = 10,
    [int]$Loops = 3,
    [string]$JMeterPath = ""
)

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "   EDUSTREAM - Lancement du Test de Charge Apache JMeter   " -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "Cible      : $Protocol://$TargetHost`:$Port" -ForegroundColor Yellow
Write-Host "Charge     : $Users utilisateurs virtuels (Ramp-up: ${RampUp}s, Boucles: $Loops)" -ForegroundColor Yellow
Write-Host "----------------------------------------------------------"

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$JmxFile = Join-Path $ScriptDir "edustream_load_test.jmx"
$ResultsFile = Join-Path $ScriptDir "results.jtl"
$ReportDir = Join-Path $ScriptDir "report"

# Détection de l'exécutable JMeter
$JMeterExe = $null

if ($JMeterPath -ne "" -and (Test-Path $JMeterPath)) {
    $JMeterExe = $JMeterPath
} elseif (Get-Command "jmeter" -ErrorAction SilentlyContinue) {
    $JMeterExe = "jmeter"
} else {
    # Recherche dans les dossiers fréquents
    $PossiblePaths = @(
        "C:\apache-jmeter*\bin\jmeter.bat",
        "$env:USERPROFILE\Downloads\apache-jmeter*\bin\jmeter.bat",
        "$env:USERPROFILE\Desktop\apache-jmeter*\bin\jmeter.bat",
        "C:\Tools\apache-jmeter*\bin\jmeter.bat"
    )
    foreach ($path in $PossiblePaths) {
        $found = Get-Item $path -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($found) {
            $JMeterExe = $found.FullName
            break
        }
    }
}

if (-not $JMeterExe) {
    Write-Host "[!] Apache JMeter n'a pas été détecté automatiquement dans le PATH." -ForegroundColor Yellow
    Write-Host "    Pour installer JMeter rapidement :" -ForegroundColor White
    Write-Host "    1. Téléchargez le zip officiel : https://jmeter.apache.org/download_jmeter.cgi" -ForegroundColor White
    Write-Host "    2. Dézippez-le (ex: dans C:\apache-jmeter-5.6.3)" -ForegroundColor White
    Write-Host "    3. Relancez : .\run-jmeter.ps1 -JMeterPath 'C:\apache-jmeter-5.6.3\bin\jmeter.bat'" -ForegroundColor Green
    Write-Host "    OU ouvrez simplement 'jmeter.bat' et faites File > Open > edustream_load_test.jmx !" -ForegroundColor Cyan
    exit 1
}

Write-Host "[+] Exécutable JMeter trouvé : $JMeterExe" -ForegroundColor Green

# Nettoyage des anciens résultats
if (Test-Path $ResultsFile) { Remove-Item $ResultsFile -Force }
if (Test-Path $ReportDir) { Remove-Item $ReportDir -Recurse -Force }

Write-Host "[*] Exécution du test en mode CLI non-graphique (-n)..." -ForegroundColor Cyan

& $JMeterExe -n `
    -t $JmxFile `
    -l $ResultsFile `
    -e -o $ReportDir `
    -Jhost=$TargetHost `
    -Jport=$Port `
    -Jprotocol=$Protocol `
    -Jusers=$Users `
    -Jrampup=$RampUp `
    -Jloops=$Loops

Write-Host "----------------------------------------------------------"
if (Test-Path (Join-Path $ReportDir "index.html")) {
    Write-Host "[SUCCESS] Rapport HTML interactif généré avec succès !" -ForegroundColor Green
    Write-Host "Emplacement : $(Join-Path $ReportDir 'index.html')" -ForegroundColor White
    Start-Process (Join-Path $ReportDir "index.html")
} else {
    Write-Host "[INFO] Exécution terminée. Vérifiez le fichier $ResultsFile pour les métriques détaillées." -ForegroundColor Yellow
}
