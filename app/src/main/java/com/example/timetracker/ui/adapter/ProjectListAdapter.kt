package com.example.timetracker.ui.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.timetracker.R // 需要创建资源文件
import com.example.timetracker.data.model.Project
import com.example.timetracker.databinding.ItemProjectBinding
import com.example.timetracker.util.formatDurationHMS // 需要创建工具类

/**
 * 项目列表的RecyclerView适配器
 * 使用ListAdapter来高效处理列表更新
 *
 * @param onItemClick 项目项点击回调
 * @param onStartTrackingClick 开始/停止打卡按钮点击回调
 */
class ProjectListAdapter(
    private val onItemClick: (Project) -> Unit,
    private val onStartTrackingClick: (Project) -> Unit
) : ListAdapter<Project, ProjectListAdapter.ProjectViewHolder>(ProjectDiffCallback()) {

    // 存储每个项目的剩余时间和状态，以便更新UI
    private val remainingTimeMap = mutableMapOf<Long, Int>()
    private val statusMap = mutableMapOf<Long, Boolean>()

    /**
     * 创建ViewHolder
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProjectViewHolder {
        val binding = ItemProjectBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ProjectViewHolder(binding)
    }

    /**
     * 绑定数据到ViewHolder
     */
    override fun onBindViewHolder(holder: ProjectViewHolder, position: Int) {
        val project = getItem(position)
        holder.bind(project, remainingTimeMap[project.id], statusMap[project.id])
    }

    /**
     * 更新指定项目的剩余时间
     * @param projectId 项目ID
     * @param remainingSeconds 剩余时间（秒）
     */
    fun updateRemainingTime(projectId: Long, remainingSeconds: Int) {
        if (remainingTimeMap[projectId] != remainingSeconds) {
            remainingTimeMap[projectId] = remainingSeconds
            // 通知特定项的payload更新，避免整个项重绘
            notifyItemChanged(currentList.indexOfFirst { it.id == projectId }, PAYLOAD_REMAINING_TIME)
        }
    }

    /**
     * 更新指定项目的状态（是否正在打卡）
     * @param projectId 项目ID
     * @param isTracking 是否正在打卡
     */
    fun updateStatus(projectId: Long, isTracking: Boolean) {
        if (statusMap[projectId] != isTracking) {
            statusMap[projectId] = isTracking
             // 通知特定项的payload更新，避免整个项重绘
            val position = currentList.indexOfFirst { it.id == projectId }
            if (position != -1) {
                notifyItemChanged(position, PAYLOAD_STATUS)
            }
        }
    }
    
    /**
     * 获取项目的打卡状态
     * @param projectId 项目ID
     * @return 项目的打卡状态，如果不存在则返回null
     */
    fun getProjectStatus(projectId: Long): Boolean? {
        return statusMap[projectId]
    }
    
    /**
     * 重置所有项目的状态
     */
    fun resetAllStatus() {
        val projectIds = ArrayList(statusMap.keys)
        for (id in projectIds) {
            statusMap[id] = false
        }
        notifyDataSetChanged() // 这里使用全局刷新，因为可能有多个项目状态变化
    }

    /**
     * ViewHolder类
     */
    inner class ProjectViewHolder(private val binding: ItemProjectBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private val context: Context = binding.root.context

        init {
            // 设置整个项目项的点击事件
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(getItem(position))
                }
            }
            // 设置开始/停止打卡按钮的点击事件
            binding.btnStartTracking.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onStartTrackingClick(getItem(position))
                }
            }
        }

        /**
         * 绑定数据到视图
         * @param project 项目数据
         * @param remainingSeconds 剩余时间（秒），可能为null
         * @param isTracking 是否正在打卡，可能为null
         */
        @SuppressLint("StringFormatInvalid", "StringFormatMatches") // R.string...需要定义
        fun bind(project: Project, remainingSeconds: Int?, isTracking: Boolean?) {
            binding.tvProjectName.text = project.name
            // 格式化每日目标时间，显示为 'X 分钟'
            val dailyTargetText = context.getString(R.string.target_format, project.dailyTargetMinutes.toString()) // 使用原始分钟数
            binding.tvDailyTarget.text = dailyTargetText

            // 更新剩余时间显示
            updateRemainingTimeView(remainingSeconds)
            // 更新状态显示
            updateStatusView(isTracking)
        }
        
        /**
         * 更新剩余时间视图
         * @param remainingSeconds 剩余时间（秒）
         */
        fun updateRemainingTimeView(remainingSeconds: Int?) {
            val remainingTimeText = if (remainingSeconds != null && remainingSeconds > 0) {
                context.getString(R.string.remaining_time_format, formatDurationHMS(remainingSeconds.toLong(), false))
            } else if (remainingSeconds != null && remainingSeconds <= 0) {
                context.getString(R.string.target_achieved)
            } else {
                // 默认或加载中状态
                context.getString(R.string.loading_time) 
            }
            binding.tvRemainingTime.text = remainingTimeText
        }
        
        /**
         * 更新状态视图（打卡按钮和状态标签）
         * @param isTracking 是否正在打卡
         */
        fun updateStatusView(isTracking: Boolean?){
            val tracking = isTracking ?: false // 默认为false
            if (tracking) {
                binding.tvStatus.text = context.getString(R.string.status_tracking)
                binding.tvStatus.visibility = ViewGroup.VISIBLE
                binding.btnStartTracking.text = context.getString(R.string.stop_tracking)
                // 可以设置不同的背景或图标
                // binding.tvStatus.setBackgroundResource(R.drawable.status_background_tracking)
            } else {
                binding.tvStatus.visibility = ViewGroup.GONE // 或者显示“已停止”
                binding.btnStartTracking.text = context.getString(R.string.start_tracking)
                 // 可以设置不同的背景或图标
                // binding.tvStatus.setBackgroundResource(R.drawable.status_background_stopped)
            }
        }
    }
    
    /**
     * 处理部分更新
     */
    override fun onBindViewHolder(holder: ProjectViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads)
        } else {
            val project = getItem(position)
            payloads.forEach { payload ->
                when (payload) {
                    PAYLOAD_REMAINING_TIME -> holder.updateRemainingTimeView(remainingTimeMap[project.id])
                    PAYLOAD_STATUS -> holder.updateStatusView(statusMap[project.id])
                }
            }
        }
    }

    companion object {
        // 用于部分更新的Payload标识
        private const val PAYLOAD_REMAINING_TIME = 1
        private const val PAYLOAD_STATUS = 2
    }
}

/**
 * DiffUtil.ItemCallback实现
 * 用于比较项目列表项是否相同以及内容是否发生变化
 */
class ProjectDiffCallback : DiffUtil.ItemCallback<Project>() {
    override fun areItemsTheSame(oldItem: Project, newItem: Project): Boolean {
        // 比较项目ID是否相同
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Project, newItem: Project): Boolean {
        // 比较项目内容是否相同（这里只比较了 name 和 dailyTargetMinutes，可以根据需要添加更多字段）
        // 注意：剩余时间和状态不在这里比较，它们通过payload更新
        return oldItem.name == newItem.name && oldItem.dailyTargetMinutes == newItem.dailyTargetMinutes
    }
}
