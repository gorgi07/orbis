#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
PID_FILE=run/orbis.pid
if [[ ! -f "$PID_FILE" ]]; then echo "PID file missing"; exit 1; fi
pid=$(cat "$PID_FILE")
kill "$pid" 2>/dev/null || true
rm -f "$PID_FILE"
echo "Sent TERM to $pid"
