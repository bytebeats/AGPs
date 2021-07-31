package me.bytebeats.agp.tinify

import com.tinify.*
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import me.bytebeats.agp.tinify.TinifyPlugin.Companion.COMPRESSED_RESOURCES_JSON
import me.bytebeats.agp.tinify.TinifyPlugin.Companion.EXTENSION_NAME
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.math.BigInteger
import java.security.MessageDigest
import java.text.DecimalFormat
import kotlin.Exception

/**
 * Created by bytebeats on 2021/7/31 : 16:02
 * E-mail: happychinapc@gmail.com
 * Quote: Peasant. Educated. Worker
 */

/**
 * A Gradle Task to compress pngs and jpgs.
 */
class TinifyTask : DefaultTask() {
    private val tinifyConfig =
        project.extensions.getByName(EXTENSION_NAME) as TinifyConfigExtension

    init {
        description = "Tinify Compress Pngs&Jpgs"
        group = "tinify"
        outputs.upToDateWhen { false }
    }

    @TaskAction
    fun compress() {
        println(tinifyConfig.toString())

        if (tinifyConfig.apiKey?.isEmpty() == true) {
            println("apiKey should be assigned")
            return
        }

        if (tinifyConfig.resDirs.isNullOrEmpty()) {
            println("resDirs can't be null")
            return
        }
        try {
            Tinify.setKey(tinifyConfig.apiKey)
            Tinify.validate()
        } catch (e: Exception) {
            println("apiKey is not validated by Tinify")
            e.printStackTrace()
            return
        }

        val compressedPngs = mutableListOf<TinifyPngInfo>()
        val compressedJsonFile = File("${project.projectDir}/$COMPRESSED_RESOURCES_JSON")
        if (!compressedJsonFile.exists()) {
            compressedJsonFile.createNewFile()
        } else {
            try {
                val json = JsonSlurper().parse(compressedJsonFile, "UTF-8")
                if (json is ArrayList<*>) {
                    json.forEach { compressedPngs.add(it as TinifyPngInfo) }
                } else {
                    println("$COMPRESSED_RESOURCES_JSON is invalid")
                }
            } catch (ignore: Exception) {
                println("Failed in parsing $COMPRESSED_RESOURCES_JSON ${ignore.message}")
            }
        }

        var sizeBefore = 0L
        var sizeAfter = 0L
        var hasError = false
        val newCompressedPngs = mutableListOf<TinifyPngInfo>()
        tinifyConfig.resDirs?.forEach { d ->
            val dir = File(d)
            if (dir.exists() && dir.isDirectory) {
                if (tinifyConfig.resPatterns.isNullOrEmpty()) {
                    tinifyConfig.resPatterns = listOf("drawable[a-z-]*")
                }
                tinifyConfig.resPatterns?.forEach { pattern ->
                    dir.walk().filter { it.isDirectory && it.path.matches(Regex(pattern)) }
                        .forEach { resDir ->
                            if (!hasError) {
                                val compressResult = doCompress(resDir, tinifyConfig.whiteList, compressedPngs)
                                sizeBefore += compressResult.totalSizeBefore
                                sizeAfter += compressResult.totalSizeAfter
                                hasError = compressResult.hasError
                                if (compressResult.compressedPngInfos.isNotEmpty()) {
                                    newCompressedPngs.addAll(compressResult.compressedPngInfos)
                                }
                            }
                        }
                }
            }
        }
        if (newCompressedPngs.isNotEmpty()) {
            for (pngInfo in newCompressedPngs) {
                val idx = compressedPngs.indexOfFirst { it.path == pngInfo.path }
                if (idx > 0) {
                    compressedPngs.add(idx, pngInfo)
                } else {
                    compressedPngs.add(0, pngInfo)
                }
            }
            FileOutputStream(compressedJsonFile).use { fos ->
                JsonOutput.prettyPrint(JsonOutput.toJson(compressedPngs))
                    .toByteArray(Charsets.UTF_8)
                    .let {
                        fos.write(it)
                        println(
                            "Task finish, compress ${newCompressedPngs.size} files, before total size: ${
                                formatFileSize(
                                    sizeBefore
                                )
                            } after total size: ${formatFileSize(sizeAfter)}"
                        )
                    }
            }
        }
    }


    companion object {
        private fun formatFileSize(size: Long): String {
            val format = DecimalFormat("#.00")
            if (size == 0L) return "0B"
            return if (size < 1024) {
                "${format.format(size.toDouble())}B"
            } else if (size < 1024 * 1024) {
                "${format.format(size.toDouble() / 1024)}KB"
            } else if (size < 1024 * 1024 * 1024) {
                "${format.format(size.toDouble() / 1024 / 1024)}MB"
            } else {
                "${format.format(size.toDouble() / 1024 / 1024 / 1024)}GB"
            }
        }

        private fun md5(file: File): String {
            val digest = MessageDigest.getInstance("MD5")
            FileInputStream(file).use { fis ->
                val buffer = ByteArray(8192)
                var read = fis.read(buffer)
                while (read > 0) {
                    digest.update(buffer, 0, read)
                    read = fis.read(buffer)
                }
            }
            val md5Bytes = digest.digest()
            val bigInt = BigInteger(1, md5Bytes)
            return bigInt.toString(16).padStart(32, '0')
        }

        fun doCompress(
            resDir: File,
            whiteList: Collection<String>?,
            compressedList: MutableList<TinifyPngInfo>
        ): TinifyCompressResult {
            val newCompressList = mutableListOf<TinifyPngInfo>()
            var accountError = false
            var totalSizeBefore = 0L
            var totalSizeAfter = 0L

            resDir.listFiles()?.let {
                outer@ for (file in it) {
                    val path = file.path
                    val name = file.name
                    if (whiteList != null) {
                        for (item in whiteList) {
                            if (name.matches(Regex(item))) {
                                println("matches the white list, skip >>>>>>>> $path")
                                continue@outer
                            }
                        }
                    }
                    outer@ for (info in compressedList) {
                        if (path == info.path || info.md5 == md5(file)) {
                            continue@outer
                        }
                    }
                    if (name.endsWith(".png") && !name.endsWith(".9.png") || name.equals(".jpg")) {
                        println("trying to compress >>>>>>> $path")
                        try {
                            FileInputStream(file).use { fis ->
                                val sizeBefore = fis.available().toLong()
                                val formattedSizeBefore = formatFileSize(sizeBefore)

                                val tinifySource = Tinify.fromFile("$resDir/$name")
                                tinifySource.toFile("$resDir/$name")

                                val sizeAfter = fis.available().toLong()
                                val formattedSizeAfter = formatFileSize(sizeAfter)

                                totalSizeBefore += sizeBefore
                                totalSizeAfter += sizeAfter
                                newCompressList.add(
                                    TinifyPngInfo(
                                        path,
                                        formattedSizeBefore,
                                        formattedSizeAfter,
                                        md5(file)
                                    )
                                )
                                println("compressed. $formattedSizeBefore ---> $formattedSizeAfter")
                            }
                        } catch (ae: AccountException) {
                            // Verify your API key and account limit.
                            println("AccountException: ${ae.message}")
                            accountError = true
                            break
                        } catch (ce: ClientException) {
                            // Check your source image and request options.
                            println("ClientException: ${ce.message}")
                        } catch (se: ServerException) {
                            // Temporary issue with the Tinify API.
                            println("ServerException: ${se.message}")
                        } catch (ce: ConnectionException) {
                            // A network connection error occurred.
                            println("ConnectionException: ${ce.message}")
                        } catch (e: Exception) {
                            // Something else went wrong, unrelated to the Tinify API.
                            println("ConnectionException: ${e.message}")
                        }
                    }
                }
            }
            return TinifyCompressResult(
                totalSizeBefore,
                totalSizeAfter,
                accountError,
                newCompressList
            )
        }
    }
}