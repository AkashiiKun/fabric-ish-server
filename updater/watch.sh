#!/bin/sh
# Polls the packwiz pack.toml; restarts the Minecraft container when it changes.
set -u
apk add --no-cache curl >/dev/null

fetch_hash() {
  curl -fsSL --max-time 30 "$PACK_URL" | sha256sum | cut -d' ' -f1
}

last=""
while [ -z "$last" ]; do
  last=$(fetch_hash) || last=""
  [ -z "$last" ] && { echo "initial fetch failed, retrying"; sleep 30; }
done
echo "baseline pack hash: $last"

while true; do
  sleep "$POLL_SECONDS"
  cur=$(fetch_hash) || { echo "fetch failed, skipping"; continue; }
  [ -z "$cur" ] && continue
  if [ "$cur" != "$last" ]; then
    echo "pack changed ($last -> $cur); restarting $MC_CONTAINER"
    docker exec "$MC_CONTAINER" rcon-cli "say Mod update available - server restarting in ${WARN_SECONDS}s" || true
    sleep "$WARN_SECONDS"
    docker exec "$MC_CONTAINER" rcon-cli "save-all flush" || true
    docker restart "$MC_CONTAINER" && last="$cur"
  fi
done
