<template>
  <!-- 底部固定的任务进度条 -->
  <div v-if="task" class="task-bar">
    <div class="task-bar__info">
      <el-tag :type="tagType" size="small" effect="dark">{{ statusText }}</el-tag>
      <span class="task-bar__label">{{ bizLabel }}</span>
      <span class="task-bar__progress">{{ task.progress ?? 0 }}%</span>
      <span v-if="task.status === 'FAILED' && task.errorMsg" class="task-bar__error">
        {{ task.errorMsg }}
      </span>
    </div>
    <el-progress
      :percentage="task.progress ?? 0"
      :status="progressStatus"
      :stroke-width="8"
      class="task-bar__progress-bar"
    />
  </div>
</template>

<script setup lang="ts">
// 底部任务进度条：从 task store 读取当前任务
import { computed } from 'vue'
import { useTaskStore } from '@/stores/task'
import { bizTypeLabel, taskStatusLabel } from '@/utils'

const taskStore = useTaskStore()
const task = computed(() => taskStore.current)

const statusText = computed(() => taskStatusLabel(task.value?.status))
const bizLabel = computed(() => bizTypeLabel(task.value?.bizType))

const tagType = computed<'success' | 'info' | 'warning' | 'danger'>(() => {
  switch (task.value?.status) {
    case 'SUCCESS':
      return 'success'
    case 'FAILED':
      return 'danger'
    case 'RUNNING':
      return 'warning'
    default:
      return 'info'
  }
})

const progressStatus = computed<'success' | 'exception' | undefined>(() => {
  if (task.value?.status === 'SUCCESS') return 'success'
  if (task.value?.status === 'FAILED') return 'exception'
  return undefined
})
</script>

<style scoped>
.task-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 100;
  background: #fff;
  border-top: 1px solid #ebeef5;
  padding: 8px 16px 10px;
  box-shadow: 0 -2px 8px rgba(0, 0, 0, 0.06);
}
.task-bar__info {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 4px;
  font-size: 13px;
}
.task-bar__label {
  font-weight: 600;
}
.task-bar__progress {
  color: #909399;
}
.task-bar__error {
  color: #f56c6c;
  margin-left: auto;
  max-width: 50%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.task-bar__progress-bar {
  margin: 0;
}
</style>
