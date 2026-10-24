@echo off
set "EDGE=%ProgramFiles(x86)%\Microsoft\Edge\Application\msedge.exe"
if not exist "%EDGE%" set "EDGE=%ProgramFiles%\Microsoft\Edge\Application\msedge.exe"
if not exist "%EDGE%" (
  echo Microsoft Edge not found.
  pause
  exit /b 1
)
start "QuantPF V25 Prototype" "%EDGE%" --new-window "file:///%~dp0index.html"
