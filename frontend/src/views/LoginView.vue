<template>
  <div class="login-page">
    <el-card class="login-card" shadow="always">
      <div class="login-card__brand">
        <span class="login-card__logo">🎬</span>
        <h2 class="login-card__title">AI 漫剧制作平台</h2>
        <p class="login-card__subtitle">把小说变成动漫，一站式创作</p>
      </div>

      <el-tabs v-model="activeTab" stretch>
        <el-tab-pane label="登录" name="login" />
        <el-tab-pane label="注册" name="register" />
      </el-tabs>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        @submit.prevent="onSubmit"
      >
        <el-form-item label="用户名" prop="username">
          <el-input
            v-model="form.username"
            placeholder="请输入用户名"
            :prefix-icon="User"
            autocomplete="username"
          />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            :prefix-icon="Lock"
            show-password
            :autocomplete="activeTab === 'login' ? 'current-password' : 'new-password'"
          />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            class="login-card__submit"
            :loading="loading"
            @click="onSubmit"
          >
            {{ activeTab === 'login' ? '登 录' : '注 册' }}
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
// 登录 / 注册页：Tab 切换
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { Lock, User } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const activeTab = ref<'login' | 'register'>('login')
const loading = ref(false)
const formRef = ref<FormInstance>()

const form = reactive({
  username: '',
  password: ''
})

const rules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 2, max: 32, message: '用户名长度 2-32', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 64, message: '密码长度 6-64', trigger: 'blur' }
  ]
}

async function onSubmit() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    if (activeTab.value === 'register') {
      await userStore.register(form.username, form.password)
      ElMessage.success('注册成功，正在登录...')
    }
    await userStore.login(form.username, form.password)
    ElMessage.success('登录成功')
    const redirect = (route.query.redirect as string) || '/'
    router.replace(redirect)
  } catch {
    /* 拦截器已提示 */
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}
.login-card {
  width: 380px;
  border-radius: 8px;
}
.login-card__brand {
  text-align: center;
  margin-bottom: 8px;
}
.login-card__logo {
  font-size: 40px;
}
.login-card__title {
  margin: 8px 0 4px;
  font-size: 20px;
  color: #303133;
}
.login-card__subtitle {
  margin: 0 0 12px;
  font-size: 13px;
  color: #909399;
}
.login-card__submit {
  width: 100%;
}
</style>
