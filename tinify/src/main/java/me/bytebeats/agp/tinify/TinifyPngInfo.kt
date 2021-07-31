package me.bytebeats.agp.tinify

/**
 * Created by bytebeats on 2021/7/31 : 15:56
 * E-mail: happychinapc@gmail.com
 * Quote: Peasant. Educated. Worker
 */

/**
 * A data class to imply sizes of png file before and after compressing
 */
data class TinifyPngInfo(
    val path: String,
    val sizeBefore: String,
    val sizeAfter: String,
    val md5: String
)
