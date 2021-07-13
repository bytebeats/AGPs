package me.bytebeats.agp.inliner

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import java.util.regex.Pattern

/**
 * Created by bytebeats on 2021/7/12 : 16:11
 * E-mail: happychinapc@gmail.com
 * Quote: Peasant. Educated. Worker
 */
open class InlineRExtension(private val project: Project) {
    private val mRKeepInfo = project.container(RKeepInfo::class.java)

    fun keepInfo(action: Action<NamedDomainObjectContainer<RKeepInfo>>) {
        action.execute(mRKeepInfo)
    }

    /**
     * Should this class be kept?
     * @param className e.g.: me/bytebeats/agp/R$mipmap.class
     * @return
     */
    fun shouldKeepRFile(className: String?): RKeepInfo? {
        mRKeepInfo?.forEach { rKeepInfo ->
            if (className == "${rKeepInfo.rClassName}.class") {
                return rKeepInfo
            }
        }
        return null
    }

    inner class RKeepInfo(private val name: String) {
        var keepPackageName: String? = null
        var keepClassName: String? = null
        val keepResName = mutableListOf<String>()
        val keepResNameRegex = mutableListOf<String>()

        var rClassName: String? = null
            get() {
                if (field == null) {//me.bytebeats.agp.R$mipmap
                    field = "${keepPackageName?.replace(".", "/")}/R\$${keepClassName}"
                }
                return field!!
            }

        fun shouldKeep(fieldName: String): Boolean {
            if ("" != fieldName) {
                if (keepResName.isNotEmpty()) {
                    for (i in 0..keepResName.size) {
                        if (fieldName == keepResName[i]) {
                            return true
                        }
                    }
                }
                if (keepResNameRegex.isNotEmpty()) {
                    for (i in 0..keepResNameRegex.size) {
                        if (Pattern.compile(keepResNameRegex[i]).matcher(fieldName).matches()) {
                            return true
                        }
                    }
                }
            }
            return false
        }
    }

    companion object {
        const val EXTENSION_NAME = "inlineRConfig"
    }
}