<template>
  <div class="studio-page">
    <!-- ===== 顶部栏 ===== -->
    <el-header class="studio-header">
      <div class="studio-header__left">
        <el-button text :icon="Back" @click="goBack">项目列表</el-button>
        <el-divider direction="vertical" />
        <el-input
          v-model="projectTitle"
          class="studio-header__title"
          size="default"
          @blur="onSaveTitle"
          @keyup.enter="onSaveTitle"
        />
        <el-tag size="small" type="info">{{ styleLabel(project?.stylePreset) }}</el-tag>
        <el-tag size="small">{{ statusLabel(project?.status) }}</el-tag>
      </div>
      <div class="studio-header__right">
        <el-button type="warning" :icon="Film" :loading="composing" @click="onCompose">
          合成视频
        </el-button>
        <el-button type="success" :icon="Download" :disabled="!finalVideoUrl" @click="onDownload">
          下载
        </el-button>
      </div>
    </el-header>

    <div class="studio-body">
      <!-- ===== 左侧步骤导航 ===== -->
      <aside class="studio-sider">
        <el-steps direction="vertical" :active="activeStep - 1" :space="80">
          <el-step
            v-for="(s, idx) in steps"
            :key="s.key"
            :title="`${idx + 1} ${s.title}`"
            :status="stepStatus(idx + 1)"
            @click="activeStep = idx + 1"
          />
        </el-steps>
        <div class="studio-sider__tip">点击步骤可切换/回退编辑</div>
      </aside>

      <!-- ===== 中间主区域 ===== -->
      <main class="studio-main">
        <!-- 步骤 1：原文 -->
        <section v-if="activeStep === 1">
          <div class="step-toolbar">
            <h3>① 原文输入</h3>
            <div>
              <el-button :icon="RefreshRight" :loading="savingSource" @click="onSaveSource">
                保存原文
              </el-button>
            </div>
          </div>
          <el-input
            v-model="sourceText"
            type="textarea"
            :rows="18"
            placeholder="粘贴小说文本（建议 5000 字以内）..."
            maxlength="20000"
            show-word-limit
          />
          <p class="step-tip">保存原文后，进入下一步生成剧本。</p>
        </section>

        <!-- 步骤 2：剧本 -->
        <section v-else-if="activeStep === 2">
          <div class="step-toolbar">
            <h3>② 剧本</h3>
            <div>
              <el-button :icon="Edit" @click="scriptEditing = !scriptEditing">
                {{ scriptEditing ? '完成编辑' : '编辑剧本' }}
              </el-button>
              <el-button
                type="primary"
                :icon="MagicStick"
                :loading="generatingScript"
                :disabled="!sourceText"
                @click="onGenerateScript"
              >
                生成剧本
              </el-button>
              <el-button
                type="success"
                :icon="Check"
                :loading="savingScript"
                :disabled="!scriptContent"
                @click="onSaveScript"
              >
                保存编辑
              </el-button>
            </div>
          </div>

          <el-empty v-if="!scriptContent" description="暂无剧本，点击「生成剧本」" />

          <!-- 编辑态：纯文本 JSON -->
          <el-input
            v-else-if="scriptEditing"
            v-model="scriptContent"
            type="textarea"
            :rows="22"
            placeholder="剧本 JSON 内容"
          />

          <!-- 展示态：结构化卡片 -->
          <div v-else>
            <el-card v-for="(scene, idx) in scriptScenes" :key="idx" class="script-card" shadow="hover">
              <template #header>
                <b>场景 {{ idx + 1 }}：{{ scene.heading }}</b>
              </template>
              <p class="script-card__desc">{{ scene.description }}</p>
              <div v-for="(d, di) in scene.dialogues" :key="di" class="script-card__line">
                <el-tag size="small">{{ d.character }}</el-tag>
                <span>{{ d.line }}</span>
              </div>
              <p v-if="scene.narration" class="script-card__narration">
                <i>旁白：{{ scene.narration }}</i>
              </p>
            </el-card>
          </div>
        </section>

        <!-- 步骤 3：分镜 + 角色 -->
        <section v-else-if="activeStep === 3">
          <div class="step-toolbar">
            <h3>③ 分镜 & 角色</h3>
            <div>
              <el-button
                type="primary"
                :icon="MagicStick"
                :loading="generatingStoryboard"
                :disabled="!scriptContent"
                @click="onGenerateStoryboard"
              >
                生成分镜
              </el-button>
              <el-button
                :icon="User"
                :loading="extractingCharacters"
                :disabled="!scriptContent"
                @click="onExtractCharacters"
              >
                抽取角色
              </el-button>
            </div>
          </div>

          <el-empty v-if="!storyboards.length" description="暂无分镜，点击「生成分镜」" />

          <div class="storyboard-grid">
            <StoryboardCard
              v-for="sb in storyboards"
              :key="sb.id"
              :storyboard="sb"
              @updated="onStoryboardUpdated"
            />
          </div>

          <el-divider content-position="left">角色列表</el-divider>
          <el-empty v-if="!characters.length" description="暂无角色，点击「抽取角色」" />
          <div class="character-grid">
            <el-card v-for="c in characters" :key="c.id" class="character-card" shadow="hover">
              <template #header>
                <div class="character-card__header">
                  <b>{{ c.name }}</b>
                  <el-tag size="small" :type="c.status === 'READY' ? 'success' : 'info'">
                    {{ c.status === 'READY' ? '参考图就绪' : '待生成' }}
                  </el-tag>
                </div>
              </template>
              <div class="character-card__img-wrap">
                <el-image
                  v-if="c.referenceImageUrl"
                  :src="resolveAssetUrl(c.referenceImageUrl)"
                  fit="cover"
                  :preview-src-list="[resolveAssetUrl(c.referenceImageUrl)]"
                  class="character-card__img"
                />
                <div v-else class="character-card__placeholder">无参考图</div>
              </div>
              <p class="character-card__desc">{{ c.appearance || '无外貌描述' }}</p>
              <el-button
                size="small"
                type="primary"
                plain
                :loading="generatingCharImg[c.id]"
                @click="onGenerateCharImage(c)"
              >
                {{ c.referenceImageUrl ? '重新生成参考图' : '生成参考图' }}
              </el-button>
            </el-card>
          </div>
        </section>

        <!-- 步骤 4：画面 -->
        <section v-else-if="activeStep === 4">
          <div class="step-toolbar">
            <h3>④ 画面</h3>
            <el-button
              type="primary"
              :icon="MagicStick"
              :loading="generatingFrames"
              :disabled="!storyboards.length"
              @click="onGenerateFrames"
            >
              批量生成画面
            </el-button>
          </div>
          <el-empty v-if="!frames.length" description="暂无画面，点击「批量生成画面」" />
          <div class="media-grid">
            <FrameImageCard
              v-for="f in framesWithSeq"
              :key="f.id"
              :frame="f"
              :seq="f.seq"
              :storyboard-id="f.storyboardId"
              @regenerate="onSubTask"
            />
          </div>
        </section>

        <!-- 步骤 5：视频 -->
        <section v-else-if="activeStep === 5">
          <div class="step-toolbar">
            <h3>⑤ 视频片段</h3>
            <el-button
              type="primary"
              :icon="MagicStick"
              :loading="generatingVideos"
              :disabled="!frames.length"
              @click="onGenerateVideos"
            >
              批量生成视频
            </el-button>
          </div>
          <el-empty v-if="!videos.length" description="暂无视频，点击「批量生成视频」" />
          <div class="media-grid">
            <VideoClipCard
              v-for="v in videosWithSeq"
              :key="v.id"
              :clip="v"
              :seq="v.seq"
              :storyboard-id="v.storyboardId"
              @regenerate="onSubTask"
            />
          </div>
        </section>

        <!-- 步骤 6：导出 -->
        <section v-else-if="activeStep === 6">
          <div class="step-toolbar">
            <h3>⑥ 导出</h3>
            <div>
              <el-button type="warning" :icon="Film" :loading="composing" @click="onCompose">
                合成视频
              </el-button>
              <el-button
                v-if="finalVideoUrl"
                type="success"
                :icon="Download"
                @click="onDownload"
              >
                下载 MP4
              </el-button>
            </div>
          </div>
          <el-empty v-if="!finalVideoUrl" description="尚未合成最终视频" />
          <div v-else class="export-wrap">
            <video :src="finalVideoUrl" controls class="export-video" />
            <p class="step-tip">
              视频时长：{{ exportInfo?.durationSec ?? project?.durationSec ?? '-' }} 秒
            </p>
          </div>
        </section>
      </main>
    </div>

    <!-- ===== 底部任务进度条 ===== -->
    <TaskProgressBar />
  </div>
</template>

<script setup lang="ts">
// 创作工作台：原文 → 剧本 → 分镜 → 画面 → 视频 → 导出
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  Back,
  Check,
  Download,
  Edit,
  Film,
  MagicStick,
  RefreshRight,
  User
} from '@element-plus/icons-vue'
import TaskProgressBar from '@/components/TaskProgressBar.vue'
import StoryboardCard from '@/components/StoryboardCard.vue'
import FrameImageCard from '@/components/FrameImageCard.vue'
import VideoClipCard from '@/components/VideoClipCard.vue'
import { useTaskStore } from '@/stores/task'
import * as projectApi from '@/api/project'
import * as creationApi from '@/api/creation'
import * as mediaApi from '@/api/media'
import * as renderApi from '@/api/render'
import { resolveAssetUrl, statusLabel, styleLabel } from '@/utils'
import type {
  Character,
  ExportResult,
  FrameImage,
  Project,
  ScriptContent,
  Storyboard,
  VideoClip
} from '@/types'

const route = useRoute()
const router = useRouter()
const taskStore = useTaskStore()

const projectId = computed(() => Number(route.params.projectId))

const steps = [
  { key: 'source', title: '原文' },
  { key: 'script', title: '剧本' },
  { key: 'storyboard', title: '分镜' },
  { key: 'frame', title: '画面' },
  { key: 'video', title: '视频' },
  { key: 'export', title: '导出' }
]

// ===== 数据 =====
const project = ref<Project | null>(null)
const projectTitle = ref('')
const sourceText = ref('')
const scriptContent = ref('')
const scriptEditing = ref(false)
const storyboards = ref<Storyboard[]>([])
const characters = ref<Character[]>([])
const frames = ref<FrameImage[]>([])
const videos = ref<VideoClip[]>([])
const exportInfo = ref<ExportResult | null>(null)

const activeStep = ref(1)

// ===== 各按钮 loading =====
const savingSource = ref(false)
const savingScript = ref(false)
const generatingScript = ref(false)
const generatingStoryboard = ref(false)
const extractingCharacters = ref(false)
const generatingFrames = ref(false)
const generatingVideos = ref(false)
const composing = ref(false)
const generatingCharImg = reactive<Record<number, boolean>>({})

// ===== 派生 =====
const scriptScenes = computed<ScriptContent['scenes']>(() => {
  if (!scriptContent.value) return []
  try {
    const obj = JSON.parse(scriptContent.value) as ScriptContent
    return obj.scenes || []
  } catch {
    return []
  }
})

/** 把 frames 关联上分镜序号 */
const framesWithSeq = computed(() =>
  frames.value.map((f) => ({
    ...f,
    seq: storyboards.value.find((s) => s.id === f.storyboardId)?.seq
  }))
)

/** 把 videos 关联上分镜序号 */
const videosWithSeq = computed(() =>
  videos.value.map((v) => ({
    ...v,
    seq: storyboards.value.find((s) => s.id === v.storyboardId)?.seq
  }))
)

const finalVideoUrl = computed(() => resolveAssetUrl(exportInfo.value?.videoUrl || project.value?.finalVideoUrl))

// ===== 初始化加载 =====
async function loadAll() {
  await loadProject()
  await Promise.all([loadStoryboards(), loadCharacters(), loadFrames(), loadVideos(), loadExport()])
}

async function loadProject() {
  const p = await projectApi.getProject(projectId.value)
  project.value = p
  projectTitle.value = p.title
  sourceText.value = p.sourceText || ''
  scriptContent.value = p.scriptContent || ''
}

async function loadStoryboards() {
  try {
    storyboards.value = await creationApi.listStoryboards(projectId.value)
  } catch {
    /* ignore */
  }
}

async function loadCharacters() {
  try {
    characters.value = await creationApi.listCharacters(projectId.value)
  } catch {
    /* ignore */
  }
}

async function loadFrames() {
  try {
    frames.value = await mediaApi.listFrames(projectId.value)
  } catch {
    /* ignore */
  }
}

async function loadVideos() {
  try {
    videos.value = await mediaApi.listVideos(projectId.value)
  } catch {
    /* ignore */
  }
}

async function loadExport() {
  try {
    exportInfo.value = await renderApi.exportVideo(projectId.value)
  } catch {
    // 未合成时后端返回 400，忽略
    exportInfo.value = null
  }
}

// ===== 步骤切换辅助 =====
function stepStatus(idx: number): 'wait' | 'process' | 'finish' | 'error' {
  if (idx + 1 === activeStep.value) return 'process'
  if (idx + 1 < activeStep.value) return 'finish'
  return 'wait'
}

// ===== 原文 / 标题 =====
async function onSaveSource() {
  if (!project.value) return
  savingSource.value = true
  try {
    await projectApi.updateProject(project.value.id, { sourceText: sourceText.value })
    ElMessage.success('原文已保存')
  } catch {
    /* 拦截器已提示 */
  } finally {
    savingSource.value = false
  }
}

async function onSaveTitle() {
  if (!project.value) return
  if (projectTitle.value === project.value.title) return
  try {
    const updated = await projectApi.updateProject(project.value.id, { title: projectTitle.value })
    project.value = updated
    ElMessage.success('标题已更新')
  } catch {
    /* 拦截器已提示 */
  }
}

// ===== 剧本 =====
async function onGenerateScript() {
  if (!sourceText.value.trim()) {
    ElMessage.warning('请先输入原文')
    return
  }
  generatingScript.value = true
  try {
    // 先保存原文，再触发生成
    await projectApi.updateProject(projectId.value, { sourceText: sourceText.value })
    const res = await creationApi.generateScript(projectId.value)
    ElMessage.success('剧本生成任务已提交')
    activeStep.value = 2
    trackTask(res.taskId, async () => {
      await loadProject()
    })
  } catch {
    /* 拦截器已提示 */
  } finally {
    generatingScript.value = false
  }
}

async function onSaveScript() {
  if (!scriptContent.value) return
  // 校验 JSON 合法性
  try {
    JSON.parse(scriptContent.value)
  } catch {
    ElMessage.error('剧本不是合法的 JSON，请检查')
    return
  }
  savingScript.value = true
  try {
    await creationApi.saveScript(projectId.value, scriptContent.value)
    ElMessage.success('剧本已保存')
    scriptEditing.value = false
  } catch {
    /* 拦截器已提示 */
  } finally {
    savingScript.value = false
  }
}

// ===== 分镜 / 角色 =====
async function onGenerateStoryboard() {
  generatingStoryboard.value = true
  try {
    const res = await creationApi.generateStoryboard(projectId.value)
    ElMessage.success('分镜生成任务已提交')
    activeStep.value = 3
    trackTask(res.taskId, async () => {
      await loadStoryboards()
    })
  } catch {
    /* 拦截器已提示 */
  } finally {
    generatingStoryboard.value = false
  }
}

async function onExtractCharacters() {
  extractingCharacters.value = true
  try {
    const res = await creationApi.extractCharacters(projectId.value)
    ElMessage.success('角色抽取任务已提交')
    activeStep.value = 3
    trackTask(res.taskId, async () => {
      await loadCharacters()
    })
  } catch {
    /* 拦截器已提示 */
  } finally {
    extractingCharacters.value = false
  }
}

async function onGenerateCharImage(c: Character) {
  generatingCharImg[c.id] = true
  try {
    const res = await creationApi.generateCharacterImage(c.id)
    ElMessage.success(`角色「${c.name}」参考图任务已提交`)
    trackTask(res.taskId, async () => {
      await loadCharacters()
    })
  } catch {
    /* 拦截器已提示 */
  } finally {
    generatingCharImg[c.id] = false
  }
}

function onStoryboardUpdated(sb: Storyboard) {
  const idx = storyboards.value.findIndex((s) => s.id === sb.id)
  if (idx >= 0) storyboards.value[idx] = sb
}

// ===== 画面 =====
async function onGenerateFrames() {
  generatingFrames.value = true
  try {
    const res = await mediaApi.generateFrames(projectId.value)
    ElMessage.success('批量画面生成任务已提交')
    activeStep.value = 4
    trackTask(res.taskId, async () => {
      await loadFrames()
    })
  } catch {
    /* 拦截器已提示 */
  } finally {
    generatingFrames.value = false
  }
}

// ===== 视频 =====
async function onGenerateVideos() {
  generatingVideos.value = true
  try {
    const res = await mediaApi.generateVideos(projectId.value)
    ElMessage.success('批量视频生成任务已提交')
    activeStep.value = 5
    trackTask(res.taskId, async () => {
      await loadVideos()
    })
  } catch {
    /* 拦截器已提示 */
  } finally {
    generatingVideos.value = false
  }
}

// ===== 合成 / 下载 =====
async function onCompose() {
  composing.value = true
  try {
    const res = await renderApi.composeVideo(projectId.value)
    ElMessage.success('合成任务已提交')
    activeStep.value = 6
    trackTask(res.taskId, async () => {
      await loadProject()
      await loadExport()
    })
  } catch {
    /* 拦截器已提示 */
  } finally {
    composing.value = false
  }
}

function onDownload() {
  if (!finalVideoUrl.value) {
    ElMessage.warning('尚无可下载的视频')
    return
  }
  window.open(finalVideoUrl.value, '_blank')
}

// ===== 任务跟踪 =====
function trackTask(taskId: number, onDone: () => Promise<void>) {
  taskStore.track(taskId, async (task) => {
    if (task.status === 'SUCCESS') {
      ElMessage.success('任务完成')
      await onDone()
    } else if (task.status === 'FAILED') {
      ElMessage.error(`任务失败：${task.errorMsg || '请重试'}`)
    }
  })
}

/** 单张/单个重新生成返回的子任务，跟踪进度并刷新对应列表 */
function onSubTask(taskId: number) {
  trackTask(taskId, async () => {
    await Promise.all([loadFrames(), loadVideos()])
  })
}

function goBack() {
  router.push('/')
}

onMounted(async () => {
  await loadAll()
  // 根据已有数据自动定位步骤
  if (videos.value.length) activeStep.value = 5
  else if (frames.value.length) activeStep.value = 4
  else if (storyboards.value.length) activeStep.value = 3
  else if (scriptContent.value) activeStep.value = 2
  else activeStep.value = 1
})
</script>

<style scoped>
.studio-page {
  display: flex;
  flex-direction: column;
  height: 100vh;
  padding-bottom: 70px; /* 给底部进度条留空间 */
}
.studio-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border-bottom: 1px solid #ebeef5;
  height: 56px;
  padding: 0 16px;
}
.studio-header__left {
  display: flex;
  align-items: center;
  gap: 8px;
}
.studio-header__title {
  width: 240px;
}
.studio-header__right {
  display: flex;
  gap: 8px;
}
.studio-body {
  flex: 1;
  display: flex;
  overflow: hidden;
}
.studio-sider {
  width: 180px;
  background: #fff;
  border-right: 1px solid #ebeef5;
  padding: 16px 12px;
  overflow: auto;
}
.studio-sider__tip {
  margin-top: 16px;
  font-size: 12px;
  color: #909399;
  line-height: 1.6;
}
.studio-main {
  flex: 1;
  overflow: auto;
  padding: 16px;
  background: #f5f7fa;
}
.step-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}
.step-toolbar h3 {
  margin: 0;
}
.step-tip {
  margin-top: 12px;
  font-size: 13px;
  color: #909399;
}
.storyboard-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(360px, 1fr));
  gap: 12px;
}
.character-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 12px;
}
.character-card__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.character-card__img-wrap {
  width: 100%;
  aspect-ratio: 1 / 1;
  background: #f5f7fa;
  margin-bottom: 8px;
  overflow: hidden;
}
.character-card__img {
  width: 100%;
  height: 100%;
}
.character-card__placeholder {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #c0c4cc;
  font-size: 12px;
}
.character-card__desc {
  font-size: 12px;
  color: #606266;
  margin: 4px 0 8px;
  min-height: 32px;
}
.media-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 12px;
}
.script-card {
  margin-bottom: 12px;
}
.script-card__desc {
  margin: 0 0 8px;
  font-size: 13px;
  color: #606266;
}
.script-card__line {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 4px 0;
  font-size: 13px;
}
.script-card__narration {
  margin: 8px 0 0;
  font-size: 13px;
  color: #909399;
}
.export-wrap {
  max-width: 480px;
  margin: 0 auto;
}
.export-video {
  width: 100%;
  border-radius: 6px;
  background: #000;
}

/* el-steps 让标题可点击 */
.studio-sider :deep(.el-step__title) {
  cursor: pointer;
}
</style>
