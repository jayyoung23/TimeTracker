package com.example.timetracker.ui

import android.app.TimePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.timetracker.R // 需要创建资源文件
import com.example.timetracker.TimeTrackerApplication
import com.example.timetracker.databinding.ActivityNewProjectBinding
import com.example.timetracker.viewmodel.ProjectViewModel
import com.example.timetracker.viewmodel.ProjectViewModelFactory
import java.text.SimpleDateFormat
import java.util.*

/**
 * 新建项目活动类
 * 允许用户输入项目名称、每日目标和提醒设置
 */
class NewProjectActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNewProjectBinding
    // 通过ViewModel工厂获取ViewModel实例
    private val projectViewModel: ProjectViewModel by viewModels {
        ProjectViewModelFactory((application as TimeTrackerApplication).repository)
    }

    // 用于存储选择的提醒时间（小时和分钟）
    private var reminderHour: Int? = null
    private var reminderMinute: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 使用ViewBinding初始化布局
        binding = ActivityNewProjectBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 设置ActionBar（如果需要返回按钮）
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.title_new_project)

        setupListeners()
    }

    /**
     * 设置UI元素的监听器
     */
    private fun setupListeners() {
        // 监听提醒开关的变化
        binding.switchReminder.setOnCheckedChangeListener { _, isChecked ->
            // 根据开关状态显示/隐藏提醒时间选择器
            binding.tvReminderTimeLabel.visibility = if (isChecked) View.VISIBLE else View.GONE
            binding.tvReminderTime.visibility = if (isChecked) View.VISIBLE else View.GONE
            if (!isChecked) {
                // 如果关闭提醒，清除已选时间
                reminderHour = null
                reminderMinute = null
                binding.tvReminderTime.text = ""
            }
        }

        // 监听提醒时间文本的点击事件，弹出时间选择器
        binding.tvReminderTime.setOnClickListener {
            showTimePickerDialog()
        }

        // 监听保存按钮的点击事件
        binding.btnSaveProject.setOnClickListener {
            saveProject()
        }
    }

    /**
     * 显示时间选择对话框
     */
    private fun showTimePickerDialog() {
        val calendar = Calendar.getInstance()
        val currentHour = reminderHour ?: calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = reminderMinute ?: calendar.get(Calendar.MINUTE)

        TimePickerDialog(
            this,
            { _, hourOfDay, minute ->
                // 当用户选择时间后更新变量和UI
                reminderHour = hourOfDay
                reminderMinute = minute
                updateReminderTimeTextView()
            },
            currentHour, currentMinute, false // 使用12小时制
        ).show()
    }

    /**
     * 更新提醒时间的TextView显示
     */
    private fun updateReminderTimeTextView() {
        if (reminderHour != null && reminderMinute != null) {
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, reminderHour!!)
                set(Calendar.MINUTE, reminderMinute!!)
            }
            // 格式化时间显示
            val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
            binding.tvReminderTime.text = timeFormat.format(calendar.time)
        }
    }

    /**
     * 保存项目信息
     */
    private fun saveProject() {
        // 获取用户输入
        val projectName = binding.etProjectName.text.toString().trim()
        val dailyTargetString = binding.etDailyTarget.text.toString().trim()
        val reminderEnabled = binding.switchReminder.isChecked

        // 验证输入
        if (projectName.isEmpty()) {
            binding.tilProjectName.error = getString(R.string.error_project_name_required)
            return
        } else {
            binding.tilProjectName.error = null
        }

        if (dailyTargetString.isEmpty()) {
            binding.tilDailyTarget.error = getString(R.string.error_daily_target_required)
            return
        } else {
            binding.tilDailyTarget.error = null
        }

        val dailyTargetMinutes = dailyTargetString.toIntOrNull()
        if (dailyTargetMinutes == null || dailyTargetMinutes <= 0) {
            binding.tilDailyTarget.error = getString(R.string.error_invalid_daily_target)
            return
        } else {
            binding.tilDailyTarget.error = null
        }

        var reminderTimeMinutes: Int? = null
        if (reminderEnabled) {
            if (reminderHour == null || reminderMinute == null) {
                // 如果开启了提醒但没有选择时间
                Toast.makeText(this, R.string.error_reminder_time_required, Toast.LENGTH_SHORT).show()
                return
            } else {
                // 将小时和分钟转换为一天中的分钟数
                reminderTimeMinutes = reminderHour!! * 60 + reminderMinute!!
            }
        }

        // 使用ViewModel保存项目
        projectViewModel.insertProject(
            name = projectName,
            dailyTargetMinutes = dailyTargetMinutes,
            reminderEnabled = reminderEnabled,
            reminderTime = reminderTimeMinutes
        )

        // 提示保存成功并关闭当前活动
        Toast.makeText(this, R.string.project_saved_success, Toast.LENGTH_SHORT).show()
        finish()
    }

    // 处理ActionBar返回按钮
    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
