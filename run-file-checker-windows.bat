@echo off
REM Docker Compose script for Windows

docker compose --env-file .env.demo.bodc down
docker compose --env-file .env.demo.bodc up