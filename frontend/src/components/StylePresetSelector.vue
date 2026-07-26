<template>
  <!-- 画风选择下拉，v-model 双向绑定 stylePreset key -->
  <el-select
    :model-value="modelValue"
    :placeholder="placeholder"
    :disabled="disabled"
    style="width: 100%"
    @update:model-value="onChange"
  >
    <el-option v-for="opt in options" :key="opt.key" :label="opt.label" :value="opt.key" />
  </el-select>
</template>

<script setup lang="ts">
// 画风预设选择器，选项与后端 StylePresets 对齐
import { STYLE_PRESETS } from '@/utils'

defineProps<{
  modelValue: string | null | undefined
  placeholder?: string
  disabled?: boolean
}>()

const emit = defineEmits<{ 'update:modelValue': [value: string] }>()
const options = STYLE_PRESETS

function onChange(val: string) {
  emit('update:modelValue', val)
}
</script>
