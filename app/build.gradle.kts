// ======================= НАСТРОЙКИ ЛАУНЧЕРА =======================
// Ссылка (https) на servers.json с сервером(ами). Пусто = используется assets/servers.json из APK.
val serversUrl    = ""
// Ссылка (https) на manifest.json со списком файлов для загрузки. Пусто = обновления выключены.
val manifestUrl   = ""
// Способ запуска игры: "jni" (своя библиотека lib<nativeLib>.so) или "intent" (отдельное приложение-клиент).
val launchMode    = "intent"
// Имя библиотеки для режима "jni": "Plugin" означает файл libPlugin.so (кладётся в app/src/main/jniLibs/<abi>/).
val nativeLib     = "Plugin"
// Пакет приложения-клиента для режима "intent" (например "com.company.client"). Пусто = клиент не подключён.
val clientPackage = "com.example.crmpclient"
// ===================================================================

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.launcher"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.launcher"
        minSdk = 24
        targetSdk = 34
        versionCode = 2
        versionName = "1.1"

        buildConfigField("String", "SERVERS_URL", "\"$serversUrl\"")
        buildConfigField("String", "MANIFEST_URL", "\"$manifestUrl\"")
        buildConfigField("String", "LAUNCH_MODE", "\"$launchMode\"")
        buildConfigField("String", "NATIVE_LIB", "\"$nativeLib\"")
        buildConfigField("String", "CLIENT_PACKAGE", "\"$clientPackage\"")
        manifestPlaceholders["clientPackage"] = clientPackage.ifBlank { "none.client" }
    }

    buildFeatures { buildConfig = true }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}
