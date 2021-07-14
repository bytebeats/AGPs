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
    private var groupRKeepInfos = mutableMapOf<String, List<RKeepInfo>>()

    fun keepInfo(action: Action<NamedDomainObjectContainer<RKeepInfo>>) {//called by framework
        action.execute(mRKeepInfo)
        mRKeepInfo.groupBy { it.keepPackageName.orEmpty() }.toMap(groupRKeepInfos)
        println(mRKeepInfo)
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

    /**
     * Should this class be kept?
     * @param packageName e.g.: me/bytebeats/agp/R$mipmap.class
     * @return
     */
    fun shouldKeepRPackage(packageName: String?): List<RKeepInfo>? {
        return groupRKeepInfos[packageName?.replace('/', '.')]
    }

    override fun toString(): String {
        return "InlineRExtension(mRKeepInfo=${mRKeepInfo.toString()}, groupRKeepInfos=$groupRKeepInfos)"
    }

    /**
     * @param name "anim", "array", "attr", "bool", "color", "dimen", "drawable", "id", "integer", "layout", "menu", "plurals", "string", "style", "styleable"
     */
    class RKeepInfo(name: String) {
        var name: String? = name

        var keepPackageName: String? = null
        var keepResName = mutableListOf<String>()
        var keepResNameRegex = mutableListOf<String>()

        var rClassName: String? = null
            get() {
                if (field == null) {//me.bytebeats.agp.R$mipmap
                    field = "${keepPackageName?.replace(".", "/")}/R$${name}"
                }
                return field
            }

        fun shouldKeep(fieldName: String?): Boolean {
            fieldName ?: return false
            if (keepResName.isNotEmpty()) {
                for (i in 0 until keepResName.size) {
                    if (fieldName == keepResName[i]) {
                        return true
                    }
                }
            }
            if (keepResNameRegex.isNotEmpty()) {
                for (i in 0 until keepResNameRegex.size) {
                    if (Pattern.compile(keepResNameRegex[i]).matcher(fieldName).matches()) {
                        return true
                    }
                }
            }
            return false
        }

        override fun toString(): String {
            return "RKeepInfo(name='$name', keepPackageName=$keepPackageName, keepResName=$keepResName, keepResNameRegex=$keepResNameRegex, rClassName=$rClassName)"
        }
    }


    companion object {
        const val EXTENSION_NAME = "inlineRConfig"
    }
}