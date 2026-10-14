#!/usr/bin/env bash
# Сборка Insta Mod из оригинального Instagram APK.
# Использование:
#   scripts/build.sh                 # скачает базовый APK (Aptoide, arm64) по version.txt
#   scripts/build.sh path/to/instagram.apk
# Результат: dist/insta-mod-<версия>-mod.<rev>-signed.apk
set -euo pipefail
cd "$(dirname "$0")/.."
ROOT=$PWD

IG_VERSION=$(cat version.txt)
MODREV=$(cat modrev.txt)
TAG="${IG_VERSION}-mod.${MODREV}"

ANDROID_HOME=${ANDROID_HOME:-"$HOME/Library/Android/sdk"}
BUILD_TOOLS="$ANDROID_HOME/build-tools/36.1.0"
PLATFORM=$(ls -d "$ANDROID_HOME"/platforms/android-* | sort -V | tail -1)
KEYSTORE=${INSTAMOD_KEYSTORE:-mod.keystore}
KS_PASS=${INSTAMOD_KS_PASS:-androidmod}

WORK=$(mktemp -d)
trap 'rm -rf "$WORK"' EXIT
echo "== build dir: $WORK"

# 1. Базовый APK
if [ $# -ge 1 ]; then
    BASE_APK=$1
else
    BASE_APK="$WORK/instagram-base.apk"
    echo "== скачиваю Instagram $IG_VERSION (aptoide, arm64)"
    URL=$(curl -s "https://ws75.aptoide.com/api/7/app/get?package_name=com.instagram.android" \
        | python3 -c "import json,sys; d=json.load(sys.stdin); print(d['nodes']['meta']['data']['file']['path'])")
    curl -sL "$URL" -o "$BASE_APK"
fi

# 2. Декомпиляция
APKTOOL=${APKTOOL:-apktool}
echo "== apktool d"
$APKTOOL d -f "$BASE_APK" -o "$WORK/src" > /dev/null

# 3. Патчи
for p in patches/*.patch; do
    echo "== git apply $p"
    git apply --directory="$WORK" --exclude='README.md' --exclude='modsrc/*' "$p"
done

# 4. Вордмарк (бинарные ассеты, в патчах не живут)
cp assets/wordmark/drawable-xhdpi.png \
   "$WORK/src/res/drawable-xhdpi/instagram_wordmark_ig_wordmark_r3_finalize_v1.png"
cp assets/wordmark/drawable-night-xhdpi.png \
   "$WORK/src/res/drawable-night-xhdpi/instagram_wordmark_ig_wordmark_r3_finalize_v1.png"
rm -f "$WORK/src/res/drawable-xhdpi/instagram_wordmark_ig_wordmark_r3_finalize_v1.webp" \
      "$WORK/src/res/drawable-night-xhdpi/instagram_wordmark_ig_wordmark_r3_finalize_v1.webp"

# 5. Версия мода (читает автообновлялка)
mkdir -p "$WORK/src/assets/instamod"
echo -n "$TAG" > "$WORK/src/assets/instamod/version"

# 6. Dex с меню настроек
echo "== javac + d8 (modsrc)"
mkdir -p "$WORK/modcls" "$WORK/moddex"
javac --release 11 -cp "$PLATFORM/android.jar" -d "$WORK/modcls" modsrc/*.java
"$BUILD_TOOLS/d8" --min-api 28 --lib "$PLATFORM/android.jar" \
    $(find "$WORK/modcls" -name '*.class') --output "$WORK/moddex"

# 7. Сборка APK
echo "== apktool b"
$APKTOOL b "$WORK/src" -o "$WORK/mod.apk" > /dev/null

# 8. Инжект dex со следующим свободным номером
MAXN=$(unzip -Z1 "$WORK/mod.apk" | grep -oE '^classes[0-9]*\.dex$' \
    | grep -oE '[0-9]+' | sort -n | tail -1)
NEXTN=$(( ${MAXN:-1} + 1 ))
DEXNAME="classes${NEXTN}.dex"
echo "== инжект $DEXNAME"
cp "$WORK/moddex/classes.dex" "$WORK/$DEXNAME"
(cd "$WORK" && zip -j mod.apk "$DEXNAME" > /dev/null)

# 9. Выравнивание и подпись
if [ ! -f "$KEYSTORE" ]; then
    echo "== генерирую keystore $KEYSTORE (сохрани его — обновления требуют той же подписи!)"
    keytool -genkeypair -keystore "$KEYSTORE" -alias instamod \
        -keyalg RSA -keysize 2048 -validity 10000 \
        -storepass "$KS_PASS" -keypass "$KS_PASS" \
        -dname "CN=Insta Mod, OU=Mod, O=Mod, L=NA, S=NA, C=NA"
fi
"$BUILD_TOOLS/zipalign" -p -f 4 "$WORK/mod.apk" "$WORK/aligned.apk"
"$BUILD_TOOLS/apksigner" sign --ks "$KEYSTORE" --ks-pass "pass:$KS_PASS" \
    --out "$WORK/signed.apk" "$WORK/aligned.apk"

mkdir -p dist
OUT="dist/insta-mod-${TAG}-signed.apk"
cp "$WORK/signed.apk" "$OUT"
echo "== готово: $OUT"
