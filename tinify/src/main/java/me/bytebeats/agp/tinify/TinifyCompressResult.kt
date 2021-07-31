package me.bytebeats.agp.tinify

/**
 * Created by bytebeats on 2021/7/31 : 15:59
 * E-mail: happychinapc@gmail.com
 * Quote: Peasant. Educated. Worker
 */
data class TinifyCompressResult(
    val totalSizeBefore: Long,
    val totalSizeAfter: Long,
    val hasError: Boolean,
    val compressedPngInfos: List<TinifyPngInfo>
) {
    override fun toString(): String {
        return "TinifyCompressResult(totalSizeBefore=$totalSizeBefore, totalSizeAfter=$totalSizeAfter, hasError=$hasError, compressedPngInfos=$compressedPngInfos)"
    }
}