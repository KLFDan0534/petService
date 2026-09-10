<template>
  <section class="card section-card">
    <h3>我的资料</h3>
    <div class="form-grid">
      <label>昵称<input v-model="form.nickname_wsh" class="form-control"></label>
      <label class="form-grid__wide">
        keeper头像
        <div class="upload-field">
          <input ref="keeperAvatarInput" type="file" accept="image/*" style="display:none" @change="handleAvatarUpload">
          <button class="btn btn-outline btn-sm" @click.prevent="keeperAvatarInput?.click()">{{ uploading ? '上传中...' : '上传头像' }}</button>
          <span>{{ form.avatar_wsh ? '已上传头像' : '未上传头像' }}</span>
        </div>
        <div v-if="form.avatar_wsh" class="photo-preview-grid single">
          <img :src="form.avatar_wsh" alt="keeper头像预览">
        </div>
      </label>
    </div>
    <button class="btn btn-primary" @click="$emit('save', form.nickname_wsh)">保存资料</button>
  </section>
</template>

<script setup>
import { reactive, ref, watch } from 'vue'

const props = defineProps({
  profile: { type: Object, required: true },
  uploading: { type: Boolean, default: false },
})
const emit = defineEmits(['save', 'avatar-upload'])

const form = reactive({ nickname_wsh: '', avatar_wsh: '' })
watch(() => props.profile, (val) => {
  if (val) {
    form.nickname_wsh = val.nickname_wsh || ''
    form.avatar_wsh = val.avatar_wsh || ''
  }
}, { immediate: true })

const keeperAvatarInput = ref(null)

function handleAvatarUpload(event) {
  const file = event.target.files?.[0]
  if (file) {
    emit('avatar-upload', file)
    event.target.value = ''
  }
}
</script>

<style scoped>
.section-card { padding: 22px; display: grid; gap: 14px; }
.form-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 14px; }
.form-grid label { display: grid; gap: 6px; font-size: 13px; color: var(--color-muted-foreground); }
.form-grid__wide { grid-column: 1 / -1; }
.upload-field { display: flex; flex-wrap: wrap; gap: 8px; align-items: center; color: var(--color-muted-foreground); font-size: 13px; }
.photo-preview-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(88px, 1fr)); gap: 8px; }
.photo-preview-grid.single { max-width: 180px; }
.photo-preview-grid img { width: 100%; aspect-ratio: 1; object-fit: cover; border-radius: var(--radius-inline); border: 1px solid var(--color-border); background: var(--color-muted); }
</style>
