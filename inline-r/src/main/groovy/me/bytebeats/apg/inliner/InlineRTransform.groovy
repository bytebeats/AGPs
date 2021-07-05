package me.bytebeats.apg.inliner

import com.android.build.api.transform.QualifiedContent
import com.android.build.api.transform.Transform
import com.android.build.gradle.internal.pipeline.TransformManager
import org.gradle.api.Project


/**
 * Created by bytebeats on 2021/7/5 : 19:10
 * E-mail: happychinapc@gmail.com
 * Quote: Peasant. Educated. Worker
 */

class InlineRTransform extends Transform {
    private static final TAG = "inline-r-transform"

    private Project mProject

    InlineRTransform(Project mProject) {
        this.mProject = mProject
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
}