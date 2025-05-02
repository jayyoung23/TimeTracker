package com.example.timetracker.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.timetracker.data.model.Project
import kotlinx.coroutines.flow.Flow

/**
 * 项目数据访问对象
 * 提供对项目表的访问方法
 */
@Dao
interface ProjectDao {
    /**
     * 插入新项目
     * 
     * @param project 要插入的项目对象
     * @return 插入的项目ID
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(project: Project): Long

    /**
     * 更新项目
     * 
     * @param project 要更新的项目对象
     */
    @Update
    suspend fun update(project: Project)

    /**
     * 删除项目
     * 
     * @param project 要删除的项目对象
     */
    @Delete
    suspend fun delete(project: Project)

    /**
     * 根据ID获取项目
     * 
     * @param id 项目ID
     * @return 项目对象的LiveData
     */
    @Query("SELECT * FROM projects WHERE id = :id")
    fun getProjectById(id: Long): LiveData<Project>

    /**
     * 获取所有项目
     * 
     * @return 包含所有项目的Flow
     */
    @Query("SELECT * FROM projects ORDER BY createdAt DESC")
    fun getAllProjects(): Flow<List<Project>>
    
    /**
     * 获取所有活跃项目
     * 
     * @return 包含所有活跃项目的Flow
     */
    @Query("SELECT * FROM projects WHERE isActive = 1 ORDER BY createdAt DESC")
    fun getActiveProjects(): Flow<List<Project>>
}
