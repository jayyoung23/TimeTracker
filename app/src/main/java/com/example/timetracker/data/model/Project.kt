package com.example.timetracker.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

/**
 * 项目实体类
 * 用于存储项目的基本信息
 * 
 * @property id 项目唯一标识符
 * @property name 项目名称
 * @property dailyTargetMinutes 每日计划时间（分钟）
 * @property reminderEnabled 是否开启提醒
 * @property reminderTime 提醒时间（如果启用提醒）
 * @property createdAt 项目创建时间
 * @property isActive 项目是否处于活跃状态
 */
@Entity(tableName = "projects")
data class Project(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    // 项目名称
    val name: String,
    
    // 每日计划时间（分钟）
    val dailyTargetMinutes: Int,
    
    // 是否开启提醒
    val reminderEnabled: Boolean = false,
    
    // 提醒时间（如果启用提醒）- 存储为一天中的分钟数（0-1439）
    val reminderTime: Int? = null,
    
    // 项目创建时间
    val createdAt: Date = Date(),
    
    // 项目是否处于活跃状态
    val isActive: Boolean = true
)
