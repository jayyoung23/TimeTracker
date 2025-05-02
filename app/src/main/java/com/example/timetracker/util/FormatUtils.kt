package com.example.timetracker.util

import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * 格式化时间长度的工具函数
 * 将秒数转换为 HH:mm:ss 或 Xh Ym Zs 格式
 *
 * @param seconds 总秒数
 * @param includeHoursAlways 是否始终包含小时部分（即使为0），例如 "00:15:30"
 * @param useWords 是否使用文字表示单位（例如 "1h 30m 5s"）而不是冒号分隔
 * @return 格式化后的时间字符串
 */
fun formatDurationHMS(seconds: Long, includeHoursAlways: Boolean = false, useWords: Boolean = false): String {
    if (seconds < 0) return "00:00:00" // 处理负数

    val hours = TimeUnit.SECONDS.toHours(seconds)
    val minutes = TimeUnit.SECONDS.toMinutes(seconds) % 60
    val secs = seconds % 60

    return if (useWords) {
        // 使用文字表示单位 (e.g., 1h 30m 5s)
        val parts = mutableListOf<String>()
        if (hours > 0 || includeHoursAlways) {
            parts.add("${hours}h")
        }
        if (minutes > 0 || (hours > 0 && includeHoursAlways) || (includeHoursAlways && parts.isEmpty())) {
            // 仅当小时>0 或 总是包含小时 或 还没有任何部分时才添加分钟
             parts.add("${minutes}m")
        }
        // 总是添加秒，除非总时间为0且不强制包含小时
        if (secs > 0 || seconds == 0L || (includeHoursAlways && parts.size < 2) ) {
             parts.add("${secs}s")
        }
        // 如果秒数为0且已有其他部分，则不显示秒
        if(secs == 0L && parts.size > 1 && !(includeHoursAlways && parts.size < 2)){
            parts.removeLast()
        }
         if(parts.isEmpty() && seconds == 0L) return "0s" // 处理0秒的情况
         parts.joinToString(" ")

    } else {
        // 使用 HH:mm:ss 格式
        if (hours > 0 || includeHoursAlways) {
            String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, secs)
        } else {
            // 如果不强制包含小时且小时为0，则只显示 mm:ss
            String.format(Locale.getDefault(), "%02d:%02d", minutes, secs)
        }
    }
}
