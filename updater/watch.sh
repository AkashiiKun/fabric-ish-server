#!/bin/sh
# - Syncs whitelist.json from WHITELIST_URL (applied live with `whitelist reload`)
# - Watches PACK_URL (upstream pack.toml); on change: warn, snapshot data/, restart mc
# - Does nothing about pack changes while /backups/PINNED exists (set by revert.sh)
set -u
apk add --no-cache curl jq >/dev/null

rcon() { docker exec "$MC_CONTAINER" rcon-cli "$@"; }

pack_hash() { curl -fsSL --max-time 30 "$PACK_URL" | sha256sum | cut -d' ' -f1; }

sync_whitelist() {
  [ -n "$WHITELIST_URL" ] || return 0
  tmp=$(mktemp)
  if ! curl -fsSL --max-time 30 "$WHITELIST_URL" -o "$tmp" || ! jq -e 'type == "array"' "$tmp" >/dev/null 2>&1; then
    echo "whitelist: fetch/validation failed, keeping current file"
    rm -f "$tmp"; return 0
  fi
  if ! cmp -s "$tmp" /data/whitelist.json; then
    echo "whitelist: changed, applying"
    cat "$tmp" > /data/whitelist.json
    chmod 666 /data/whitelist.json
    rcon whitelist reload >/dev/null 2>&1 || echo "whitelist: server not reachable, it will load the file on next start"
  fi
  rm -f "$tmp"
}

snapshot() {
  name="backup-$(date +%Y%m%d-%H%M%S).tar.gz"
  echo "snapshot: stopping $MC_CONTAINER and writing $name"
  rcon "save-all flush" >/dev/null 2>&1 || true
  docker stop "$MC_CONTAINER" >/dev/null
  tar -C /data --exclude=./logs --exclude=./crash-reports --exclude=./cache \
      -czf "/backups/$name" . || echo "snapshot: tar reported errors"
  ls -1t /backups/backup-*.tar.gz 2>/dev/null | tail -n +"$((BACKUP_KEEP + 1))" | xargs -r rm -f
}

last=""
while [ -z "$last" ]; do
  last=$(pack_hash) || last=""
  [ -z "$last" ] && { echo "initial pack fetch failed, retrying"; sleep 30; }
done
echo "baseline pack hash: $last"

while true; do
  sync_whitelist

  if [ -e /backups/PINNED ]; then
    echo "pinned (revert active): ignoring pack changes"
  else
    cur=$(pack_hash) || { echo "pack fetch failed, skipping"; cur=""; }
    if [ -n "$cur" ] && [ "$cur" != "$last" ]; then
      echo "pack changed ($last -> $cur)"
      rcon "say Mod update available - server restarting in ${WARN_SECONDS}s" >/dev/null 2>&1 || true
      sleep "$WARN_SECONDS"
      snapshot
      docker start "$MC_CONTAINER" >/dev/null && last="$cur"
    fi
  fi

  sleep "$POLL_SECONDS"
done
