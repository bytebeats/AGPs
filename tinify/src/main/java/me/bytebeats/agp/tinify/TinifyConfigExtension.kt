package me.bytebeats.agp.tinify

/**
 * Created by bytebeats on 2021/7/31 : 15:47
 * E-mail: happychinapc@gmail.com
 * Quote: Peasant. Educated. Worker
 */


/**
 * Gradle DSL extension for tiny png plugin
 */
open class TinifyConfigExtension {
    /**
     * apiKey from tinypng.com for application to use
     */
    var apiKey: String? = ""

    /**
     * resource directories you do not want to compress pngs and jpgs
     */
    var whiteList: List<String>? = null

    /**
     * resource directories you DO want to compress pngs and jpgs
     */
    var resDirs: List<String>? = null

    /**
     * resource patterns you DO want to compress pngs and jpgs
     */
    var resPatterns: List<String>? = null
    override fun toString(): String {
        return "TinifyConfigExtension(apiKey='$apiKey', whiteList=$whiteList, resDirs=$resDirs, resPattern=$resPatterns)"
    }
}