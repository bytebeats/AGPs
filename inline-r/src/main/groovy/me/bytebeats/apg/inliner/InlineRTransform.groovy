package me.bytebeats.apg.inliner

import com.android.build.api.transform.Context
import com.android.build.api.transform.Format
import com.android.build.api.transform.QualifiedContent
import com.android.build.api.transform.Transform
import com.android.build.api.transform.TransformException
import com.android.build.api.transform.TransformInput
import com.android.build.api.transform.TransformInvocation
import com.android.build.api.transform.TransformOutputProvider
import com.android.build.gradle.internal.pipeline.TransformManager
import com.android.utils.FileUtils
import org.apache.commons.codec.digest.DigestUtils
import org.gradle.api.Project


/**
 * Created by bytebeats on 2021/7/5 : 19:10
 * E-mail: happychinapc@gmail.com
 * Quote: Peasant. Educated. Worker
 */

class InlineRTransform extends Transform {
    private static final TAG = "inline-r-transform"

    private Project mProject
    private InlineRExtension mExtension

    InlineRTransform(Project mProject) {
        this.mProject = mProject
        this.mExtension = this.mProject.extensions.getByName(InlineRExtension.EXTENSION_NAME)
    }

    @Override
    String getName() {
        return TAG
    }

    @Override
    Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_CLASS
    }

    @Override
    Set<? super QualifiedContent.Scope> getScopes() {
        return TransformManager.SCOPE_FULL_PROJECT
    }

    @Override
    boolean isIncremental() {
        return false
    }

    @Override
    void transform(TransformInvocation transformInvocation) throws TransformException, InterruptedException, IOException {
        println "----------------------------------"
        println "------${TAG} starts-----"
        transformInvocation.outputProvider.deleteAll()
        InlineRUtil.clear()
        def jarList = []

        println "------reading R class information------"
        transformInvocation.inputs.each { input ->
            input.directoryInputs.each { directoryInput ->
                if (directoryInput.file.isDirectory()) {
                    directoryInput.file.eachFileRecurse { recurseFile ->
                        if (recurseFile.isFile()) {
                            InlineRUtil.readRMappings(recurseFile)
                        }
                    }
                } else {
                    InlineRUtil.readRMappings(directoryInput)
                }
            }
            input.jarInputs.each { jarInput ->
                def jarName = jarInput.name
                def md5 = DigestUtils.md2Hex(jarInput.file.absolutePath)
                if (jarName.endsWith(".jar")) {
                    jarName = jarName.substring(0, jarName.size() - 4)
                }
                def dest = transformInvocation.outputProvider.getContentLocation("${jarName}${md5}", jarInput.contentTypes, jarInput.scopes, Format.JAR)
                def src = jarInput.file
                FileUtils.copyFile(src, dest)
                if (src.path.contains("com.squareup")) {
                    //todo add excluded package configurations
                } else {
                    jarList.add(dest)
                }
            }
        }
        println "------R class information is read------"

        println "------replace all places where R.class is referred------"

        transformInvocation.inputs.each {input ->
            input.directoryInputs.each { directoryInput ->
                if (directoryInput.file.isDirectory()) {
                    directoryInput.file.eachFileRecurse { recursiveFile ->
                        if (recursiveFile.isFile()) {

                        }
                    }
                } else {

                }

                def dest = transformInvocation.outputProvider.getContentLocation(directoryInput.name, directoryInput.contentTypes, directoryInput.scopes, Format.DIRECTORY)
                FileUtils.copyFile(directoryInput.file, dest)
            }
        }

        for (File jar : jarList) {

        }

        println "------${TAG} ends-----"
        println "----------------------------------"
    }
}