set -e

# Build pack
echo "Building pack..."

rm -rf build
mkdir -p build
cp -r config mods index.toml pack.toml build

cd build
mkdir -p ../out
packwiz modrinth export -o ../out/holiday-server-pack.mrpack
packwiz refresh --build
