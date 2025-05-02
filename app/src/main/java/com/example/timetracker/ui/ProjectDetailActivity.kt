package com.example.timetracker.ui

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.timetracker.R // 需要创建资源文件
import com.example.timetracker.TimeTrackerApplication
import com.example.timetracker.data.model.TimeRecord
import com.example.timetracker.databinding.ActivityProjectDetailBinding
import com.example.timetracker.util.formatDurationHMS // 需要创建工具类
import com.example.timetracker.viewmodel.ProjectViewModel
import com.example.timetracker.viewmodel.ProjectViewModelFactory
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

/**
 * 项目详情活动类
 * 显示项目的统计数据和图表
 */
class ProjectDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProjectDetailBinding
    // 通过ViewModel工厂获取ViewModel实例
    private val projectViewModel: ProjectViewModel by viewModels {
        ProjectViewModelFactory((application as TimeTrackerApplication).repository)
    }
    private var projectId: Long = -1
    private val calendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val monthFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
    private val yearFormat = SimpleDateFormat("yyyy", Locale.getDefault())

    companion object {
        const val EXTRA_PROJECT_ID = "project_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 使用ViewBinding初始化布局
        binding = ActivityProjectDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 获取传递过来的项目ID
        projectId = intent.getLongExtra(EXTRA_PROJECT_ID, -1)
        if (projectId == -1L) {
            // 如果项目ID无效，则结束活动
            finish()
            return
        }

        // 设置Toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        setupSpinner()
        observeProjectDetails()
        observeStatistics()
        setupCharts()
    }

    /**
     * 设置时间范围选择Spinner
     */
    private fun setupSpinner() {
        val adapter = ArrayAdapter.createFromResource(
            this,
            R.array.time_range_options, // 需要在 strings.xml 中定义
            android.R.layout.simple_spinner_item
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerTimeRange.adapter = adapter

        binding.spinnerTimeRange.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                // 当选择的时间范围变化时，重新加载图表数据
                loadChartData(position)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        // 默认选择“周”
        binding.spinnerTimeRange.setSelection(1) // 0:天, 1:周, 2:月, 3:季度, 4:年
    }

    /**
     * 观察项目基本信息的变化（例如标题）
     */
    private fun observeProjectDetails() {
        projectViewModel.getProjectById(projectId).observe(this) { project ->
            project?.let {
                supportActionBar?.title = it.name
            }
        }
    }

    /**
     * 观察并更新统计数据
     */
    @SuppressLint("StringFormatInvalid", "StringFormatMatches") // R.string...需要定义
    private fun observeStatistics() {
        // 观察总时长
        projectViewModel.getTotalDuration(projectId).observe(this) { totalSeconds ->
            binding.tvTotalDuration.text = getString(R.string.total_duration_format,
                formatDurationHMS(totalSeconds?.toLong() ?: 0L))
        }

        // 观察累计天数（基于项目创建时间计算，或直接从记录计算）
        // 这里使用打卡天数作为累计天数的近似值，更精确的计算需要考虑项目创建日期
        projectViewModel.getCheckInDays(projectId).observe(this) { days ->
             binding.tvTotalDays.text = getString(R.string.total_days_format, days ?: 0)
        }

        // 观察日均时长
        projectViewModel.getAverageDailyDuration(projectId).observe(this) { avgMinutes ->
             binding.tvAvgDailyDuration.text = getString(R.string.avg_daily_duration_format,
                formatDurationHMS((avgMinutes?.toLong() ?: 0L) * 60))
        }

        // 观察打卡天数
        projectViewModel.getCheckInDays(projectId).observe(this) { checkInDays ->
            binding.tvCheckInDays.text = getString(R.string.check_in_days_format, checkInDays ?: 0)
        }
    }

    /**
     * 初始化图表设置
     */
    private fun setupCharts() {
        setupBarChart(binding.barChart)
        setupPieChart(binding.pieChart)
    }

    /**
     * 设置柱状图的基本样式
     */
    private fun setupBarChart(barChart: BarChart) {
        barChart.description.isEnabled = false
        barChart.setDrawGridBackground(false)
        barChart.setDrawBarShadow(false)
        barChart.legend.isEnabled = false // 隐藏图例
        barChart.animateY(1000)

        val xAxis = barChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.granularity = 1f // 设置最小间隔
        // xAxis.labelRotationAngle = -45f // 如果标签过长可以旋转

        val leftAxis = barChart.axisLeft
        leftAxis.setDrawGridLines(true)
        leftAxis.axisMinimum = 0f // Y轴最小值

        barChart.axisRight.isEnabled = false // 禁用右侧Y轴
    }

    /**
     * 设置饼图的基本样式
     */
    private fun setupPieChart(pieChart: PieChart) {
        pieChart.description.isEnabled = false
        pieChart.isDrawHoleEnabled = true // 是否显示中间的洞
        pieChart.holeRadius = 40f
        pieChart.transparentCircleRadius = 45f
        pieChart.setEntryLabelColor(Color.BLACK)
        pieChart.setEntryLabelTextSize(12f)
        pieChart.legend.isEnabled = true // 显示图例
        pieChart.animateY(1000)
        pieChart.setUsePercentValues(true) // 显示百分比
    }

    /**
     * 根据选择的时间范围加载图表数据
     * @param rangeOptionIndex Spinner中选项的索引
     */
    private fun loadChartData(rangeOptionIndex: Int) {
        val (startDate, endDate, groupByFormat, axisLabels) = getTimeRangeAndFormat(rangeOptionIndex)

        projectViewModel.getTimeRecordsByDateRange(projectId, startDate, endDate).observe(this) { records ->
            if (records != null) {
                updateBarChartData(binding.barChart, records, groupByFormat, axisLabels)
                updatePieChartData(binding.pieChart, records) // 饼图可以显示总时长占比
            }
        }
    }

    /**
     * 根据选择的范围获取开始/结束日期、分组格式和X轴标签
     * @param rangeOptionIndex Spinner中选项的索引
     * @return Pair<Date, Date, SimpleDateFormat, List<String>>
     */
    private fun getTimeRangeAndFormat(rangeOptionIndex: Int): Triple<Date, Date, SimpleDateFormat?, List<String>?> {
        val cal = Calendar.getInstance()
        var startDate: Date
        var endDate: Date
        var groupByFormat: SimpleDateFormat? = null
        var axisLabels: List<String>? = null

        when (rangeOptionIndex) {
            0 -> { // 天 (显示最近7天)
                endDate = cal.time
                cal.add(Calendar.DAY_OF_YEAR, -6)
                setCalendarToStartOfDay(cal)
                startDate = cal.time
                groupByFormat = SimpleDateFormat("MM-dd", Locale.getDefault())
                axisLabels = getLastNDaysLabels(7)
            }
            1 -> { // 周 (显示当前周)
                cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                setCalendarToStartOfDay(cal)
                startDate = cal.time
                cal.add(Calendar.WEEK_OF_YEAR, 1)
                cal.add(Calendar.MILLISECOND, -1)
                endDate = cal.time
                groupByFormat = SimpleDateFormat("EEE", Locale.getDefault()) // 星期几
                axisLabels = getWeekDayLabels()
            }
            2 -> { // 月 (显示当前月)
                cal.set(Calendar.DAY_OF_MONTH, 1)
                setCalendarToStartOfDay(cal)
                startDate = cal.time
                cal.add(Calendar.MONTH, 1)
                cal.add(Calendar.MILLISECOND, -1)
                endDate = cal.time
                groupByFormat = SimpleDateFormat("dd", Locale.getDefault()) // 日期
                 axisLabels = getMonthDayLabels(Calendar.getInstance().getActualMaximum(Calendar.DAY_OF_MONTH))
            }
            3 -> { // 季度 (显示当前季度)
                val currentMonth = cal.get(Calendar.MONTH)
                val quarterStartMonth = currentMonth / 3 * 3
                cal.set(Calendar.MONTH, quarterStartMonth)
                cal.set(Calendar.DAY_OF_MONTH, 1)
                setCalendarToStartOfDay(cal)
                startDate = cal.time
                cal.add(Calendar.MONTH, 3)
                cal.add(Calendar.MILLISECOND, -1)
                endDate = cal.time
                groupByFormat = SimpleDateFormat("MMM", Locale.getDefault()) // 月份缩写
                axisLabels = getQuarterMonthLabels(quarterStartMonth)
            }
            4 -> { // 年 (显示当前年)
                cal.set(Calendar.DAY_OF_YEAR, 1)
                setCalendarToStartOfDay(cal)
                startDate = cal.time
                cal.add(Calendar.YEAR, 1)
                cal.add(Calendar.MILLISECOND, -1)
                endDate = cal.time
                groupByFormat = SimpleDateFormat("MMM", Locale.getDefault()) // 月份缩写
                axisLabels = getYearMonthLabels()
            }
            else -> { // 默认按周
                cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                setCalendarToStartOfDay(cal)
                startDate = cal.time
                cal.add(Calendar.WEEK_OF_YEAR, 1)
                cal.add(Calendar.MILLISECOND, -1)
                endDate = cal.time
                groupByFormat = SimpleDateFormat("EEE", Locale.getDefault())
                axisLabels = getWeekDayLabels()
            }
        }
        return Triple(startDate, endDate, groupByFormat, axisLabels)
    }

    // --- X轴标签生成辅助函数 ---
    private fun getLastNDaysLabels(days: Int): List<String> {
        val labels = mutableListOf<String>()
        val cal = Calendar.getInstance()
        val format = SimpleDateFormat("MM-dd", Locale.getDefault())
        for (i in 0 until days) {
            labels.add(format.format(cal.time))
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }
        return labels.reversed()
    }
    
    private fun getWeekDayLabels(): List<String> {
        val labels = mutableListOf<String>()
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
        val format = SimpleDateFormat("EEE", Locale.getDefault())
        for (i in 0..6) {
            labels.add(format.format(cal.time))
            cal.add(Calendar.DAY_OF_WEEK, 1)
        }
        return labels
    }
    
    private fun getMonthDayLabels(daysInMonth: Int): List<String> {
        return (1..daysInMonth).map { it.toString() }
    }
    
    private fun getQuarterMonthLabels(startMonth: Int): List<String> {
        val labels = mutableListOf<String>()
        val cal = Calendar.getInstance()
        val format = SimpleDateFormat("MMM", Locale.getDefault())
        for(i in 0 until 3){
            cal.set(Calendar.MONTH, startMonth + i)
            labels.add(format.format(cal.time))
        }
       return labels
    }
    
    private fun getYearMonthLabels(): List<String> {
        val labels = mutableListOf<String>()
        val cal = Calendar.getInstance()
        val format = SimpleDateFormat("MMM", Locale.getDefault())
         for(i in 0 until 12){
            cal.set(Calendar.MONTH, i)
            labels.add(format.format(cal.time))
        }
        return labels
    }

    /**
     * 将Calendar对象的时间设置为当天的开始（00:00:00.000）
     */
    private fun setCalendarToStartOfDay(cal: Calendar) {
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
    }

    /**
     * 更新柱状图数据
     * @param barChart 柱状图实例
     * @param records 时间记录列表
     * @param groupByFormat 用于分组的日期格式
     * @param axisLabels X轴标签列表
     */
    private fun updateBarChartData(barChart: BarChart, records: List<TimeRecord>, groupByFormat: SimpleDateFormat?, axisLabels: List<String>?) {
        if (groupByFormat == null || axisLabels == null) return // 如果格式或标签无效则返回

        // 按格式化后的日期分组并计算总时长（分钟）
        val groupedData = records.groupBy { groupByFormat.format(it.date) }
            .mapValues { entry -> (entry.value.sumOf { it.durationSeconds } / 60.0).toFloat() }
            .toMutableMap()

        // 为标签列表中没有数据的日期填充0
        axisLabels.forEach { label ->
            groupedData.putIfAbsent(label, 0f)
        }

        // 创建柱状图数据项，确保顺序与 axisLabels 一致
        val entries = axisLabels.mapIndexed { index, label ->
             BarEntry(index.toFloat(), groupedData[label] ?: 0f)
        }

        // 设置X轴标签
        barChart.xAxis.valueFormatter = IndexAxisValueFormatter(axisLabels)
        barChart.xAxis.labelCount = axisLabels.size // 确保所有标签都显示
        
        // 如果只有一个数据集，直接设置
        if (barChart.data != null && barChart.data.dataSetCount > 0) {
            val set = barChart.data.getDataSetByIndex(0) as BarDataSet
            set.values = entries
            barChart.data.notifyDataChanged()
            barChart.notifyDataSetChanged()
        } else {
            // 创建新的数据集
            val dataSet = BarDataSet(entries, "时间 (分钟)")
            dataSet.colors = ColorTemplate.MATERIAL_COLORS.toList() // 设置颜色
            dataSet.setDrawValues(true) // 在柱子上显示数值
            dataSet.valueTextSize = 10f

            val barData = BarData(dataSet)
            barData.barWidth = 0.6f // 设置柱子宽度
            barChart.data = barData
        }
        
        barChart.invalidate() // 刷新图表
        barChart.animateY(1000)
    }

    /**
     * 更新饼图数据（示例：显示总时长占比，可以根据需求修改）
     * @param pieChart 饼图实例
     * @param records 时间记录列表
     */
    private fun updatePieChartData(pieChart: PieChart, records: List<TimeRecord>) {
        if (records.isEmpty()) {
            pieChart.clear()
            pieChart.invalidate()
            return
        }
        
        // 示例：按日期（天）统计时长占比
        val dailyTotals = records
            .groupBy { dateFormat.format(it.date) }
            .mapValues { (_, dayRecords) -> dayRecords.sumOf { it.durationSeconds }.toFloat() }
            .toList()
            .sortedByDescending { it.second } // 按时长降序
            .take(5) // 最多显示前5天
            
        val otherTotal = records.sumOf { it.durationSeconds }.toFloat() - dailyTotals.sumOf { it.second }
        
        val entries = mutableListOf<PieEntry>()
        dailyTotals.forEach {
            entries.add(PieEntry(it.second, it.first)) // 值, 标签
        }
        if(otherTotal > 0){
             entries.add(PieEntry(otherTotal, "其他"))
        }

        val dataSet = PieDataSet(entries, "按天时长分布")
        dataSet.colors = ColorTemplate.COLORFUL_COLORS.toList() // 设置颜色
        dataSet.valueFormatter = PercentFormatter(pieChart) // 显示百分比
        dataSet.valueTextSize = 12f
        dataSet.valueTextColor = Color.BLACK
        dataSet.sliceSpace = 2f // 分片之间的间隔

        val pieData = PieData(dataSet)
        pieChart.data = pieData
        pieChart.invalidate() // 刷新图表
        pieChart.animateY(1000)
    }

    // 处理ActionBar返回按钮
    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
