#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
APP_DIR="$PWD"
ENV_FILE="${ENV_FILE:-$APP_DIR/config/server.env}"
if [[ ! -r "$ENV_FILE" ]]; then echo "Missing private config: $ENV_FILE" >&2; exit 1; fi
set -a
# Create config/server.env from config/server.env.example; chmod 600.
source "$ENV_FILE"
set +a
mkdir -p "$APP_DIR/logs" "$APP_DIR/run"
PID_FILE="$APP_DIR/run/orbis.pid"
if [[ -e "$PID_FILE" ]] && kill -0 "$(cat "$PID_FILE")" 2>/dev/null; then echo "Already running (PID $(cat "$PID_FILE"))"; exit 1; fi
if [[ ! -f "$APP_DIR/orbis.jar" ]]; then echo "Copy build/libs/orbis.jar into $APP_DIR" >&2; exit 1; fi
nohup java ${JAVA_OPTS:--Xms64m -Xmx512m} -jar "$APP_DIR/orbis.jar" --spring.profiles.active=server >> "$APP_DIR/logs/orbis.log" 2>&1 < /dev/null &
echo $! > "$PID_FILE"
echo "Started PID $(cat "$PID_FILE") (check logs/orbis.log)."
