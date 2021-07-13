package me.bytebeats.agp.inliner

import org.objectweb.asm.*
import java.io.File
import java.io.FileInputStream
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


    fun clear() {
        mRInfoMap.clear()
    }

    fun isRInfoEmpty(): Boolean {
        println("is r info empty: ${mRInfoMap.isEmpty()}")
        mRInfoMap.forEach { t, u -> println("$t -> $u") }
        return mRInfoMap.isEmpty()
    }

    /**
     * Read all R fields into Map
     * e.g.: "me/bytebeats/agp/R$mipmapic_launcher" = 0x00001
     * @param file
     */
    fun readRMappings(file: File) {
        if (!isRClass(file.absolutePath)) {
            println("not R class: ${file.absolutePath}")
            return
        }
        val fullClassName = getFullClassName(file.absolutePath)
        println("reading R class: $fullClassName")
        FileInputStream(file).use { fis ->
            val reader = ClassReader(fis.readBytes())
            val visitor = object : ClassVisitor(Opcodes.ASM6) {
                override fun visitField(
                    access: Int,
                    name: String?,
                    descriptor: String?,
                    signature: String?,
                    value: Any?
                ): FieldVisitor {
                    if (value is Int) {
                        val key = "${fullClassName.replace(".class", "")}$name"
                        println("R info: $key -> $value")
                        mRInfoMap[key] = value
                    }
                    return super.visitField(access, name, descriptor, signature, value)
                }
            }
            reader.accept(visitor, ClassReader.EXPAND_FRAMES)
        }
    }

    fun replaceAndDeleteRInfoFromFile(classFile: File, extension: InlineRExtension) {
        val fullClassName = getFullClassName(classFile.absolutePath)
        if (isRClassExcludedStyleable(classFile.absolutePath)) {//except R$Styleable.class
            val rKeepInfo = extension.shouldKeepRFile(fullClassName)
            if (rKeepInfo != null) {
                println("R class has fields to keep: ${classFile.absolutePath}")
                FileInputStream(classFile).use { fis ->
                    val reader = ClassReader(fis)
                    val writer = ClassWriter(ClassWriter.COMPUTE_MAXS)
                    val visitor = object : ClassVisitor(Opcodes.ASM6, writer) {
                        override fun visitField(
                            access: Int,
                            name: String?,
                            descriptor: String?,
                            signature: String?,
                            value: Any?
                        ): FieldVisitor? {
                            if (value is Int) {
                                if (rKeepInfo.shouldKeep(name!!)) {
                                    println("Fields $name is kept")
                                    return super.visitField(
                                        access,
                                        name,
                                        descriptor,
                                        signature,
                                        value
                                    )
                                }
                                return null
                            }
                            return super.visitField(access, name, descriptor, signature, value)
                        }
                    }
                    reader.accept(visitor, ClassReader.EXPAND_FRAMES)
                    val bytes = writer.toByteArray()
                    val newClassFile = File(classFile.parentFile, "${classFile.name}.tmp")
                    FileOutputStream(newClassFile).use { os ->
                        os.write(bytes)
                    }
                    classFile.delete()
                    newClassFile.renameTo(classFile)
                }
            } else {
                println("No fields to be replaced. delete this R class file: $fullClassName")
                classFile.delete()
            }
        } else if (isRClass(classFile.absolutePath)) {//R$Styleable.class
            println("delete all static final int fields in $fullClassName")

            FileInputStream(classFile).use { fis ->
                val reader = ClassReader(fis.readBytes())
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
                            return null
                        }
                        return super.visitField(access, name, descriptor, signature, value)
                    }
                }
                reader.accept(visitor, ClassReader.EXPAND_FRAMES)
                val bytes = writer.toByteArray()
                val newClassFile = File(classFile.parentFile, "${classFile.name}.tmp")
                FileOutputStream(newClassFile).use { os ->
                    os.write(bytes)
                }
                classFile.delete()
                newClassFile.renameTo(classFile)
            }
        } else {
            FileInputStream(classFile).use { fis ->
                val bytes =
                    if (isClassFile(classFile.absolutePath)) replaceRInfo(fis.readBytes()) else fis.readBytes()
                val newClassFile = File(classFile.parentFile, "${classFile.name}.tmp")
                FileOutputStream(newClassFile).use { os ->
                    os.write(bytes)
                }
                classFile.delete()
                newClassFile.renameTo(classFile)
            }
        }
    }

    fun replaceAndDeleteRInfoFromJar(srcjar: File) {
        println("replaceAndDeleteRInfoFromJar")
        val newJar = File(srcjar.parentFile, "${srcjar.name}.tmp")
        val jarFile = JarFile(srcjar)
        JarOutputStream(FileOutputStream(newJar)).use { jos ->
            jarFile.entries().asSequence().forEach { entry ->
                jarFile.getInputStream(entry).use { jis ->
                    val zipEntry = ZipEntry(entry.name)
                    var bytes = jis.readBytes()
                    if (isClassFile(entry.name) && !isRClass(entry.name)) {
                        println("replace R info: ${entry.name}")
                        bytes = replaceRInfo(bytes)
                    }
                    jos.putNextEntry(zipEntry)
                    jos.write(bytes)
                    jos.closeEntry()
                }
            }
        }
        jarFile.close()
        srcjar.delete()
        newJar.renameTo(srcjar)
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
                    override fun visitMethodInsn(
                        opcode: Int,
                        owner: String?,
                        name1: String?,
                        descriptor1: String?,
                        isInterface: Boolean
                    ) {
                        val key = owner + name1
                        val value = mRInfoMap[key]
                        if (value == null) {
                            super.visitFieldInsn(opcode, owner, name1, descriptor1)
                        } else {
                            println("Replaced direct reference to R.class: ${owner}-${name1}")
                            super.visitLdcInsn(value)
                        }
                    }
                }
                return methodVisitor
            }
        }
        reader.accept(visitor, ClassReader.EXPAND_FRAMES)
        val result = writer.toByteArray()
        return result
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
        return classFilePath.matches(Regex(".*/R\\\$.*\\.class|.*/R\\.class"))
    }

    /**
     * is R.class or its inner class like R$drawable.class except R$styleable.class
     * {@see #isRClass(String)}
     * @param classFilePath
     * @return
     */
    private fun isRClassExcludedStyleable(classFilePath: String): Boolean {
        return classFilePath.matches(Regex(".*/R\\\$(?!styleable).*?\\.class|.*/R\\.class"))
    }

    private fun isClassFile(classFilePath: String): Boolean {
        return classFilePath.endsWith(".class")
    }

}