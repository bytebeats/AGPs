package me.bytebeats.agp.inliner

import org.objectweb.asm.*
import java.io.File
import java.io.FileOutputStream
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import java.util.zip.ZipEntry

/**
 * Created by bytebeats on 2021/7/12 : 16:23
 * E-mail: happychinapc@gmail.com
 * Quote: Peasant. Educated. Worker
 */
object InlineRUtil {

    /**
     * mappings e.g.: me/bytebeats/agp.R$mipmap.class#ic_launcher => 0x0000001
     */
    private val mRInfoMap = mutableMapOf<String, Int>()
    var mFieldCount = 0
        private set


    fun clear() {
        mRInfoMap.clear()
        mFieldCount = 0
    }

    fun getRInfoMappingSize(): Int = mRInfoMap.size

    /**
     * Read all R fields into Map and delete from jar r class
     * e.g.: "me/bytebeats/agp/R$mipmapic_launcher" = 0x00001
     * @param jFile
     */
    fun collectAndDeleteRFieldsFromJar(jFile: File, extension: InlineRExtension) {
        println("collectAndDeleteRFieldsFromJar: ${jFile.path}")
        val tgtJar = File(jFile.parentFile, "${jFile.name}.inliner.tmp")
        tgtJar.createNewFile()
        JarOutputStream(FileOutputStream(tgtJar)).use { jos ->
            val jarFile = JarFile(jFile)
            jarFile.entries().iterator().forEachRemaining { jarEntry ->
                val entryName = jarEntry.name
                if (!isRClass(entryName)) {
                    println("not R class jar: $entryName")
                } else {
                    val lastSlashIdx = entryName.lastIndexOf('/')
                    val pkg = entryName.substring(0, lastSlashIdx)
                    val keepRInfo = extension.shouldKeepRPackage(pkg)
//                    println("keepRInfo: ${keepRInfo.toString()}")
                    val shortName = entryName.substring(lastSlashIdx + 1)//R.class or R$id.class
                    val isStyleable = shortName.contains("styleable")
                    val rInnerClassShortName = shortName.replace("R\$", "").replace(".class", "")
                    var toBeKept = false
                    var bytes = jarFile.getInputStream(jarEntry).readBytes()
                    val reader = ClassReader(bytes)
                    val writer = ClassWriter(ClassWriter.COMPUTE_FRAMES)
                    val visitor = object : ClassVisitor(Opcodes.ASM6, writer) {
                        override fun visitField(
                            access: Int,
                            name: String?,
                            descriptor: String?,
                            signature: String?,
                            value: Any?
                        ): FieldVisitor? {
                            if (value is Int) {
                                val key = "${entryName.replace(".class", "")}#$name"
                                mFieldCount++
                                if (keepRInfo?.find { it.name == rInnerClassShortName }
                                        ?.shouldKeep(name) == true) {
                                    toBeKept = true
                                    println("kept $key = $value")
                                } else {
                                    toBeKept = false
                                    mRInfoMap[key] = value
//                                    println("removed $key = $value")
                                    return null
                                }
                            }
                            return super.visitField(access, name, descriptor, signature, value)
                        }
                    }
                    reader.accept(visitor, ClassReader.EXPAND_FRAMES)
                    bytes = writer.toByteArray()
                    if (bytes.isNotEmpty() && (toBeKept || isStyleable)) {
                        val zipEntry = ZipEntry(entryName)
                        jos.putNextEntry(zipEntry)
                        jos.write(bytes)
                        jos.closeEntry()
                    }
                }

            }
            jarFile.close()
            jFile.delete()
            jos.close()
            tgtJar.renameTo(jFile)
        }
    }

    fun replaceJarFileRInfo(srcJar: File) {
        val tgtJar = File(srcJar.parentFile, "${srcJar.name}.inliner.tmp")
        val srcJarFile = JarFile(srcJar)
        JarOutputStream(FileOutputStream(tgtJar)).use { jos ->
            srcJarFile.entries().asSequence().forEach { entry ->
                srcJarFile.getInputStream(entry).use { jis ->
                    val zipEntry = ZipEntry(entry.name)
                    var bytes = jis.readBytes()
                    if (isClassFile(entry.name) && !isRClass(entry.name)) {
                        bytes = replaceRInfo(bytes)
                    }
                    if (bytes.isNotEmpty()) {
                        jos.putNextEntry(zipEntry)
                        jos.write(bytes)
                        jos.closeEntry()
                    }
                }
            }
        }
        srcJarFile.close()
        srcJar.delete()
        tgtJar.renameTo(srcJar)
    }

    fun replaceDirectoryFileRInfo(dFile: File) {
        if (isClassFile(dFile.path) && !isRClass(dFile.path)) {
            val tgtFile = File(dFile.parentFile, "${dFile.name}.inliner.tmp")
            FileOutputStream(tgtFile).use { fos ->
                var bytes = dFile.readBytes()
                bytes = replaceRInfo(bytes)
                fos.write(bytes)
                fos.close()
                dFile.delete()
                tgtFile.renameTo(dFile)
            }
        }
    }

    private fun replaceRInfo(bytes: ByteArray): ByteArray {
        val reader = ClassReader(bytes)
        val writer = ClassWriter(ClassWriter.COMPUTE_MAXS)
        val visitor = object : ClassVisitor(Opcodes.ASM6, writer) {
            override fun visitMethod(
                access: Int,
                name: String?,
                descriptor: String?,
                signature: String?,
                exceptions: Array<out String>?
            ): MethodVisitor {
                var methodVisitor =
                    super.visitMethod(access, name, descriptor, signature, exceptions)
                methodVisitor = object : MethodVisitor(Opcodes.ASM6, methodVisitor) {
                    override fun visitFieldInsn(
                        opcode: Int,
                        owner: String?,
                        name1: String?,
                        descriptor1: String?
                    ) {
                        val key = "$owner#$name1"
                        val value = mRInfoMap[key]
                        if (value == null) {
                            super.visitFieldInsn(opcode, owner, name1, descriptor1)
                        } else {
                            super.visitLdcInsn(value)
                        }
                    }
                }
                return methodVisitor
            }
        }
        reader.accept(visitor, ClassReader.EXPAND_FRAMES)
        return writer.toByteArray()
    }

    /**
     * 依赖工程中形如: ../app/build/intermediates/classes/debug/me/bytebeats/agp/app/R.class ==> me/bytebeats/agp/app/R.class
     * 根据不同 Android Studio 版本, 不同编译配置, 路径可能不同:
     * ../app/build/intermediates/javac/officialDebug/compileOfficialDebugJavaWithJavac/classes/android/arch/lifecycle/R.class ==> android/arch/lifecycle/R.class
     * 但是, 不管是当前工程, 还是依赖工程, 还是远程依赖 aar 包, 在打包编译时, 都会在工程 app/build/intermediates/classes 包下生成系列 R.class 文件
     * 根据构建模式是 debug 还是 release 还是 fast, 可以从中截取出 R.class 文件的包名.
     * @param filePath 在app/build/intermediates/classes路径下生成的文件的绝对路径
     * @return 返回形如: me/bytebeats/agp/app/R.class, me/bytebeats/agp/module1/R$mipmap.class的类名
     */
    private fun getFullClassName(filePath: String): String {
        var relativeClassFilePath = filePath
        var mode = "/debug/"
        var idx = relativeClassFilePath.indexOf(mode)
        if (idx == -1) {
            mode = "/release/"
            idx = relativeClassFilePath.indexOf(mode)
        }
        if (idx != -1) {
            val result =
                relativeClassFilePath.substring(idx).replace(mode, "")
            println("tgt 1: $result")
            return result
        }
        idx = relativeClassFilePath.indexOf("/classes")
        relativeClassFilePath =
            relativeClassFilePath.substring(idx).replace("/classes", "").replace("/debug", "")
                .replace("/release", "")
        relativeClassFilePath = relativeClassFilePath.substring(1)
        println("tgt 2: $relativeClassFilePath")
        return relativeClassFilePath
    }

    /**
     * is R.class or its inner class like R$mipmap.class
     * @param classFilePath 形如 ../app/build/intermediates/classes/debug/me/bytebeats/agp/app/R.class
     * @return true if this class is R.class or its inner class like R$id.class
     */
    private fun isRClass(classFilePath: String): Boolean {
        return classFilePath.matches(Regex(".*/R\\$.*\\.class|.*/R\\.class"))
    }

    /**
     * is R.class or its inner class like R$drawable.class except R$styleable.class
     * {@see #isRClass(String)}
     * @param classFilePath
     * @return
     */
    private fun isRClassExcludedStyleable(classFilePath: String): Boolean {
        return classFilePath.matches(Regex(".*/R\\$(?!styleable).*?\\.class|.*/R\\.class"))
    }

    private fun isClassFile(classFilePath: String): Boolean {
        return classFilePath.endsWith(".class")
    }

}