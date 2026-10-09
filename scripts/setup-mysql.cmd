@echo off
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0setup-mysql.ps1" %*
pause
