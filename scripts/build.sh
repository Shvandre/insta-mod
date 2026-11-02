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
if [ -z "${KEEP_WORK:-}" ]; then
    trap 'rm -rf "$WORK"' EXIT
fi
echo "== build dir: $WORK"

# 1. Базовый APK: локальный файл → Aptoide → релиз base-<версия> в нашем репо
#    (Aptoide часто отстаёт; тогда базу берм из кэша: релиз с тегом base-<версия>
#    и активом instagram-<версия-с-дефисами>.apk, туда его кладут вручную)
if [ $# -ge 1 ]; then
    BASE_APK=$1
else
    BASE_APK="$WORK/instagram-base.apk"
    VER=$(curl -s "https://ws75.aptoide.com/api/7/app/get?package_name=com.instagram.android" \
        | python3 -c "import json,sys; d=json.load(sys.stdin); m=d['nodes']['meta']['data']; print(m.get('vername') or m['file']['vername'])")
    if [ "$VER" = "$IG_VERSION" ]; then
        echo "== скачиваю Instagram $IG_VERSION (aptoide, arm64)"
        URL=$(curl -s "https://ws75.aptoide.com/api/7/app/get?package_name=com.instagram.android" \
            | python3 -c "import json,sys; d=json.load(sys.stdin); m=d['nodes']['meta']['data']; print(m['file']['path'])")
        curl -sL "$URL" -o "$BASE_APK"
    else
        REPO=${INSTAMOD_REPO:-Shvandre/insta-mod}
        BASE_URL="https://github.com/${REPO}/releases/download/base-${IG_VERSION}/instagram-$(echo "$IG_VERSION" | tr . -).apk"
        echo "== Aptoide отдаёт $VER — беру базу из кэша: $BASE_URL"
        if ! curl -fsSL "$BASE_URL" -o "$BASE_APK"; then
            echo "!! База $IG_VERSION недоступна ни в Aptoide ($VER), ни в релизе base-$IG_VERSION." >&2
            echo "!! Скачай APK (Uptodown: https://instagram.en.uptodown.com/android/versions)," >&2
            echo "!! положи в релиз base-$IG_VERSION как instagram-$(echo "$IG_VERSION" | tr . -).apk" >&2
            echo "!! или собери локально: scripts/build.sh instagram.apk" >&2
            exit 1
        fi
    fi
fi

# 2. Декомпиляция
APKTOOL=${APKTOOL:-apktool}
echo "== apktool d"
$APKTOOL d -f "$BASE_APK" -o "$WORK/src" > /dev/null

# 3. Патчи (порядок важен)
apply_patch() {
    echo "== git apply $1"
    (cd "$WORK" && git apply --exclude='README.md' --exclude='modsrc/*' "$ROOT/$1")
}
apply_patch patches/01-ads.patch
scripts/apply-by-class.sh "$WORK" "X/05nH" patches/by-class/05nH-ads-gate.patch
apply_patch patches/02-quality-0F6S.patch
scripts/apply-by-class.sh "$WORK" "X/0ZkC" patches/by-class/0ZkC-quality.patch
scripts/apply-by-class.sh "$WORK" "X/0Xqx" patches/by-class/0Xqx-bitrate.patch
apply_patch patches/03-rename.patch
# манифест-патч — ДО pkg-rename: контекст ханка захватывает package= в <manifest>
apply_patch patches/06a-modsettings-manifest.patch
# pkg — скриптом: имена X/Foo.N.smali от apktool нестабильны между ФС
echo "== pkg-rename"
scripts/pkg-rename.sh "$WORK/src"
apply_patch patches/05a-wordmark-force-switcher.patch
apply_patch patches/05b-wordmark-force-merged.patch
apply_patch patches/05c-wordmark-force-gate.patch
apply_patch patches/06b-modsettings-reelmodel.patch
apply_patch patches/06c-modsettings-seenstate.patch
apply_patch patches/07a-wordmark-longclick-switcher.patch
apply_patch patches/07b-wordmark-longclick-merged.patch
scripts/apply-by-class.sh "$WORK" "X/0CeX" patches/by-class/0CeX-seen-flush.patch
# детекторы скриншотов (обе реализации, ищутся по методу Abh) — тоже скриптом
scripts/hook-screenshot.sh "$WORK/src"

# 4. Вордмарк (бинарные ассеты, в патчах не живут); 455+ несёт 3 плотности
for d in xhdpi xxhdpi mdpi; do
    cp "assets/wordmark/drawable-${d}.png" \
       "$WORK/src/res/drawable-${d}/instagram_wordmark_ig_wordmark_r3_finalize_v1.png"
    cp "assets/wordmark/drawable-night-${d}.png" \
       "$WORK/src/res/drawable-night-${d}/instagram_wordmark_ig_wordmark_r3_finalize_v1.png"
    rm -f "$WORK/src/res/drawable-${d}/instagram_wordmark_ig_wordmark_r3_finalize_v1.webp" \
          "$WORK/src/res/drawable-night-${d}/instagram_wordmark_ig_wordmark_r3_finalize_v1.webp"
done

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
