package me.bytebeats.agp.chronus

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

/**
 * Created by bytebeats on 2021/6/26 : 20:54
 * E-mail: happychinapc@gmail.com
 * Quote: Peasant. Educated. Worker
 */
class TestChronusExtensionAction extends DefaultTask {

    @TaskAction
    void checkExtension() {
        println("chronus enabled = ${project.chronus.enabled}")
        println("chronus verbose = ${project.chronus.verbose}")
    }
}
