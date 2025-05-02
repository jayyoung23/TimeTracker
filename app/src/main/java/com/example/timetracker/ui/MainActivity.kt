package com.example.timetracker.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.timetracker.R // 需要创建strings.xml等资源文件
import com.example.timetracker.TimeTrackerApplication
import com.example.timetracker.data.model.Project
import com.example.timetracker.databinding.ActivityMainBinding
import com.example.timetracker.viewmodel.ProjectViewModel
import com.example.timetracker.viewmodel.ProjectViewModelFactory
import java.util.*

/**
 * 主活动类
 * 显示项目列表，并允许用户添加新项目或查看项目详情
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    // 通过ViewModel工厂获取ViewModel实例
    private val projectViewModel: ProjectViewModel by viewModels {
        ProjectViewModelFactory((application as TimeTrackerApplication).repository)
    }
    private lateinit var adapter: ProjectListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 使用ViewBinding初始化布局
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 设置Toolbar
        setSupportActionBar(binding.toolbar)

        // 初始化RecyclerView适配器
        adapter = ProjectListAdapter(
            onItemClick = { project ->
                // 点击项目项，跳转到项目详情页
                val intent = Intent(this, ProjectDetailActivity::class.java).apply {
                    putExtra(ProjectDetailActivity.EXTRA_PROJECT_ID, project.id)
                }
                startActivity(intent)
            },
            onStartTrackingClick = { project ->
                // 点击开始/停止打卡按钮
                handleTrackingButtonClick(project)
            }
        )

        // 设置RecyclerView
        binding.projectsRecyclerView.adapter = adapter
        binding.projectsRecyclerView.layoutManager = LinearLayoutManager(this)

        // 观察活跃项目列表的变化
        projectViewModel.activeProjects.observe(this) { projects ->
            projects?.let {
                if (it.isEmpty()) {
                    // 如果列表为空，显示空视图提示
                    binding.projectsRecyclerView.visibility = View.GONE
                    binding.emptyView.visibility = View.VISIBLE
                } else {
                    // 如果列表不为空，显示RecyclerView，隐藏空视图
                    binding.projectsRecyclerView.visibility = View.VISIBLE
                    binding.emptyView.visibility = View.GONE
                    // 提交列表给适配器
                    adapter.submitList(it)
                    // 更新每个项目的状态和剩余时间（通过Adapter更新）
                    it.forEach { project ->
                        observeProjectUpdates(project)
                    }
                }
            }
        }

        // 设置添加项目按钮（FAB）的点击事件
        binding.fabAddProject.setOnClickListener {
            // 跳转到新建项目页面
            val intent = Intent(this, NewProjectActivity::class.java)
            startActivity(intent)
        }
    }

    /**
     * 观察单个项目的更新（状态和剩余时间）
     * @param project 要观察的项目
     */
    private fun observeProjectUpdates(project: Project) {
        // 观察今天日期，用于计算每日剩余时间
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time

        // 观察今日已打卡时长
        projectViewModel.getDailyDuration(project.id, today).observe(this) { dailyDurationSeconds ->
            val dailyDuration = dailyDurationSeconds ?: 0
            // 计算剩余时间（秒）
            val remainingSeconds = (project.dailyTargetMinutes * 60) - dailyDuration
            // 通过Adapter更新UI
            adapter.updateRemainingTime(project.id, remainingSeconds)
        }
    }
    
    /**
     * 处理打卡按钮点击事件
     * 确保只有一个项目可以同时打卡
     * 
     * @param project 点击的项目
     */
    private fun handleTrackingButtonClick(project: Project) {
        // 获取当前项目的打卡状态
        val isCurrentProjectTracking = adapter.getProjectStatus(project.id) ?: false
        
        if (isCurrentProjectTracking) {
            // 如果当前项目正在打卡，停止它
            projectViewModel.endTimeRecordByProjectId(project.id)
            Toast.makeText(this, getString(R.string.tracking_stopped), Toast.LENGTH_SHORT).show()
        } else {
            // 如果当前项目没有打卡，检查是否有其他项目正在打卡
            projectViewModel.currentlyTrackingRecord.observe(this, Observer { record ->
                if (record != null && record.projectId != project.id) {
                    // 有其他项目正在打卡，显示提示
                    projectViewModel.getProjectById(record.projectId).observe(this, Observer { trackingProject ->
                        val message = if (trackingProject != null) {
                            getString(R.string.already_tracking, trackingProject.name)
                        } else {
                            getString(R.string.another_project_tracking)
                        }
                        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                    })
                } else {
                    // 没有其他项目正在打卡，或者当前项目已经在打卡，开始打卡
                    startTracking(project)
                }
            })
        }
    }
    
    /**
     * 开始打卡并跳转到计时器页面
     * 
     * @param project 要打卡的项目
     */
    private fun startTracking(project: Project) {
        projectViewModel.startTimeRecord(project.id)
        Toast.makeText(this, getString(R.string.tracking_started), Toast.LENGTH_SHORT).show()
        
        // 跳转到计时器页面
        val intent = Intent(this, TimerActivity::class.java).apply {
            putExtra(TimerActivity.EXTRA_PROJECT_ID, project.id)
        }
        startActivity(intent)
    }
    
    override fun onResume() {
        super.onResume()
        // 在页面恢复时更新所有项目的状态
        observeTrackingStatus()
    }
    
    /**
     * 观察当前正在打卡的记录，更新UI状态
     */
    private fun observeTrackingStatus() {
        // 观察当前正在打卡的记录
        projectViewModel.currentlyTrackingRecord.observe(this, Observer { record ->
            // 重置所有项目的状态
            adapter.resetAllStatus()
            
            // 更新正在打卡的项目状态
            if (record != null) {
                adapter.updateStatus(record.projectId, true)
            }
        })
    }
}
