package me.bytebeats.agp.tinify

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Created by bytebeats on 2021/7/31 : 15:47
 * E-mail: happychinapc@gmail.com
 * Quote: Peasant. Educated. Worker
 */

/**
 * The Gradle Plugin to use tiny-png api to compress pngs and jpgs.
 */
class TinifyPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.extensions.create(EXTENSION_NAME, TinifyConfigExtension::class.java)
        project.afterEvaluate {
            it.task(TASK_NAME) { TinifyTask::class.java }
        }
    }

    companion object {
        const val EXTENSION_NAME = "tinifyConfig"
        const val TASK_NAME = "tinifyTask"
        const val COMPRESSED_RESOURCES_JSON = "compressed-resources.json"
    }
}