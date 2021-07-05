package me.bytebeats.apg.inliner

import com.android.build.gradle.AppExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Created by bytebeats on 2021/7/5 : 19:07
 * E-mail: happychinapc@gmail.com
 * Quote: Peasant. Educated. Worker
 */
class InlineRPlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        def android = project.extensions.getByType(AppExtension)
        project.extensions.create(InlineRExtension.EXTENSION_NAME, InlineRExtension)
        android.registerTransform(new InlineRTransform(project))
    }
}
