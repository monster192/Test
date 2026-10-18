package com.example.launcher

/**
 * Мост к вашей собственной нативной библиотеке lib<NATIVE_LIB>.so (по умолчанию libPlugin.so).
 *
 * Библиотека кладётся в app/src/main/jniLibs/<abi>/ (например arm64-v8a/libPlugin.so)
 * и должна экспортировать функцию с таким именем (статический метод):
 *
 *   extern "C" JNIEXPORT jint JNICALL
 *   Java_com_example_launcher_NativeBridge_start(JNIEnv* env, jclass clazz,
 *                                                jstring nick, jstring ip, jint port);
 *
 * Возвращает 0 при успехе, любое другое число — код ошибки.
 */
object NativeBridge {

    /** Возвращает null при успехе или текст ошибки. */
    fun load(name: String): String? = try {
        System.loadLibrary(name)
        null
    } catch (e: UnsatisfiedLinkError) {
        e.message ?: "не удалось загрузить библиотеку"
    }

    @JvmStatic
    external fun start(nick: String, ip: String, port: Int): Int
}
