package com.example.timetracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.timetracker.data.dao.ProjectDao
import com.example.timetracker.data.dao.TimeRecordDao
import com.example.timetracker.data.model.Project
import com.example.timetracker.data.model.TimeRecord
import com.example.timetracker.data.util.DateConverter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * 应用数据库类
 * 使用Room持久化库管理SQLite数据库
 */
@Database(entities = [Project::class, TimeRecord::class], version = 1, exportSchema = false)
@TypeConverters(DateConverter::class)
abstract class AppDatabase : RoomDatabase() {

    // 项目数据访问对象
    abstract fun projectDao(): ProjectDao
    
    // 时间记录数据访问对象
    abstract fun timeRecordDao(): TimeRecordDao

    // 数据库回调，用于初始化数据库
    private class AppDatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {

        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            // 数据库创建时的回调，可以在这里添加初始数据
            INSTANCE?.let { database ->
                scope.launch {
                    // 如果需要，可以在这里添加一些示例项目
                }
            }
        }
    }

    companion object {
        // 单例模式，防止同时打开多个数据库实例
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * 获取数据库实例
         * 如果实例不存在则创建新实例
         * 
         * @param context 应用上下文
         * @param scope 协程作用域，用于数据库操作
         * @return 数据库实例
         */
        fun getDatabase(
            context: Context,
            scope: CoroutineScope
        ): AppDatabase {
            // 如果INSTANCE不为null，则直接返回
            // 如果为null，则在synchronized块中创建数据库实例
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "time_tracker_database"
                )
                    // 添加数据库回调
                    .addCallback(AppDatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                // 返回实例
                instance
            }
        }
    }
}
