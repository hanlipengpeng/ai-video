// 任务状态：工作台底部进度条使用
import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import * as taskApi from '@/api/task'
import type { TaskLog } from '@/types'

/** 任务终态：达到这些状态后停止轮询 */
const TERMINAL_STATUS = new Set(['SUCCESS', 'FAILED'])

export const useTaskStore = defineStore('task', () => {
  /** 当前正在跟踪的任务 */
  const current = ref<TaskLog | null>(null)
  /** 轮询定时器 */
  let timer: ReturnType<typeof setInterval> | null = null

  const isActive = computed(() => {
    const s = current.value?.status
    return !!s && !TERMINAL_STATUS.has(s)
  })

  /** 开始跟踪某个任务，每 2s 轮询一次 */
  function track(taskId: number, onDone?: (task: TaskLog) => void) {
    stop()
    fetchOnce(taskId)
      .then((t) => {
        current.value = t
        if (TERMINAL_STATUS.has(t.status)) {
          onDone?.(t)
          return
        }
        timer = setInterval(async () => {
          try {
            const latest = await fetchOnce(taskId)
            current.value = latest
            if (TERMINAL_STATUS.has(latest.status)) {
              stop()
              onDone?.(latest)
            }
          } catch {
            // 单次轮询失败不影响整体，下次再试
          }
        }, 2000)
      })
      .catch(() => {
        // 拉取失败则不轮询
      })
  }

  async function fetchOnce(taskId: number) {
    return taskApi.getTask(taskId)
  }

  /** 停止轮询并清空当前任务 */
  function stop() {
    if (timer) {
      clearInterval(timer)
      timer = null
    }
  }

  /** 清空当前任务展示 */
  function clear() {
    stop()
    current.value = null
  }

  return { current, isActive, track, stop, clear }
})
