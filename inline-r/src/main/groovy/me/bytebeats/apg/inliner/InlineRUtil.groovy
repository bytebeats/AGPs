package me.bytebeats.apg.inliner


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

    static boolean isRClass(String classFilePath) {
        return classFilePath ==~ '''.*/R\\$(?!styleable).*?\\.class|.*/R\\.class'''
    }
}