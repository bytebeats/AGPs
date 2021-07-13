package me.bytebeats.agp.inliner

import com.android.build.api.transform.*
import com.android.utils.FileUtils
import org.apache.commons.codec.digest.DigestUtils
import java.io.File

/**
 * Created by bytebeats on 2021/7/13 : 11:50
 * E-mail: happychinapc@gmail.com
 * Quote: Peasant. Educated. Worker
 */

fun JarInput.getDestAfterOutput(provider: TransformOutputProvider): File {
    var jarName = name
    val md5 = DigestUtils.md2Hex(file.absolutePath)
    if (jarName.endsWith(".jar")) {
        jarName = jarName.substring(0, jarName.length - 4)
    }
    val dest = provider.getContentLocation(
        "${jarName}${md5}",
        contentTypes,
        scopes,
        Format.JAR
    )
    FileUtils.copyFile(file, dest)
    return dest
}

fun DirectoryInput.getDestAfterOutput(provider: TransformOutputProvider): File {
    val dest = provider.getContentLocation(
        name,
        contentTypes,
        scopes,
        Format.DIRECTORY
    )
    FileUtils.copyFile(file, dest)
    return dest
}