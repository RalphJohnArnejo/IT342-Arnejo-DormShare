@echo off
setlocal enabledelayedexpansion

REM Set database connection (Supabase Direct Connection)
set "SPRING_DATASOURCE_URL=jdbc:postgresql://db.geiegjgcelzmplldhgjh.supabase.co:5432/postgres?sslmode=require"
set "SPRING_DATASOURCE_USERNAME=postgres"
set "SPRING_DATASOURCE_PASSWORD=dormshare2026"

REM Load environment variables from .env file
for /f "usebackq delims=" %%A in (.env) do (
    set "line=%%A"
    if not "!line:~0,1!"=="#" if not "!line!"=="" (
        for /f "tokens=1,* delims==" %%B in ("!line!") do (
            set "%%B=%%C"
        )
    )
)

REM Run Maven with environment variables
mvn clean spring-boot:run

pause
