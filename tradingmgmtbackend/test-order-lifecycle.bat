```bat
@echo off
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0test-order-lifecycle.ps1"

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo Script failed with exit code %ERRORLEVEL%.
    pause
    exit /b %ERRORLEVEL%
)

echo.
echo Script completed successfully.
pause
```
