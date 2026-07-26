<template>
  <div class="project-list-page">
    <!-- 顶栏 -->
    <el-header class="page-header">
      <div class="page-header__left">
        <span class="page-header__title">🎬 AI 漫剧制作平台</span>
      </div>
      <div class="page-header__right">
        <span class="page-header__user">欢迎，{{ userStore.username || '创作者' }}</span>
        <el-button text :icon="SwitchButton" @click="onLogout">退出</el-button>
      </div>
    </el-header>

    <el-main class="page-main">
      <!-- 工具栏 -->
      <div class="toolbar">
        <h3 class="toolbar__title">我的项目</h3>
        <el-button type="primary" :icon="Plus" @click="openCreate">新建项目</el-button>
      </div>

      <!-- 项目表格 -->
      <el-table :data="projects" v-loading="loading" border stripe>
        <el-table-column label="标题" prop="title" min-width="180">
          <template #default="{ row }">
            <el-link type="primary" @click="enterStudio(row.id)">{{ row.title }}</el-link>
          </template>
        </el-table-column>
        <el-table-column label="画风" width="100">
          <template #default="{ row }">{{ styleLabel(row.stylePreset) }}</template>
        </el-table-column>
        <el-table-column label="宽高比" prop="aspectRatio" width="90" />
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small">
              {{ statusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" prop="createdAt" width="180" />
        <el-table-column label="更新时间" prop="updatedAt" width="180" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" plain @click="enterStudio(row.id)">
              进入工作台
            </el-button>
            <el-popconfirm title="确定删除该项目？" @confirm="onDelete(row)">
              <template #reference>
                <el-button size="small" type="danger" plain>删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="size"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="fetchList"
          @size-change="fetchList"
        />
      </div>
    </el-main>

    <!-- 新建项目弹窗 -->
    <el-dialog v-model="createVisible" title="新建项目" width="420px">
      <el-form :model="createForm" label-width="80px">
        <el-form-item label="标题" required>
          <el-input v-model="createForm.title" placeholder="请输入项目标题" maxlength="64" />
        </el-form-item>
        <el-form-item label="画风">
          <StylePresetSelector v-model="createForm.stylePreset" />
        </el-form-item>
        <el-form-item label="宽高比">
          <el-select v-model="createForm.aspectRatio" style="width: 100%">
            <el-option label="9:16（竖屏，推荐）" value="9:16" />
            <el-option label="16:9（横屏）" value="16:9" />
            <el-option label="1:1（方形）" value="1:1" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="onCreate">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
// 项目列表页
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Plus, SwitchButton } from '@element-plus/icons-vue'
import StylePresetSelector from '@/components/StylePresetSelector.vue'
import { useUserStore } from '@/stores/user'
import * as projectApi from '@/api/project'
import { statusLabel, styleLabel } from '@/utils'
import type { Project } from '@/types'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const projects = ref<Project[]>([])
const page = ref(1)
const size = ref(10)
const total = ref(0)

const createVisible = ref(false)
const creating = ref(false)
const createForm = reactive({
  title: '',
  stylePreset: 'anime_jp',
  aspectRatio: '9:16'
})

async function fetchList() {
  loading.value = true
  try {
    const res = await projectApi.listProjects(page.value, size.value)
    projects.value = res.list || []
    total.value = res.total || 0
  } catch {
    /* 拦截器已提示 */
  } finally {
    loading.value = false
  }
}

function openCreate() {
  createForm.title = ''
  createForm.stylePreset = 'anime_jp'
  createForm.aspectRatio = '9:16'
  createVisible.value = true
}

async function onCreate() {
  if (!createForm.title.trim()) {
    ElMessage.warning('请输入项目标题')
    return
  }
  creating.value = true
  try {
    await projectApi.createProject({ ...createForm })
    ElMessage.success('创建成功')
    createVisible.value = false
    page.value = 1
    fetchList()
  } catch {
    /* 拦截器已提示 */
  } finally {
    creating.value = false
  }
}

async function onDelete(row: Project) {
  try {
    await projectApi.deleteProject(row.id)
    ElMessage.success('已删除')
    fetchList()
  } catch {
    /* 拦截器已提示 */
  }
}

function enterStudio(id: number) {
  router.push(`/studio/${id}`)
}

function onLogout() {
  userStore.logout()
  router.replace('/login')
}

function statusTagType(status: string): 'success' | 'info' | 'warning' {
  if (status === 'READY') return 'success'
  if (status === 'DRAFT') return 'info'
  return 'warning'
}

onMounted(fetchList)
</script>

<style scoped>
.project-list-page {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
}
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border-bottom: 1px solid #ebeef5;
  height: 56px;
  padding: 0 24px;
}
.page-header__title {
  font-size: 18px;
  font-weight: 600;
}
.page-header__right {
  display: flex;
  align-items: center;
  gap: 8px;
}
.page-header__user {
  font-size: 14px;
  color: #606266;
}
.page-main {
  flex: 1;
  padding: 24px;
  max-width: 1200px;
  width: 100%;
  margin: 0 auto;
}
.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}
.toolbar__title {
  margin: 0;
}
.pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
