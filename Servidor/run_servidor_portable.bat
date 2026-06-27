@echo off
title Servidor LP2-Zoom (Portable)
echo ===================================================
echo   LP2-ZOOM: COMPILANDO E INICIANDO SERVIDOR...
echo ===================================================
echo.
echo [*] Compilando codigo fuente en carpeta 'out'...
javac -d out -cp "lib/*" src/main/java/network/*.java src/main/java/database/*.java src/main/java/model/*.java
if %errorlevel% neq 0 (
    echo.
    echo [ERROR] Hubo un error de compilacion.
    pause
    exit /b %errorlevel%
)
echo [OK] Compilacion exitosa.
echo.
echo [*] Iniciando servidor de sockets en el puerto 5000...
echo.
java -cp "out;src/main/resources;lib/*" network.MainServidor
pause
