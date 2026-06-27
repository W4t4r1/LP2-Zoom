@echo off
title Cliente LP2-Zoom (Portable)
echo ===================================================
echo   LP2-ZOOM: COMPILANDO E INICIANDO CLIENTE...
echo ===================================================
echo.
echo [*] Buscando archivos fuente Java...
dir /s /b src\main\java\*.java > sources.txt
if %errorlevel% neq 0 (
    echo [ERROR] No se encontraron archivos fuente Java.
    pause
    exit /b 1
)

echo [*] Compilando codigo fuente en carpeta 'out'...
javac -d out -cp "lib/*" @sources.txt
if %errorlevel% neq 0 (
    echo.
    echo [ERROR] Hubo un error de compilacion.
    del sources.txt
    pause
    exit /b %errorlevel%
)
del sources.txt
echo [OK] Compilacion exitosa.
echo.
echo [*] Iniciando cliente grafico (UI.LoginFrame)...
echo.
java -cp "out;lib/*" UI.LoginFrame
pause
