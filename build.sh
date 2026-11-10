set -e

# Build internal mod
echo "Building internal mod..."
# The jar name must change on every deploy: the pack index has no file hashes, so packwiz
# only replaces a mod when its file name changes.
BASE_VERSION=$(sed -n 's/^version=//p' mod/gradle.properties)
BUILD_ID=$(echo "${GITHUB_SHA:-local}" | cut -c1-7)
(cd mod && chmod +x ./gradlew && ./gradlew clean  build "-Pversion=${BASE_VERSION}-${BUILD_ID}")

# Build pack
echo "Building pack..."

rm -rf build
mkdir -p build
cp -r mods index.toml pack.toml build
# optional client+server config shipped with the pack
[ -d config ] && cp -r config build
# placeholder files only keep empty dirs in git; Pages doesn't serve dotfiles
find build -name .gitkeep -delete
cp $(ls mod/build/libs/*.jar | grep -v -- "-sources.jar") build/mods

cd build
mkdir -p ../out
packwiz modrinth export -o ../out/holiday-server-pack.mrpack
packwiz refresh --build
# marks every deployment (written after refresh so it is not part of the pack index)
echo "${GITHUB_SHA:-$(date +%s)}" > deploy-version.txt
