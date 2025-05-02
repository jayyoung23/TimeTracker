package com.example.timetracker.data.util

import androidx.room.TypeConverter
import java.util.*

/**
 * 日期转换器类
 * 用于在Room数据库中存储和读取Date类型
 */
class DateConverter {
    /**
     * 将时间戳转换为Date对象
     * 
     * @param value 时间戳（毫秒）
     * @return Date对象，如果时间戳为null则返回null
     */
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    /**
     * 将Date对象转换为时间戳
     * 
     * @param date Date对象
     * @return 时间戳（毫秒），如果Date为null则返回null
     */
    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
}
