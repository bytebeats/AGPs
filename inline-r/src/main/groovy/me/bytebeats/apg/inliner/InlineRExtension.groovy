package me.bytebeats.apg.inliner

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project

import java.util.regex.Pattern

/**
 * Created by bytebeats on 2021/7/5 : 19:11
 * E-mail: happychinapc@gmail.com
 * Quote: Peasant. Educated. Worker
 */

class InlineRExtension {
    static final EXTENSION_NAME = "inlineRConfig"

    NamedDomainObjectContainer<RKeepInfo> mRKeepInfo

    InlineRExtension(Project project) {
        mRKeepInfo = project.container(RKeepInfo)
    }

    void keepInfo(Action<NamedDomainObjectContainer<RKeepInfo>> action) {
        action.execute(mRKeepInfo)
    }

    /**
     * Should this class be kept?
     * @param className e.g.: me/bytebeats/agp/R$mipmap.class
     * @return
     */
    RKeepInfo shouldKeepRFile(String className) {
        if (mRKeepInfo != null) {
            mRKeepInfo.each { rKeepInfo ->
                if (className == "${rKeepInfo.getRClassName()}.class") {
                    return rKeepInfo
                }
            }
        }
        return null
    }

    static class RKeepInfo {
        String name

        RKeepInfo(String name) {
            this.name = name
        }

        String keepPackageName
        String keepClassName
        List<String> keepResName
        List<String> keepResNameRegex

        private String rClassName = null

        boolean shouldKeep(String fieldName) {
            if (fieldName != null && "" != fieldName) {
                if (keepResName != null && !keepResName.isEmpty()) {
                    for (i in 0..<keepResName.size()) {
                        if (fieldName == keepResName[i]) {
                            return true
                        }
                    }
                }
                if (keepResNameRegex != null && !keepResNameRegex.isEmpty()) {
                    for (i in 0..<keepResNameRegex.size()) {
                        if (Pattern.compile(keepResNameRegex[i]).matcher(fieldName).matches()) {
                            return true
                        }
                    }
                }
            }
            return false
        }

        String getRClassName() {
            if (rClassName == null) {//me.bytebeats.agp.R$mipmap
                rClassName = "${keepPackageName.replaceAll(".", "/")}/R\$${keepClassName}"
            }
            return rClassName
        }
    }
}