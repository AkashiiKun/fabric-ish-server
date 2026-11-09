# Holiday-ish Fabric server + modpack

The repo is the single source of truth for mods, using [packwiz](https://packwiz.infra.link/).

```
pack/                    packwiz pack (pack.toml, index.toml, mods/*.pw.toml, config overrides)
.github/workflows/       build-pack.yml  -> refreshes index, builds .mrpack, publishes "latest" release
                         update-mods.yml -> weekly PR with updated mods
docker-compose.yml       Minecraft server (itzg/minecraft-server) + updater sidecar
updater/watch.sh         restarts the server when pack.toml changes in GitHub
```

Flow: you push a mod change to `main` -> Actions rebuilds `modpack.mrpack` (GitHub release `latest`) ->
the updater on the server notices the changed `pack.toml` (within 5 min), warns players, restarts the
server -> itzg runs packwiz-installer on start and syncs the mods. Mods marked client-only are skipped on the server.

## 1. Managing mods (on your PC)

Install the packwiz CLI (`go install github.com/packwiz/packwiz@latest`, or grab a build from
https://nightly.link/packwiz/packwiz/workflows/go/main), then:

```sh
cd pack
packwiz modrinth add fabric-api        # search by name or paste a Modrinth URL
packwiz modrinth add sodium
packwiz curseforge add <name>          # CurseForge mods (see caveat below)
packwiz remove sodium
packwiz update --all
packwiz refresh                        # CI does this too, but run it to keep things tidy
```

Commit and push `pack/`. Put shared config (e.g. `config/foo.toml`) inside `pack/` and it is distributed
to players; server-side config lives in `data/` on the server.

Edit `pack/pack.toml` to change `name`, `author`, Minecraft/Fabric versions. Bump `version` for releases.
Keep `MC_VERSION` in `.env` in sync with it. Find Fabric loader versions with `packwiz update`/`packwiz init`.

Caveat: `.mrpack` export only works for mods that Modrinth allows to be distributed that way; CurseForge-only
mods whose authors disable third-party downloads will make the export fail. Prefer Modrinth.

## 2. GitHub setup (after you push)

1. Repo must be **public** (the server and `raw.githubusercontent.com` fetch without credentials).
2. Settings -> Actions -> General -> Workflow permissions: **Read and write**, and tick
   **Allow GitHub Actions to create and approve pull requests** (for the weekly update PR).
3. Run "Build modpack" once (Actions tab -> Run workflow) to create the `latest` release.

## 3. Server setup (Linux host with Docker + Docker Compose v2)

```sh
git clone https://github.com/OWNER/REPO.git mc-server && cd mc-server
cp .env.example .env
nano .env        # set PACK_URL (your repo's raw pack.toml URL), RCON_PASSWORD, MC_OPS, memory...
docker compose up -d
docker compose logs -f mc
```

- Open/forward TCP **25565** on your firewall/router.
- World + mods live in `./data` (back it up). First start takes a few minutes.
- `docker compose exec mc rcon-cli` opens the server console.
- `docker compose logs -f updater` shows update checks.
- Force an update now: `docker compose restart mc`.
- Whitelist: set `MC_WHITELIST=true`, then `docker compose exec mc rcon-cli whitelist add NAME`.
- The server host only needs the `.env`, `docker-compose.yml` and `updater/` - the clone is the simplest way.
  `git pull` there only when you change those files, not for mod updates.
- Security note: the updater mounts the Docker socket (needed to restart `mc`). Only run on a host you control.

## 4. Players (Prism Launcher and others)

Import `https://github.com/OWNER/REPO/releases/download/latest/modpack.mrpack`
(Prism: Add Instance -> Import -> paste URL; Modrinth App works too).
This is a snapshot - to update, re-import the new file.

Optional auto-updating instance in Prism: import once, then Instance -> Settings -> Custom commands ->
Pre-launch command:

```
"$INST_JAVA" -jar packwiz-installer-bootstrap.jar https://raw.githubusercontent.com/OWNER/REPO/main/pack/pack.toml
```

after downloading `packwiz-installer-bootstrap.jar` from
https://github.com/packwiz/packwiz-installer-bootstrap/releases into the instance's `.minecraft` folder
(tick "Override global settings" for custom commands). Mods then sync on every launch.
