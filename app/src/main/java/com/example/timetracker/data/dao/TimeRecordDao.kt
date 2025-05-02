package com.example.timetracker.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.timetracker.data.model.TimeRecord
import kotlinx.coroutines.flow.Flow
import java.util.*

/**
 * 时间记录数据访问对象
 * 提供对时间记录表的访问方法
 */
@Dao
interface TimeRecordDao {
    /**
     * 插入新的时间记录
     * 
     * @param timeRecord 要插入的时间记录对象
     * @return 插入的记录ID
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(timeRecord: TimeRecord): Long

    /**
     * 更新时间记录
     * 
     * @param timeRecord 要更新的时间记录对象
     */
    @Update
    suspend fun update(timeRecord: TimeRecord)

    /**
     * 删除时间记录
     * 
     * @param timeRecord 要删除的时间记录对象
     */
    @Delete
    suspend fun delete(timeRecord: TimeRecord)

    /**
     * 根据ID获取时间记录
     * 
     * @param id 时间记录ID
     * @return 时间记录对象的LiveData
     */
    @Query("SELECT * FROM time_records WHERE id = :id")
    fun getTimeRecordById(id: Long): LiveData<TimeRecord>

    /**
     * 获取指定项目的所有时间记录
     * 
     * @param projectId 项目ID
     * @return 包含指定项目所有时间记录的Flow
     */
    @Query("SELECT * FROM time_records WHERE projectId = :projectId ORDER BY startTime DESC")
    fun getTimeRecordsByProject(projectId: Long): Flow<List<TimeRecord>>
    
    /**
     * 获取指定项目在指定日期的时间记录
     * 
     * @param projectId 项目ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 包含指定项目在指定日期范围内所有时间记录的Flow
     */
    @Query("SELECT * FROM time_records WHERE projectId = :projectId AND date BETWEEN :startDate AND :endDate ORDER BY startTime DESC")
    fun getTimeRecordsByProjectAndDateRange(projectId: Long, startDate: Date, endDate: Date): Flow<List<TimeRecord>>
    
    /**
     * 获取指定项目的总时长（秒）
     * 
     * @param projectId 项目ID
     * @return 总时长（秒）
     */
    @Query("SELECT SUM(durationSeconds) FROM time_records WHERE projectId = :projectId")
    suspend fun getTotalDurationByProject(projectId: Long): Int?
    
    /**
     * 获取指定项目在指定日期的总时长（秒）
     * 
     * @param projectId 项目ID
     * @param date 日期
     * @return 指定日期的总时长（秒）
     */
    @Query("SELECT SUM(durationSeconds) FROM time_records WHERE projectId = :projectId AND date = :date")
    suspend fun getDailyDurationByProject(projectId: Long, date: Date): Int?
    
    /**
     * 获取指定项目的打卡天数
     * 
     * @param projectId 项目ID
     * @return 打卡天数
     */
    @Query("SELECT COUNT(DISTINCT date) FROM time_records WHERE projectId = :projectId")
    suspend fun getCheckInDaysByProject(projectId: Long): Int
    
    /**
     * 获取指定项目的最近一条进行中的时间记录
     * 
     * @param projectId 项目ID
     * @return 最近一条进行中的时间记录
     */
    @Query("SELECT * FROM time_records WHERE projectId = :projectId AND endTime IS NULL ORDER BY startTime DESC LIMIT 1")
    suspend fun getOngoingTimeRecord(projectId: Long): TimeRecord?
    
    /**
     * 获取任意一个正在进行中的时间记录
     * 
     * @return 任意一个正在进行中的时间记录，如果没有则返回null
     */
    @Query("SELECT * FROM time_records WHERE endTime IS NULL ORDER BY startTime DESC LIMIT 1")
    suspend fun getAnyOngoingTimeRecord(): TimeRecord?
}
