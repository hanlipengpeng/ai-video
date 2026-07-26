<template>
  <!-- 视频片段卡片：video 播放器 + 重新生成按钮 -->
  <el-card class="video-card" shadow="hover" :body-style="{ padding: '0' }">
    <div class="video-card__seq">分镜 {{ seq ?? '-' }}</div>
    <div class="video-card__video-wrap">
      <video
        v-if="videoUrl"
        :src="videoUrl"
        controls
        playsinline
        class="video-card__video"
      />
      <div v-else class="video-card__placeholder">
        <el-icon :size="28"><VideoPlay /></el-icon>
        <span>{{ statusText }}</span>
      </div>
    </div>
    <div class="video-card__footer">
      <el-tag :type="tagType" size="small">{{ statusText }}</el-tag>
      <span v-if="clip.durationSec" class="video-card__duration">{{ clip.durationSec }}s</span>
      <el-button
        size="small"
        type="primary"
        plain
        :loading="loading"
        :disabled="!storyboardId"
        @click="onRegenerate"
      >
        重新生成
      </el-button>
    </div>
  </el-card>
</template>

<script setup lang="ts">
// 视频片段卡片
import { computed, ref } from 'vue'
import { VideoPlay } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import type { VideoClip } from '@/types'
import { resolveAssetUrl } from '@/utils'
import * as mediaApi from '@/api/media'

const props = defineProps<{
  clip: VideoClip
  seq?: number
  storyboardId?: number
}>()

const emit = defineEmits<{ regenerate: [taskId: number] }>()

const loading = ref(false)
const videoUrl = computed(() => resolveAssetUrl(props.clip.videoUrl))

const statusText = computed(() => {
  switch (props.clip.status) {
    case 'SUCCESS':
      return '已生成'
    case 'GENERATING':
      return '生成中'
    case 'FAILED':
      return '生成失败'
    default:
      return props.clip.status
  }
})

const tagType = computed<'success' | 'info' | 'warning' | 'danger'>(() => {
  switch (props.clip.status) {
    case 'SUCCESS':
      return 'success'
    case 'GENERATING':
      return 'warning'
    case 'FAILED':
      return 'danger'
    default:
      return 'info'
  }
})

async function onRegenerate() {
  if (!props.storyboardId) {
    ElMessage.warning('缺少分镜 ID，无法重新生成')
    return
  }
  loading.value = true
  try {
    const res = await mediaApi.regenerateVideo(props.storyboardId)
    ElMessage.success('已提交重新生成任务')
    emit('regenerate', res.taskId)
  } catch {
    /* 拦截器已提示 */
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.video-card {
  width: 100%;
}
.video-card__seq {
  padding: 6px 10px;
  font-size: 12px;
  color: #909399;
  background: #fafafa;
  border-bottom: 1px solid #f0f0f0;
}
.video-card__video-wrap {
  width: 100%;
  aspect-ratio: 9 / 16;
  background: #000;
  overflow: hidden;
}
.video-card__video {
  width: 100%;
  height: 100%;
  display: block;
  object-fit: contain;
}
.video-card__placeholder {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  color: #c0c4cc;
  font-size: 12px;
}
.video-card__footer {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
}
.video-card__duration {
  font-size: 12px;
  color: #909399;
  margin-right: auto;
}
</style>
