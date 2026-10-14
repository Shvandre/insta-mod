# Insta Mod

Патч-набор для Instagram (Android, arm64), убирающий рекламу и добавляющий приватность-фичи.
Распространяется **только как патчи** — оригинальный код Meta здесь не публикуется,
APK каждый собирает сам из легально полученного оригинала.

## Фичи

- Без рекламы (лента, сториз, reels) — включено по умолчанию
- Название «Insta» вместо «Instagram» (иконка приложения + вордмарк в шапке)
- Без сжатия фото/видео при загрузке (JPEG ~100%, max dimension, видео без даунскейла)
- Пакет `com.insta.mod` — ставится рядом с оригиналом
- Отдельное меню «Insta Mod» с тумблерами:
  - не отмечать сториз просмотренными
  - отключить детект скриншотов (Direct/сториз)
  - блокировать рекламу
  - блокировать аналитику
- Автообновление из меню (GitHub Releases)

## Сборка

Нужны: JDK 17+, Android SDK (build-tools 36.1.0, platform android-36.1), apktool 3.0.3.

```bash
scripts/build.sh                      # сам скачает базовый APK (Aptoide, arm64)
scripts/build.sh instagram.apk        # или из локального файла
```

Результат: `dist/insta-mod-<версия>-mod.<rev>-signed.apk`.

При первой сборке генерируется `mod.keystore` — **сохрани его**: автообновления
ставятся поверх только при совпадающей подписи.

## Как устроено

- `patches/*.patch` — smali/манифест-патчи, накатываются на `apktool d`-дерево (`git apply`)
- `patches/by-class/*.patch` — патчи к классам, чьи имена файлов нестабильны
  (X/1vT, X/9Dn — коллизии регистра, apktool разводит их суффиксами `.N.smali`
  по-разному на macOS/Linux); `scripts/apply-by-class.sh` находит файл по
  `.class`-директиве и подставляет его в патч
- `scripts/pkg-rename.sh` — переименование пакета `com.instagram.android` → `com.insta.mod`
  скриптом (та же проблема нестабильных имён у 185 файлов);
  список authorities — `scripts/pkg-rename-auths.txt`
- `scripts/hook-qza.sh` — хук детектора скриншотов в класс `LX/Qza;` (ищется по `.class`)
- `assets/wordmark/` — PNG вордмарка «Insta» (бинарники в патчах не живут)
- `modsrc/` — исходник меню настроек + автообновлялки, компилируется в отдельный dex
- `version.txt` — базовая версия Instagram, `modrev.txt` — ревизия патчей
- `.github/workflows/build.yml` — CI: ежедневно проверяет новую версию Instagram,
  пересобирает и (при `vars.PUBLISH_APK = true`) публикует релиз

Порядок применения (как в jj-стеке): 01-ads → 1vT-ads → 02-quality → 03-rename →
pkg-rename.sh → 05-wordmark-force → 06-modsettings → 1vT-gate → 9Dn-seen-flush → hook-qza.

### Якоря для переноса патчей на новую версию Instagram

Обфусцированные имена классов плывут от версии к версии, искать по маркерам:

- ads: строка `"Is ad pod"` в boolean-методе; `TigonServiceLayer.startRequest`
- rename: `>Instagram<` в res/values/strings.xml + drawable `instagram_wordmark_ig_wordmark_*`
- quality: JPEG-рампа (`0x46`/`0x5f`), mobileconfig `0x8208a900021394L`, `0x895440` (9 Mbps), `0x8112a600006219L` (720p)
- pkg: глобальная замена пакета в манифесте/smali (НЕ трогать `android:name` с реальными классами)
- modsettings: `media/seen/?reel=` (PendingReelSeenState + reel-модель `A0b(UserSession;J)V` + флаш);
  `AN7(JLjava/lang/String;Ljava/util/List;)V` + строка `ScreenshotDetectorThread`
- wordmark force: `ActionBarTitleViewSwitcher`, `MergedFeedsActionBar`

## Юридическая заметка

Instagram — проприетарный продукт Meta. Модификация и распространение
модифицированного APK нарушают условия использования Meta. Этот репозиторий
содержит только оригинальные патчи автора (лицензия MIT на них) и не содержит
кода Meta. Используй на свой страх и риск, только для личного использования.
