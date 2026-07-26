<template>
  <!-- 画面卡片：图片预览 + 重新生成按钮 -->
  <el-card class="frame-card" shadow="hover" :body-style="{ padding: '0' }">
    <div class="frame-card__seq">分镜 {{ seq ?? '-' }}</div>
    <div class="frame-card__image-wrap">
      <el-image
        v-if="imageUrl"
        :src="imageUrl"
        fit="cover"
        :preview-src-list="[imageUrl]"
        class="frame-card__image"
      />
      <div v-else class="frame-card__placeholder">
        <el-icon :size="28"><Picture /></el-icon>
        <span>{{ statusText }}</span>
      </div>
    </div>
    <div class="frame-card__footer">
      <el-tag :type="tagType" size="small">{{ statusText }}</el-tag>
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
// 分镜画面卡片
import { computed, ref } from 'vue'
import { Picture } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import type { FrameImage } from '@/types'
import { resolveAssetUrl } from '@/utils'
import * as mediaApi from '@/api/media'

const props = defineProps<{
  frame: FrameImage
  seq?: number
  storyboardId?: number
}>()

const emit = defineEmits<{ regenerate: [taskId: number] }>()

const loading = ref(false)
const imageUrl = computed(() => resolveAssetUrl(props.frame.imageUrl))

const statusText = computed(() => {
  switch (props.frame.status) {
    case 'SUCCESS':
      return '已生成'
    case 'GENERATING':
      return '生成中'
    case 'FAILED':
      return '生成失败'
    default:
      return props.frame.status
  }
})

const tagType = computed<'success' | 'info' | 'warning' | 'danger'>(() => {
  switch (props.frame.status) {
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
    const res = await mediaApi.regenerateFrame(props.storyboardId)
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
.frame-card {
  width: 100%;
}
.frame-card__seq {
  padding: 6px 10px;
  font-size: 12px;
  color: #909399;
  background: #fafafa;
  border-bottom: 1px solid #f0f0f0;
}
.frame-card__image-wrap {
  width: 100%;
  aspect-ratio: 9 / 16;
  background: #f5f7fa;
  overflow: hidden;
}
.frame-card__image {
  width: 100%;
  height: 100%;
  display: block;
}
.frame-card__placeholder {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  color: #c0c4cc;
  font-size: 12px;
}
.frame-card__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 10px;
}
</style>
