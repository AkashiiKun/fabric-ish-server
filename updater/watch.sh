#!/bin/sh
# - Syncs whitelist.json from WHITELIST_URL (applied live with `whitelist reload`)
# - Syncs ops.json from OPS_URL (applied live with op/deop)
# - Watches PACK_URL (upstream pack.toml); on change: warn, snapshot data/, restart mc
# - Does nothing about pack changes while /backups/PINNED exists (set by revert.sh)
set -u
apk add --no-cache curl jq >/dev/null

rcon() { docker exec "$MC_CONTAINER" rcon-cli "$@"; }

# Hash of pack.toml plus deploy-version.txt (the commit that was deployed). pack.toml alone
# is not enough: with no-internal-hashes its index hash only changes when mods are added/removed,
# not when a mod is updated or our own mod jar changes. Returns non-zero if pack.toml can't be fetched.
pack_hash() {
  t=$(mktemp)
  curl -fsSL --max-time 30 "$PACK_URL" -o "$t" || { rm -f "$t"; return 1; }
  curl -fsSL --max-time 30 "${PACK_URL%/*}/deploy-version.txt" >> "$t" 2>/dev/null || true
  sha256sum "$t" | cut -d' ' -f1
  rm -f "$t"
}

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

# ops.json is only read at server start, so for a running server we apply the
# difference with `op` / `deop`. We remember the hash of what we applied, because
# the server rewrites ops.json itself afterwards.
sync_ops() {
  [ -n "$OPS_URL" ] || return 0
  tmp=$(mktemp)
  if ! curl -fsSL --max-time 30 "$OPS_URL" -o "$tmp" || ! jq -e 'type == "array"' "$tmp" >/dev/null 2>&1; then
    echo "ops: fetch/validation failed, keeping current file"
    rm -f "$tmp"; return 0
  fi
  new=$(sha256sum "$tmp" | cut -d' ' -f1)
  if [ "$new" != "$(cat /data/.ops.sha 2>/dev/null)" ]; then
    echo "ops: changed, applying"
    old_names=$(jq -r '.[].name' /data/ops.json 2>/dev/null)
    new_names=$(jq -r '.[].name' "$tmp")
    cat "$tmp" > /data/ops.json
    chmod 666 /data/ops.json
    echo "$new" > /data/.ops.sha
    for n in $new_names; do echo "$old_names" | grep -qx "$n" || rcon "op $n" >/dev/null 2>&1 || true; done
    for n in $old_names; do echo "$new_names" | grep -qx "$n" || rcon "deop $n" >/dev/null 2>&1 || true; done
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

# Baseline: don't block on this, so the whitelist sync below keeps running even
# while the pack URL is unreachable.
last=""

while true; do
  sync_whitelist
  sync_ops

  if [ -z "$last" ]; then
    last=$(pack_hash) || last=""
    [ -n "$last" ] && echo "baseline pack hash: $last" || echo "pack fetch failed (check PACK_URL), will retry"
  elif [ -e /backups/PINNED ]; then
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
