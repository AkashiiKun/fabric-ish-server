set -e

# Build pack
echo "Building pack..."

rm -rf build
mkdir -p build
cp -r config mods index.toml pack.toml build
# placeholder files only keep empty dirs in git; Pages doesn't serve dotfiles
find build -name .gitkeep -delete

cd build
mkdir -p ../out
packwiz modrinth export -o ../out/holiday-server-pack.mrpack
packwiz refresh --build
