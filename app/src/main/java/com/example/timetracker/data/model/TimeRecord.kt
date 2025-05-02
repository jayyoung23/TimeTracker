package com.example.timetracker.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.*

/**
 * 时间记录实体类
 * 用于记录用户的打卡时间
 * 
 * @property id 记录唯一标识符
 * @property projectId 关联的项目ID
 * @property startTime 开始时间
 * @property endTime 结束时间（如果为null表示正在进行中）
 * @property durationSeconds 持续时间（秒）
 * @property date 记录日期（用于按日期统计）
 */
@Entity(
    tableName = "time_records",
    foreignKeys = [
        ForeignKey(
            entity = Project::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("projectId")]
)
data class TimeRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    // 关联的项目ID
    val projectId: Long,
    
    // 开始时间
    val startTime: Date,
    
    // 结束时间（如果为null表示正在进行中）
    val endTime: Date? = null,
    
    // 持续时间（秒）
    val durationSeconds: Int = 0,
    
    // 记录日期（用于按日期统计）
    val date: Date = Date()
)
