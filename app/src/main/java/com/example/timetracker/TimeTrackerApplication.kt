package com.example.timetracker

import android.app.Application
import com.example.timetracker.data.AppDatabase
import com.example.timetracker.repository.ProjectRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

/**
 * 应用程序类
 * 负责初始化应用程序级别的组件，如数据库和仓库
 */
class TimeTrackerApplication : Application() {
    // 使用 SupervisorJob 创建应用程序级别的协程作用域
    private val applicationScope = CoroutineScope(SupervisorJob())
    
    // 懒加载数据库实例
    val database by lazy { AppDatabase.getDatabase(this, applicationScope) }
    
    // 懒加载项目仓库实例
    val repository by lazy { ProjectRepository(database.projectDao(), database.timeRecordDao()) }
    
    override fun onCreate() {
        super.onCreate()
        // 应用程序初始化代码可以放在这里
    }
}
