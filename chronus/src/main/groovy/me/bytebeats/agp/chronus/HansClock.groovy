package me.bytebeats.agp.chronus

import org.gradle.internal.impldep.org.joda.time.DateTimeUtils

/**
 * Created by bytebeats on 2021/6/26 : 20:14
 * E-mail: happychinapc@gmail.com
 * Quote: Peasant. Educated. Worker
 */

/**
 * Hans clock
 */
class HansClock {
    private long start

    HansClock() {
        reset()
    }

    long timeInMS() {
        return System.currentTimeMillis() - start
    }

    static String format(long time) {
        StringBuilder builder = new StringBuilder()
        if (time > 24 * 60 * 60 * 1000) {
            def day = (time / (24 * 60 * 60 * 1000)).toLong()
            builder.append("${day}d")
            time %= (24 * 60 * 60 * 1000)
        }
        if (time > 60 * 60 * 1000) {
            def hour = (time / (60 * 60 * 1000)).toLong()
            builder.append("${hour}h")
            time %= (60 * 60 * 1000)
        }
        if (time > 60 * 1000) {
            def minute = (time / (60 * 1000)).toLong()
            builder.append("${minute}m")
            time %= (60 * 1000)
        }
        if (time > 1000) {
            def sec = (time / 1000).toLong()
            builder.append("${sec}s")
            time %= 1000
        }
        if (time > 0) {
            builder.append("${time}ms")
        }
        return builder.toString()
    }

    void reset() {
        start = System.currentTimeMillis()
    }
}
