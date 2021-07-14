package me.bytebeats.agp.inliner

import com.android.build.api.transform.*
import com.android.build.gradle.internal.pipeline.TransformManager
import org.gradle.api.Project

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
        val outputProvider =
            transformInvocation?.outputProvider ?: return super.transform(transformInvocation)
        val startTime = System.currentTimeMillis()
        outputProvider.deleteAll()
        InlineRUtil.clear()
        val libJarList = mutableListOf<JarInput>()
        val rJarList = mutableListOf<JarInput>()

        println("------collecting R fields------")
        transformInvocation.inputs?.forEach { input ->
            input.jarInputs.forEach { jarInput ->
                if (jarInput.file.absolutePath.endsWith(".jar")) {
                    if (jarInput.file.path.contains("compile_and_runtime_not_namespaced_r_class_jar")) {
                        rJarList.add(jarInput)
                    } else {
                        libJarList.add(jarInput)
                    }
                }
            }
        }
        for (rJarFile in rJarList) {
            val dest = rJarFile.getDestAfterOutput(outputProvider)
            InlineRUtil.collectAndDeleteRFieldsFromJar(dest, mExtension)
        }
        println("------R fields are collected------")

        val totalRFieldCount = InlineRUtil.mFieldCount
        val collectedRFieldCount = InlineRUtil.getRInfoMappingSize()
        val keepSize = totalRFieldCount - collectedRFieldCount
        println("R jar file count = ${rJarList.size}, all r fields count = $totalRFieldCount where $collectedRFieldCount is deleted and $keepSize is kept")

        println("------start inlining R fields into classes------")
        println("------start inlining Jar files------")

        for (libJarFile in libJarList) {
            val dest = libJarFile.getDestAfterOutput(outputProvider)
            InlineRUtil.replaceJarFileRInfo(dest)
        }

        println("------start inlining Directory files------")

        transformInvocation.inputs?.forEach { input ->
            input.directoryInputs.forEach { directoryInput ->
                if (directoryInput.file.isDirectory) {
                    directoryInput.file.walk().forEach { recursiveFile ->
                        if (recursiveFile.isFile) {
//                            println("file: ${recursiveFile.relativeTo(directoryInput.file)}")
                            InlineRUtil.replaceDirectoryFileRInfo(recursiveFile)
                        }
                    }
                }

//                InlineRUtil.replaceDirectoryFileRInfo2(directoryInput.file)

                directoryInput.getDestAfterOutput(outputProvider)
            }
        }
        println("------inlining R fields finished------")
        rJarList.clear()
        libJarList.clear()
        val cost = (System.currentTimeMillis() - startTime) / 1000L
        println("------${TAG} cost $cost s-----")
        println("------${TAG} ends-----")
        println("----------------------------------")
    }

    companion object {
        private const val TAG = "inline-r-transform"
    }
}