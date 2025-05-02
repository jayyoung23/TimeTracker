package com.example.timetracker.repository

import androidx.lifecycle.LiveData
import com.example.timetracker.data.dao.ProjectDao
import com.example.timetracker.data.dao.TimeRecordDao
import com.example.timetracker.data.model.Project
import com.example.timetracker.data.model.TimeRecord
import kotlinx.coroutines.flow.Flow
import java.util.*
import kotlin.math.roundToInt

/**
 * 项目仓库类
 * 作为数据源和ViewModel之间的中介，处理所有数据操作
 * 
 * @param projectDao 项目数据访问对象
 * @param timeRecordDao 时间记录数据访问对象
 */
class ProjectRepository(
    private val projectDao: ProjectDao,
    private val timeRecordDao: TimeRecordDao
) {
    // 获取所有项目
    val allProjects: Flow<List<Project>> = projectDao.getAllProjects()
    
    // 获取所有活跃项目
    val activeProjects: Flow<List<Project>> = projectDao.getActiveProjects()
    
    /**
     * 插入新项目
     * 
     * @param project 要插入的项目
     * @return 插入的项目ID
     */
    suspend fun insertProject(project: Project): Long {
        return projectDao.insert(project)
    }
    
    /**
     * 更新项目
     * 
     * @param project 要更新的项目
     */
    suspend fun updateProject(project: Project) {
        projectDao.update(project)
    }
    
    /**
     * 删除项目
     * 
     * @param project 要删除的项目
     */
    suspend fun deleteProject(project: Project) {
        projectDao.delete(project)
    }
    
    /**
     * 根据ID获取项目
     * 
     * @param id 项目ID
     * @return 项目的LiveData
     */
    fun getProjectById(id: Long): LiveData<Project> {
        return projectDao.getProjectById(id)
    }
    
    /**
     * 插入时间记录
     * 
     * @param timeRecord 要插入的时间记录
     * @return 插入的记录ID
     */
    suspend fun insertTimeRecord(timeRecord: TimeRecord): Long {
        return timeRecordDao.insert(timeRecord)
    }
    
    /**
     * 更新时间记录
     * 
     * @param timeRecord 要更新的时间记录
     */
    suspend fun updateTimeRecord(timeRecord: TimeRecord) {
        timeRecordDao.update(timeRecord)
    }
    
    /**
     * 获取指定项目的所有时间记录
     * 
     * @param projectId 项目ID
     * @return 项目的所有时间记录Flow
     */
    fun getTimeRecordsByProject(projectId: Long): Flow<List<TimeRecord>> {
        return timeRecordDao.getTimeRecordsByProject(projectId)
    }
    
    /**
     * 获取指定项目在指定日期范围内的时间记录
     * 
     * @param projectId 项目ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 日期范围内的时间记录Flow
     */
    fun getTimeRecordsByProjectAndDateRange(projectId: Long, startDate: Date, endDate: Date): Flow<List<TimeRecord>> {
        return timeRecordDao.getTimeRecordsByProjectAndDateRange(projectId, startDate, endDate)
    }
    
    /**
     * 获取项目的总时长（秒）
     * 
     * @param projectId 项目ID
     * @return 总时长（秒）
     */
    suspend fun getTotalDurationByProject(projectId: Long): Int {
        return timeRecordDao.getTotalDurationByProject(projectId) ?: 0
    }
    
    /**
     * 获取项目在指定日期的总时长（秒）
     * 
     * @param projectId 项目ID
     * @param date 日期
     * @return 指定日期的总时长（秒）
     */
    suspend fun getDailyDurationByProject(projectId: Long, date: Date): Int {
        return timeRecordDao.getDailyDurationByProject(projectId, date) ?: 0
    }
    
    /**
     * 获取项目的打卡天数
     * 
     * @param projectId 项目ID
     * @return 打卡天数
     */
    suspend fun getCheckInDaysByProject(projectId: Long): Int {
        return timeRecordDao.getCheckInDaysByProject(projectId)
    }
    
    /**
     * 获取项目的日均时长（分钟）
     * 
     * @param projectId 项目ID
     * @return 日均时长（分钟）
     */
    suspend fun getAverageDailyDuration(projectId: Long): Int {
        val totalDuration = getTotalDurationByProject(projectId)
        val days = getCheckInDaysByProject(projectId)
        
        return if (days > 0) {
            (totalDuration / 60.0 / days).roundToInt()
        } else {
            0
        }
    }
    
    /**
     * 开始一个新的时间记录
     * 
     * @param projectId 项目ID
     * @return 新记录的ID
     */
    suspend fun startTimeRecord(projectId: Long): Long {
        // 检查是否有正在进行的记录
        val ongoingRecord = timeRecordDao.getOngoingTimeRecord(projectId)
        
        // 如果有正在进行的记录，先结束它
        if (ongoingRecord != null) {
            endTimeRecord(ongoingRecord.id)
        }
        
        // 创建新的时间记录
        val timeRecord = TimeRecord(
            projectId = projectId,
            startTime = Date(),
            date = Date()
        )
        
        return insertTimeRecord(timeRecord)
    }
    
    /**
     * 结束一个时间记录
     * 
     * @param recordId 记录ID
     */
    suspend fun endTimeRecord(recordId: Long) {
        val record = timeRecordDao.getTimeRecordById(recordId).value ?: return
        
        // 如果记录已经结束，不做任何操作
        if (record.endTime != null) return
        
        // 计算持续时间
        val endTime = Date()
        val durationSeconds = ((endTime.time - record.startTime.time) / 1000).toInt()
        
        // 更新记录
        val updatedRecord = record.copy(
            endTime = endTime,
            durationSeconds = durationSeconds
        )
        
        updateTimeRecord(updatedRecord)
    }
    
    /**
     * 获取正在进行中的时间记录
     * 
     * @param projectId 项目ID
     * @return 正在进行中的时间记录，如果没有则返回null
     */
    suspend fun getOngoingTimeRecord(projectId: Long): TimeRecord? {
        return timeRecordDao.getOngoingTimeRecord(projectId)
    }
    
    /**
     * 通过ID获取时间记录
     * 
     * @param recordId 记录ID
     * @return 时间记录，如果不存在则返回null
     */
    suspend fun getTimeRecordById(recordId: Long): TimeRecord? {
        return timeRecordDao.getTimeRecordById(recordId).value
    }
    
    /**
     * 获取任意一个正在进行中的时间记录
     * 
     * @return 任意一个正在进行中的时间记录，如果没有则返回null
     */
    suspend fun getAnyOngoingTimeRecord(): TimeRecord? {
        return timeRecordDao.getAnyOngoingTimeRecord()
    }
}
