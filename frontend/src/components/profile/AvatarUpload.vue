<template>
  <div class="form-item form-item--wide profile-avatar-field">
    <label class="form-label">个人头像</label>
    <div class="avatar-upload-row">
      <div class="avatar-wrapper avatar-wrapper--preview">
        <img v-if="avatarUrl" :src="avatarUrl" alt="头像预览">
        <span v-else>{{ avatarInitial }}</span>
      </div>
      <label class="avatar-upload-button">
        {{ uploading ? '上传中...' : '上传头像' }}
        <input type="file" accept="image/*" :disabled="uploading" @change="onFileChange">
      </label>
    </div>
  </div>
</template>

<script setup>
defineProps({
  avatarUrl: { type: String, default: '' },
  avatarInitial: { type: String, default: '?' },
  uploading: { type: Boolean, default: false },
})
const emit = defineEmits(['upload'])

function onFileChange(e) {
  const file = e.target.files?.[0]
  if (!file) return
  emit('upload', file)
  e.target.value = ''
}
</script>

<style scoped>
.profile-avatar-field {
  border-bottom: 1px solid var(--color-border);
  padding-bottom: 16px;
}
.avatar-upload-row {
  display: flex;
  align-items: center;
  gap: 14px;
}
.avatar-wrapper {
  width: 60px;
  height: 60px;
  border-radius: var(--radius-md);
  background: var(--color-primary);
  color: var(--color-on-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  font-weight: 600;
  overflow: hidden;
}
.avatar-wrapper img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.avatar-wrapper--preview {
  flex: 0 0 auto;
}
.avatar-upload-button {
  position: relative;
  height: 34px;
  padding: 0 14px;
  border: 1px solid var(--color-primary);
  border-radius: var(--radius-md);
  color: var(--color-primary);
  background: var(--color-card);
  display: inline-flex;
  align-items: center;
  cursor: pointer;
  font-size: 13px;
}
.avatar-upload-button input {
  display: none;
}
.form-item--wide {
  grid-column: 1 / -1;
}
.form-label {
  font-size: 13px;
  font-weight: 500;
  color: var(--color-foreground);
}
</style>
