<template>
  <!-- 分镜卡片：序号 + Prompt 可编辑 + 台词/旁白可编辑 + 时长 -->
  <el-card class="storyboard-card" shadow="hover">
    <template #header>
      <div class="sb-card__header">
        <el-tag size="small">分镜 {{ storyboard.seq }}</el-tag>
        <span class="sb-card__time">建议时长：{{ storyboard.durationSec ?? '-' }}s</span>
        <el-button v-if="!editing" text size="small" :icon="Edit" @click="onEdit">编辑</el-button>
        <el-button v-else text size="small" type="primary" :icon="Check" @click="onSave">保存</el-button>
        <el-button v-if="editing" text size="small" :icon="Close" @click="onCancel">取消</el-button>
      </div>
    </template>

    <!-- 展示态 -->
    <template v-if="!editing">
      <div class="sb-card__field"><b>画面 Prompt：</b>{{ storyboard.prompt || '-' }}</div>
      <div class="sb-card__field"><b>台词：</b>{{ storyboard.dialogue || '-' }}</div>
      <div class="sb-card__field"><b>旁白：</b>{{ storyboard.narration || '-' }}</div>
    </template>

    <!-- 编辑态 -->
    <template v-else>
      <el-form label-width="80px" size="small">
        <el-form-item label="时长(秒)">
          <el-input-number v-model="form.durationSec" :min="0.5" :step="0.5" :precision="1" />
        </el-form-item>
        <el-form-item label="Prompt">
          <el-input v-model="form.prompt" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="台词">
          <el-input v-model="form.dialogue" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="旁白">
          <el-input v-model="form.narration" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
    </template>
  </el-card>
</template>

<script setup lang="ts">
// 分镜卡片组件，支持就地编辑 Prompt/台词/旁白/时长
import { reactive, ref, watch } from 'vue'
import { Check, Close, Edit } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import type { Storyboard } from '@/types'
import * as creationApi from '@/api/creation'

const props = defineProps<{ storyboard: Storyboard }>()
const emit = defineEmits<{ updated: [sb: Storyboard] }>()

const editing = ref(false)
const form = reactive({
  prompt: '',
  dialogue: '',
  narration: '',
  durationSec: 4
})

watch(
  () => props.storyboard,
  (sb) => {
    form.prompt = sb.prompt || ''
    form.dialogue = sb.dialogue || ''
    form.narration = sb.narration || ''
    form.durationSec = sb.durationSec ?? 4
  },
  { immediate: true }
)

function onEdit() {
  editing.value = true
}
function onCancel() {
  editing.value = false
}
async function onSave() {
  try {
    const updated = await creationApi.updateStoryboard(props.storyboard.id, {
      prompt: form.prompt,
      dialogue: form.dialogue,
      narration: form.narration,
      durationSec: form.durationSec
    })
    ElMessage.success('分镜已保存')
    emit('updated', updated)
    editing.value = false
  } catch {
    /* 拦截器已提示 */
  }
}
</script>

<style scoped>
.storyboard-card {
  margin-bottom: 12px;
}
.sb-card__header {
  display: flex;
  align-items: center;
  gap: 8px;
}
.sb-card__time {
  color: #909399;
  font-size: 12px;
  margin-right: auto;
}
.sb-card__field {
  font-size: 13px;
  line-height: 1.7;
  margin-bottom: 4px;
  word-break: break-word;
}
</style>
