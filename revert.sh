#!/bin/sh
# Snapshot / revert helper. Run on the server host, in the project directory.
#   ./revert.sh list                 show snapshots
#   ./revert.sh snapshot             take a snapshot now (stops the server briefly)
#   ./revert.sh restore [file]       restore a snapshot (default: newest) and PIN: mods stop
#                                    updating from upstream until you run `unpin`
#   ./revert.sh unpin                resume following the upstream pack (updates mods again)
set -eu
cd "$(dirname "$0")"
mkdir -p backups data

pin() {
  touch backups/PINNED
  grep -q '^PACKWIZ_URL_OVERRIDE=' .env || printf '\nPACKWIZ_URL_OVERRIDE=\n' >> .env
}

case "${1:-}" in
  list)
    ls -1t backups/backup-*.tar.gz 2>/dev/null || echo "no snapshots"
    [ -e backups/PINNED ] && echo "(pinned: not following upstream)" || true
    ;;
  snapshot)
    docker compose stop mc
    name="backup-$(date +%Y%m%d-%H%M%S)-manual.tar.gz"
    docker run --rm -v "$PWD/data:/data:ro" -v "$PWD/backups:/backups" alpine \
      tar -C /data --exclude=./logs --exclude=./crash-reports --exclude=./cache --exclude=./bluemap --exclude=./libraries --exclude=./.fabric -czf "/backups/$name" .
    echo "wrote backups/$name"
    docker compose start mc
    ;;
  restore)
    file="${2:-$(ls -1t backups/backup-*.tar.gz 2>/dev/null | head -1)}"
    [ -n "$file" ] || { echo "no snapshot found"; exit 1; }
    file="backups/$(basename "$file")"
    [ -f "$file" ] || { echo "$file not found"; exit 1; }
    printf 'Replace the CURRENT contents of data/ (world, mods, config) with %s? [y/N] ' "$file"
    read -r ans; [ "$ans" = "y" ] || exit 1
    pin
    docker compose stop mc updater
    docker run --rm -v "$PWD/data:/data" -v "$PWD/backups:/backups:ro" alpine sh -c \
      'find /data -mindepth 1 -maxdepth 1 ! -name bluemap ! -name libraries ! -name .fabric -exec rm -rf {} + && tar -C /data -xzf "/backups/'"$(basename "$file")"'"'
    docker compose up -d --force-recreate
    echo "restored $file. Pinned: run './revert.sh unpin' to follow upstream again."
    ;;
  unpin)
    rm -f backups/PINNED
    sed -i '/^PACKWIZ_URL_OVERRIDE=/d' .env
    docker compose up -d --force-recreate
    echo "unpinned: server now syncs with the upstream pack again."
    ;;
  *)
    sed -n '2,8p' "$0"; exit 1 ;;
esac
