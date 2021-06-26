package me.bytebeats.agp.chronus

import org.gradle.api.Plugin
import org.gradle.api.Project

class ChronusPlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        project.extensions.create("chronus", ChronusExtension)
        project.gradle.addListener(new TaskTimeKeeper(project.chronus.enabled, project.chronus.verbose))
        project.task("testChronusExtensionAction", type: TestChronusExtensionAction)
    }
}
