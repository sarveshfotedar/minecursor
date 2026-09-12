#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/bridge"
if [[ ! -f .env ]]; then
  cp .env.example .env
  echo "Created bridge/.env — add your CURSOR_API_KEY and CURSOR_WORKSPACE, then run this again."
  exit 1
fi
if [[ ! -d node_modules ]]; then
  npm install
fi
exec npm start
