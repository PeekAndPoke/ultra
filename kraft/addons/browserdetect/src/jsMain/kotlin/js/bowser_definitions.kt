package de.peekandpoke.kraft.addons.browserdetect.js

@JsModule("bowser")
@JsNonModule
external class Bowser internal constructor() {

    companion object {
        fun getParser(userAgent: String): Bowser
    }

    interface Browser {
        val name: String?
        val version: String?
    }

    interface OS {
        val name: String?
        val version: String?
        val versionName: String?
    }

    interface Platform {
        val type: String?
    }

    interface Engine {
        val name: String?
        val version: String?
    }

    fun getBrowser(): Browser

    fun getOS(): OS

    fun getPlatform(): Platform

    fun getEngine(): Engine
}

