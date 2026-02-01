@echo off
echo ======================================
echo   IPAM Manager - Demarrage
echo ======================================
echo.

REM Verifier si Java est installe
java -version >nul 2>&1
if errorlevel 1 (
    echo [X] Java n'est pas installe ou n'est pas dans le PATH
    echo     Veuillez installer Java 17 ou superieur
    pause
    exit /b 1
)

echo [OK] Java detecte
java -version
echo.

REM Verifier si Maven est installe
mvn -version >nul 2>&1
if errorlevel 1 (
    echo [X] Maven n'est pas installe
    echo     Veuillez installer Maven 3.6 ou superieur
    pause
    exit /b 1
)

echo [OK] Maven detecte
echo.

REM Compiler si necessaire
if not exist "target\classes\com\ipam\MainApp.class" (
    echo [*] Compilation du projet...
    call mvn clean compile
    if errorlevel 1 (
        echo [X] Erreur lors de la compilation
        pause
        exit /b 1
    )
    echo [OK] Compilation reussie
    echo.
)

REM Lancer l'application
echo [*] Lancement de IPAM Manager...
echo.
call mvn javafx:run

pause
