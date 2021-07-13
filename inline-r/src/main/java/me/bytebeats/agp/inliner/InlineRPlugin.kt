package me.bytebeats.agp.inliner

import com.android.build.gradle.AppExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Created by bytebeats on 2021/7/5 : 19:07
 * E-mail: happychinapc@gmail.com
 * Quote: Peasant. Educated. Worker
 */

open class InlineRPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val android = project.extensions.getByType(AppExtension::class.java)
        project.extensions.create(
            InlineRExtension.EXTENSION_NAME,
            InlineRExtension::class.java,
            project
        )
        android.registerTransform(InlineRTransform(project))
    }
}