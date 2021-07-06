package me.bytebeats.apg.inliner

import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

/**
 * Created by bytebeats on 2021/7/5 : 19:07
 * E-mail: happychinapc@gmail.com
 * Quote: Peasant. Educated. Worker
 */

class InlineRUtil {
    private InlineRUtil() {

    }

    /**
     * mappings e.g.: me/bytebeats/agp.R$mipmap.class#ic_launcher => 0x0000001
     */
    static final Map<String, Integer> mRInfoMap = new HashMap()

    static void clear() {
        mRInfoMap.clear()
    }

    static void readRMappings(File file) {
        if (!isRClass(file.absolutePath)) {
            return
        }

    }

    private static byte[] replaceRInfo(byte[] bytes) {
        def reader = new ClassReader(bytes)
        ClassWriter writer = new ClassWriter(0)
        def visitor = new ClassVisitor(Opcodes.ASM6, writer) {
            @Override
            MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                MethodVisitor methodVisitor = super.visitMethod(access, name, descriptor, signature, exceptions)
                methodVisitor = new MethodVisitor(Opcodes.ASM6, methodVisitor) {
                    @Override
                    void visitFieldInsn(int opcode, String owner, String name1, String descriptor1) {
                        def key = owner + name1
                        def value = mRInfoMap[key]
                        if (value == null) {
                            super.visitFieldInsn(opcode, owner, name1, descriptor1)
                        } else {
                            println "Replaced direct reference to R.class: ${owner}-${name1}"
                            super.visitLdcInsn(value)
                        }
                    }
                }
                return methodVisitor
            }
        }
        reader.accept(visitor, 0)
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
    private static String getFullClassName(String filePath) {
        println "src: ${filePath}"
        String mode = "/debug/"
        int idx = filePath.indexOf(mode)
        if (idx == -1) {
            mode = "release"
            idx = filePath.indexOf(mode)
        }
        if (idx != -1) {
            return filePath.substring(idx) - "${mode}"
        }
        idx = filePath.indexOf("/classes")
        filePath = filePath.substring(idx) - "/classes" - "/debug" - "/release"
        filePath = filePath.substring(1)
        println "tgt: ${filePath}"
        return filePath
    }

    /**
     * is R.class or its inner class like R$mipmap.class
     * @param classFilePath 形如 ../app/build/intermediates/classes/debug/me/bytebeats/agp/app/R.class
     * @return true if this class is R.class or its inner class like R$id.class
     */
    static boolean isRClass(String classFilePath) {
        return classFilePath ==~ '''.*/R\\$.*\\.class|.*/R\\.class'''
    }

    /**
     * is R.class or its inner class like R$drawable.class except R$styleable.class
     * {@see #isRClass(String)}
     * @param classFilePath
     * @return
     */
    static boolean isRClassExcludedStyleable(String classFilePath) {
        return classFilePath ==~ '''.*/R\\$(?!styleable).*?\\.class|.*/R\\.class'''
    }

    static void replaceAndDeleteRInfoFromFile(File file, InlineRExtension extension) {

    }

    static void replaceAndDeleteRInfoFromJar(String jarFile, InlineRExtension extension) {

    }
}