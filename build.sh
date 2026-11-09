set -e

# Build internal mod
echo "Building internal mod..."
(cd mod && chmod +x ./gradlew && ./gradlew clean build)

# Build pack
echo "Building pack..."

rm -rf build
mkdir -p build
cp -r config mods index.toml pack.toml build
# placeholder files only keep empty dirs in git; Pages doesn't serve dotfiles
find build -name .gitkeep -delete
cp $(ls mod/build/libs/*.jar | grep -v -- "-sources.jar") build/mods

cd build
mkdir -p ../out
packwiz modrinth export -o ../out/holiday-server-pack.mrpack
packwiz refresh --build
