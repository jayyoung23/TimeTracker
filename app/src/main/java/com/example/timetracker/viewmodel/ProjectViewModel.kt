package com.example.timetracker.viewmodel

import androidx.lifecycle.*
import com.example.timetracker.data.model.Project
import com.example.timetracker.data.model.TimeRecord
import com.example.timetracker.repository.ProjectRepository
import kotlinx.coroutines.launch
import java.util.*

/**
 * 项目ViewModel类
 * 作为UI和数据层之间的中介，处理UI相关的数据逻辑
 * 
 * @param repository 项目仓库
 */
class ProjectViewModel(private val repository: ProjectRepository) : ViewModel() {
    
    // 当前正在追踪的记录
    private val _currentlyTrackingRecord = MutableLiveData<TimeRecord?>()
    val currentlyTrackingRecord: LiveData<TimeRecord?> = _currentlyTrackingRecord
    
    // 初始化时加载当前正在追踪的记录
    init {
        viewModelScope.launch {
            _currentlyTrackingRecord.value = repository.getAnyOngoingTimeRecord()
        }
    }
    
    // 所有项目的LiveData
    val allProjects = repository.allProjects.asLiveData()
    
    // 活跃项目的LiveData
    val activeProjects = repository.activeProjects.asLiveData()
    
    /**
     * 插入新项目
     * 
     * @param name 项目名称
     * @param dailyTargetMinutes 每日目标时间（分钟）
     * @param reminderEnabled 是否启用提醒
     * @param reminderTime 提醒时间（如果启用提醒）
     */
    fun insertProject(
        name: String,
        dailyTargetMinutes: Int,
        reminderEnabled: Boolean = false,
        reminderTime: Int? = null
    ) = viewModelScope.launch {
        val project = Project(
            name = name,
            dailyTargetMinutes = dailyTargetMinutes,
            reminderEnabled = reminderEnabled,
            reminderTime = reminderTime
        )
        repository.insertProject(project)
    }
    
    /**
     * 更新项目
     * 
     * @param project 要更新的项目
     */
    fun updateProject(project: Project) = viewModelScope.launch {
        repository.updateProject(project)
    }
    
    /**
     * 删除项目
     * 
     * @param project 要删除的项目
     */
    fun deleteProject(project: Project) = viewModelScope.launch {
        repository.deleteProject(project)
    }
    
    /**
     * 根据ID获取项目
     * 
     * @param id 项目ID
     * @return 项目的LiveData
     */
    fun getProjectById(id: Long): LiveData<Project> {
        return repository.getProjectById(id)
    }
    
    /**
     * 获取项目的时间记录
     * 
     * @param projectId 项目ID
     * @return 项目的时间记录LiveData
     */
    fun getTimeRecordsByProject(projectId: Long): LiveData<List<TimeRecord>> {
        return repository.getTimeRecordsByProject(projectId).asLiveData()
    }
    
    /**
     * 获取项目在指定日期范围内的时间记录
     * 
     * @param projectId 项目ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 日期范围内的时间记录LiveData
     */
    fun getTimeRecordsByDateRange(projectId: Long, startDate: Date, endDate: Date): LiveData<List<TimeRecord>> {
        return repository.getTimeRecordsByProjectAndDateRange(projectId, startDate, endDate).asLiveData()
    }
    
    /**
     * 开始时间记录
     * 
     * @param projectId 项目ID
     * @return 新记录的ID
     */
    fun startTimeRecord(projectId: Long) = viewModelScope.launch {
        val recordId = repository.startTimeRecord(projectId)
        // 获取新创建的记录并更新当前追踪记录
        val newRecord = repository.getTimeRecordById(recordId)
        _currentlyTrackingRecord.value = newRecord
    }
    
    /**
     * 结束时间记录
     * 
     * @param recordId 记录ID
     */
    fun endTimeRecord(recordId: Long) = viewModelScope.launch {
        repository.endTimeRecord(recordId)
        // 如果结束的是当前正在追踪的记录，则清空当前记录
        if (_currentlyTrackingRecord.value?.id == recordId) {
            _currentlyTrackingRecord.value = null
        }
    }
    
    /**
     * 通过项目ID结束时间记录
     * 
     * @param projectId 项目ID
     */
    fun endTimeRecordByProjectId(projectId: Long) = viewModelScope.launch {
        val record = repository.getOngoingTimeRecord(projectId)
        if (record != null) {
            repository.endTimeRecord(record.id)
            // 如果结束的是当前正在追踪的记录，则清空当前记录
            if (_currentlyTrackingRecord.value?.id == record.id) {
                _currentlyTrackingRecord.value = null
            }
        }
    }
    
    /**
     * 获取项目的总时长（秒）
     * 
     * @param projectId 项目ID
     * @return 总时长的LiveData
     */
    fun getTotalDuration(projectId: Long): LiveData<Int> = liveData {
        emit(repository.getTotalDurationByProject(projectId))
    }
    
    /**
     * 获取项目在指定日期的总时长（秒）
     * 
     * @param projectId 项目ID
     * @param date 日期
     * @return 指定日期的总时长LiveData
     */
    fun getDailyDuration(projectId: Long, date: Date): LiveData<Int> = liveData {
        emit(repository.getDailyDurationByProject(projectId, date))
    }
    
    /**
     * 获取项目的打卡天数
     * 
     * @param projectId 项目ID
     * @return 打卡天数的LiveData
     */
    fun getCheckInDays(projectId: Long): LiveData<Int> = liveData {
        emit(repository.getCheckInDaysByProject(projectId))
    }
    
    /**
     * 获取项目的日均时长（分钟）
     * 
     * @param projectId 项目ID
     * @return 日均时长的LiveData
     */
    fun getAverageDailyDuration(projectId: Long): LiveData<Int> = liveData {
        emit(repository.getAverageDailyDuration(projectId))
    }
    
    /**
     * 获取正在进行中的时间记录
     * 
     * @param projectId 项目ID
     * @return 正在进行中的时间记录LiveData
     */
    fun getOngoingTimeRecord(projectId: Long): LiveData<TimeRecord?> = liveData {
        emit(repository.getOngoingTimeRecord(projectId))
    }
    
    /**
     * 检查项目是否有正在进行的时间记录
     * 
     * @param projectId 项目ID
     * @return 是否有正在进行的时间记录的LiveData
     */
    fun hasOngoingTimeRecord(projectId: Long): LiveData<Boolean> = liveData {
        val record = repository.getOngoingTimeRecord(projectId)
        emit(record != null)
    }
}

/**
 * 项目ViewModel工厂类
 * 用于创建ProjectViewModel实例
 * 
 * @param repository 项目仓库
 */
class ProjectViewModelFactory(private val repository: ProjectRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProjectViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProjectViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
