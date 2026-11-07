@echo off
title AgentScope start-all

echo ==========================================
echo    AgentScope one-click start
echo ==========================================
echo.

echo [1/3] Redis container ...
docker start agentscope-redis >nul 2>&1
if errorlevel 1 (
    echo.
    echo [FAIL] Cannot start docker container agentscope-redis
    echo        Please start Docker Desktop first, then run again.
    pause
    exit /b 1
)
echo        OK - Redis :6379
echo.

netstat -ano | findstr "LISTENING" | findstr ":8000 " >nul 2>&1
if errorlevel 1 (
    echo [2/3] agent-service backend ...
    start "agent-service :8000" /D "%~dp0examples\agent_service" cmd /k %~dp0.venv\Scripts\python.exe main.py
    echo        OK - backend  http://localhost:8000
) else (
    echo [2/3] backend already running, skip
)
echo.

netstat -ano | findstr "LISTENING" | findstr ":5173 " >nul 2>&1
if errorlevel 1 (
    echo [3/3] web-ui frontend ...
    start "web-ui :5173" /D "%~dp0examples\web_ui" cmd /k pnpm dev
    echo        OK - frontend http://localhost:5173
) else (
    echo [3/3] frontend already running, skip
)
echo.

echo Opening browser when ready ...
ping -n 9 127.0.0.1 >nul
start http://localhost:5173

echo.
echo All started. Close each window to stop that service.
ping -n 4 127.0.0.1 >nul
