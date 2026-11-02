#!/usr/bin/env bash
# Хук тумблера no_screenshot_detect в обе реализации детектора скриншотов.
# В 455+ это классы с методом Abh(JLjava/lang/String;Ljava/util/List;)V
# (в 435 метод назывался AN7). Имена файлов нестабильны (apktool разводит
# коллизии регистра суффиксами .N.smali), поэтому файлы ищутся по методу.
set -euo pipefail
SRC=${1:?usage: hook-screenshot.sh <decompiled-src-dir>}

export BLOCK_FILE=$(mktemp)
cat > "$BLOCK_FILE" <<'EOF'

    sget-object v0, LX/04g9;->A00:Landroid/content/Context;

    if-nez v0, :instamod_ss_pref_off

    const-string v1, "instamod"

    const/4 v2, 0x0

    invoke-virtual {v0, v1, v2}, Landroid/content/Context;->getSharedPreferences(Ljava/lang/String;I)Landroid/content/SharedPreferences;

    move-result-object v0

    const-string v1, "no_screenshot_detect"

    invoke-interface {v0, v1, v2}, Landroid/content/SharedPreferences;->getBoolean(Ljava/lang/String;Z)Z

    move-result v0

    if-eqz v0, :instamod_ss_pref_off

    return-void

    :instamod_ss_pref_off
EOF

PATCHED=0
for F in $(grep -rl '^\.method public final Abh(JLjava/lang/String;Ljava/util/List;)V' "$SRC"/smali*/X/); do
    perl -0777 -pi -e '
      BEGIN { local $/; open my $bh, "<", $ENV{BLOCK_FILE} or die $!; $b = <$bh>; }
      s/(\.method public final Abh\(JLjava\/lang\/String;Ljava\/util\/List;\)V\n    \.locals \d+\n)\n/$1$b/
    ' "$F"
    grep -q 'instamod_ss_pref_off' "$F" || { echo "hook-screenshot: вставка не удалась в $F"; exit 1; }
    echo "hook-screenshot: patched $F"
    PATCHED=$((PATCHED + 1))
done
rm -f "$BLOCK_FILE"
[ "$PATCHED" -ge 1 ] || { echo "hook-screenshot: классы-детекторы не найдены"; exit 1; }
