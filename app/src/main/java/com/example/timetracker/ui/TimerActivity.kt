package com.example.timetracker.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.timetracker.R // 需要创建资源文件
import com.example.timetracker.TimeTrackerApplication
import com.example.timetracker.data.model.Project
import com.example.timetracker.data.model.TimeRecord
import com.example.timetracker.databinding.ActivityTimerBinding
import com.example.timetracker.util.formatDurationHMS // 需要创建工具类
import com.example.timetracker.viewmodel.ProjectViewModel
import com.example.timetracker.viewmodel.ProjectViewModelFactory
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * 计时器活动类
 * 显示正在进行的打卡会话的倒计时（基于每日目标）和已用时间
 */
class TimerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTimerBinding
    private var projectId: Long = -1
    private var currentProject: Project? = null
    private var ongoingRecord: TimeRecord? = null

    // 通过ViewModel工厂获取ViewModel实例
    private val projectViewModel: ProjectViewModel by viewModels {
        ProjectViewModelFactory((application as TimeTrackerApplication).repository)
    }

    private var dailyCountDownTimer: CountDownTimer? = null
    private var elapsedTimeHandler: Handler? = null
    private var elapsedTimeRunnable: Runnable? = null

    companion object {
        const val EXTRA_PROJECT_ID = "timer_project_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 使用ViewBinding初始化布局
        binding = ActivityTimerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 获取传递过来的项目ID
        projectId = intent.getLongExtra(EXTRA_PROJECT_ID, -1)

        // 检查项目ID是否有效
        if (projectId == -1L) {
            Toast.makeText(this, R.string.error_invalid_project_id, Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // 设置返回按钮（可选）
        // supportActionBar?.setDisplayHomeAsUpEnabled(true)

        observeProjectAndRecord()
        setupStopButton()
    }

    /**
     * 观察项目详情和正在进行的记录
     */
    private fun observeProjectAndRecord() {
        // 获取项目详情
        projectViewModel.getProjectById(projectId).observe(this) { project ->
            if (project != null) {
                currentProject = project
                binding.tvTimerProjectName.text = project.name
                // 获取到项目信息后，再获取正在进行的记录并开始计时器
                fetchOngoingRecordAndStartTimers()
            } else {
                // 项目不存在，可能已被删除
                Toast.makeText(this, R.string.error_project_not_found, Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    /**
     * 获取当前项目正在进行的打卡记录，并启动计时器
     */
    private fun fetchOngoingRecordAndStartTimers() {
        projectViewModel.getOngoingTimeRecord(projectId).observe(this) { record ->
            if (record != null) {
                ongoingRecord = record
                // 开始或更新计时器
                startTimers(currentProject!!, record)
            } else {
                // 没有找到正在进行的记录，可能已被停止
                // 这种情况理论上不应发生，因为是从MainActivity启动的
                 Toast.makeText(this, R.string.error_no_ongoing_record, Toast.LENGTH_SHORT).show()
                finish() // 关闭计时器页面
            }
        }
    }

    /**
     * 启动每日目标倒计时和已用时间计时器
     * @param project 当前项目
     * @param record 当前正在进行的记录
     */
    private fun startTimers(project: Project, record: TimeRecord) {
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time

        // 获取今天已经打卡的总时长（不包括当前正在进行的这次）
        projectViewModel.getTimeRecordsByDateRange(projectId, today, Date()).observe(this) { records ->
            val completedDurationTodaySeconds = records
                ?.filter { it.id != record.id && it.endTime != null } // 过滤掉当前记录和未完成的
                ?.sumOf { it.durationSeconds } ?: 0

            // --- 启动每日目标倒计时 --- 
            startDailyCountdown(project.dailyTargetMinutes, completedDurationTodaySeconds, record.startTime)

            // --- 启动当前会话已用时间计时器 ---
            startElapsedTimeCounter(record.startTime)
        }
    }

    /**
     * 启动每日目标的倒计时器
     * @param dailyTargetMinutes 每日目标分钟数
     * @param completedDurationTodaySeconds 今天已完成的时长（秒）
     * @param currentSessionStartTime 当前会话开始时间
     */
    private fun startDailyCountdown(dailyTargetMinutes: Int, completedDurationTodaySeconds: Int, currentSessionStartTime: Date) {
        dailyCountDownTimer?.cancel() // 取消之前的倒计时

        val targetTotalSeconds = dailyTargetMinutes * 60L
        val now = Date().time
        val currentSessionElapsedSeconds = (now - currentSessionStartTime.time) / 1000
        val totalElapsedTodaySeconds = completedDurationTodaySeconds + currentSessionElapsedSeconds
        val remainingSeconds = targetTotalSeconds - totalElapsedTodaySeconds

        if (remainingSeconds <= 0) {
            // 如果目标已达成或超出，显示00:00:00
            binding.tvTimerCountdown.text = formatDurationHMS(0L)
            return
        }

        dailyCountDownTimer = object : CountDownTimer(remainingSeconds * 1000, 1000) {
            @SuppressLint("StringFormatInvalid", "StringFormatMatches") // R.string...需要定义
            override fun onTick(millisUntilFinished: Long) {
                // 每秒更新倒计时显示
                binding.tvTimerCountdown.text = formatDurationHMS(millisUntilFinished / 1000)
            }

            @SuppressLint("StringFormatInvalid", "StringFormatMatches") // R.string...需要定义
            override fun onFinish() {
                // 倒计时结束
                binding.tvTimerCountdown.text = formatDurationHMS(0L)
                // 可以在这里添加提醒，例如震动或声音
                Toast.makeText(this@TimerActivity, R.string.daily_target_reached, Toast.LENGTH_SHORT).show()
            }
        }.start()
    }

    /**
     * 启动当前会话已用时间计时器
     * @param startTime 当前会话的开始时间
     */
    private fun startElapsedTimeCounter(startTime: Date) {
        elapsedTimeHandler?.removeCallbacks(elapsedTimeRunnable!!) // 停止之前的计时
        elapsedTimeHandler = Handler(Looper.getMainLooper())

        elapsedTimeRunnable = object : Runnable {
            @SuppressLint("StringFormatInvalid", "StringFormatMatches") // R.string...需要定义
            override fun run() {
                val elapsedMillis = Date().time - startTime.time
                // 更新已用时间显示
                binding.tvElapsedTime.text = formatDurationHMS(elapsedMillis / 1000)
                // 每秒钟更新一次
                elapsedTimeHandler?.postDelayed(this, 1000)
            }
        }
        // 立即开始计时
        elapsedTimeHandler?.post(elapsedTimeRunnable!!)
    }

    /**
     * 设置停止按钮的点击事件
     */
    private fun setupStopButton() {
        binding.btnStopTimer.setOnClickListener {
            stopTrackingAndFinish()
        }
    }

    /**
     * 停止打卡并结束活动
     */
    private fun stopTrackingAndFinish() {
        dailyCountDownTimer?.cancel()
        elapsedTimeHandler?.removeCallbacks(elapsedTimeRunnable!!)

        if (ongoingRecord != null) {
            // 调用ViewModel结束时间记录
            projectViewModel.endTimeRecord(ongoingRecord!!.id)
            Toast.makeText(this, R.string.tracking_stopped, Toast.LENGTH_SHORT).show()
        } else {
            // 如果没有正在进行的记录（理论上不应发生）
            Toast.makeText(this, R.string.error_no_ongoing_record_to_stop, Toast.LENGTH_SHORT).show()
        }
        // 结束当前活动
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        // 清理计时器和Handler，防止内存泄漏
        dailyCountDownTimer?.cancel()
        elapsedTimeHandler?.removeCallbacks(elapsedTimeRunnable!!)
    }

    // 处理返回按钮（如果启用ActionBar）
    // override fun onSupportNavigateUp(): Boolean {
    //     onBackPressedDispatcher.onBackPressed() // 默认行为是返回
    //     // 或者调用 stopTrackingAndFinish() 如果希望返回也停止计时
    //     return true
    // }

    // 处理物理返回按钮
    override fun onBackPressed() {
        // 决定返回按钮的行为：是直接返回还是停止计时再返回
        // 当前实现：停止计时再返回
        stopTrackingAndFinish()
        super.onBackPressed()
    }
}
