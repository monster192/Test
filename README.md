# Launcher (Kotlin, без внешних зависимостей)

Альбомный лаунчер: ник, выбор сервера, онлайн (запрос статуса SA-MP по UDP),
кнопки соцсетей, «Починить игру», «ИГРАТЬ».

## Настройка (app/build.gradle.kts, верхний блок)
- `serversUrl`    — https-ссылка на servers.json. Пусто = берётся app/src/main/assets/servers.json.
- `manifestUrl`   — https-ссылка на manifest.json со списком файлов для загрузки. Пусто = без обновлений.
- `launchMode`    — `"jni"` или `"intent"`.
- `nativeLib`     — для `jni`: имя библиотеки (`Plugin` = libPlugin.so).
- `clientPackage` — для `intent`: пакет приложения-клиента.

## servers.json
```json
{ "servers": [ {
  "name": "MAIN SERVER", "ip": "1.2.3.4", "port": 7777,
  "subtitle": "SAMP ONLINE PROJECT", "title": "MY SERVER",
  "description": "Текст на карточке",
  "telegram": "", "vk": "", "discord": ""
} ] }
```
Можно несколько серверов: нажатие на плашку сервера вверху открывает выбор.

## Режим JNI
Положите свою библиотеку в `app/src/main/jniLibs/arm64-v8a/libPlugin.so`.
Она должна экспортировать (статический метод):
```c
JNIEXPORT jint JNICALL Java_com_example_launcher_NativeBridge_start(
    JNIEnv* env, jclass clazz, jstring nick, jstring ip, jint port);
```
Верните 0 при успехе, другое число — код ошибки (покажется в статусе).

## Режим Intent
Лаунчер запускает приложение `clientPackage` и передаёт extras: `nickname`, `ip`, `port`.

## Сборка APK без компьютера
Загрузите проект в GitHub (вместе с папкой `.github`), откройте Actions → «Build APK»,
скачайте артефакт `launcher-apk`.

## manifest.json (обновления файлов)
```json
{ "files": [ { "path": "data/file.bin", "url": "https://cdn.example.com/file.bin", "sha256": "<hex>" } ] }
```
