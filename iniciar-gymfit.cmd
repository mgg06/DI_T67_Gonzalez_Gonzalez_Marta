@echo off
setlocal
cd /d "%~dp0"
set "GYMFIT_FX26=%LOCALAPPDATA%\SceneBuilder\runtime\bin\javaw.exe"
if exist "%GYMFIT_FX26%" (
  start "GymFit Club" "%GYMFIT_FX26%" --add-modules=javafx.controls,javafx.fxml,javafx.swing --enable-native-access=javafx.graphics -jar "GymFit-Studio.jar"
  exit /b
)
set "GYMFIT_JAVA=C:\Program Files\Java\jre1.8.0_503\bin\javaw.exe"
if exist "%GYMFIT_JAVA%" (
  start "GymFit Club" "%GYMFIT_JAVA%" -jar "GymFit-Studio.jar"
) else (
  echo Este lanzador necesita el Java 8 con JavaFX instalado en el equipo de Marta.
  echo En otro equipo, abre pom.xml con IntelliJ y ejecuta Maven javafx:run.
  pause
)
endlocal
