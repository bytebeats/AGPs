package me.bytebeats.agp.inliner

import com.android.build.api.transform.Format
import com.android.build.api.transform.QualifiedContent
import com.android.build.api.transform.Transform
import com.android.build.api.transform.TransformInvocation
import com.android.build.gradle.internal.pipeline.TransformManager
import com.android.utils.FileUtils
import org.apache.commons.codec.digest.DigestUtils
import org.gradle.api.Project
import java.io.File

/**
 * Created by bytebeats on 2021/7/12 : 17:15
 * E-mail: happychinapc@gmail.com
 * Quote: Peasant. Educated. Worker
 */
class InlineRTransform(
    private val mProject: Project,
) : Transform() {
    private val mExtension: InlineRExtension =
        mProject.extensions.getByName(InlineRExtension.EXTENSION_NAME) as InlineRExtension

    override fun getName(): String = TAG
    override fun getInputTypes(): MutableSet<QualifiedContent.ContentType> =
        TransformManager.CONTENT_CLASS

    override fun getScopes(): MutableSet<in QualifiedContent.Scope> =
        TransformManager.SCOPE_FULL_PROJECT

    override fun isIncremental(): Boolean = false

    override fun transform(transformInvocation: TransformInvocation?) {
        println("----------------------------------")
        println("------${TAG} starts-----")
        transformInvocation?.outputProvider?.deleteAll()
        InlineRUtil.clear()
        val jarList = mutableListOf<File>()

        println("------reading R class information------")
        transformInvocation?.inputs?.forEach { input ->
            input.directoryInputs.forEach { directoryInput ->
                if (directoryInput.file.isDirectory) {
                    directoryInput.file.walk().forEach { recurseFile ->
                        if (recurseFile.isFile) {
                            println("directory file reading 1")
                            InlineRUtil.readRMappings(recurseFile)
                        }
                    }
                } else {
                    println("directory file reading 2")
                    InlineRUtil.readRMappings(directoryInput.file)
                }
            }
            input.jarInputs.forEach { jarInput ->
                var jarName = jarInput.name
                val md5 = DigestUtils.md2Hex(jarInput.file.absolutePath)
                if (jarName.endsWith(".jar")) {
                    jarName = jarName.substring(0, jarName.length - 4)
                }
                val dest = transformInvocation.outputProvider.getContentLocation(
                    "${jarName}${md5}",
                    jarInput.contentTypes,
                    jarInput.scopes,
                    Format.JAR
                )
                val src = jarInput.file
                FileUtils.copyFile(src, dest)
                if (src.absolutePath.endsWith(".jar")) {
                    println("jar: ${src.absolutePath}")
                    jarList.add(dest)
                }
            }
        }
        println("------R class information is read------")
        if (!InlineRUtil.isRInfoEmpty()) {

            println("------start inlining R fields into classes------")

            transformInvocation?.inputs?.forEach { input ->
                input.directoryInputs.forEach { directoryInput ->
                    if (directoryInput.file.isDirectory) {
                        directoryInput.file.walk().forEach { recursiveFile ->
                            if (recursiveFile.isFile) {
                                InlineRUtil.replaceAndDeleteRInfoFromFile(recursiveFile, mExtension)
                            }
                        }
                    } else {
                        InlineRUtil.replaceAndDeleteRInfoFromFile(directoryInput.file, mExtension)
                    }

                    val dest = transformInvocation.outputProvider.getContentLocation(
                        directoryInput.name,
                        directoryInput.contentTypes,
                        directoryInput.scopes,
                        Format.DIRECTORY
                    )
                    FileUtils.copyFile(directoryInput.file, dest)
                }
            }
            jarList.forEachIndexed { index, file ->
                if (index < 2) {
                    InlineRUtil.replaceAndDeleteRInfoFromJar(file)
                }
            }

            println("------inlining R fields finished------")
        }

        println("------${TAG} ends-----")
        println("----------------------------------")
    }

    companion object {
        private const val TAG = "inline-r-transform"
    }
}