<template>
  <div>
    <PageHero title="宠物领养" subtitle="给流浪的它们一个温暖的家" />

    <div v-if="loading" class="loading">加载中...</div>

    <div v-else-if="pets.length === 0" class="empty-state">
      <div class="icon">🐾</div>
      <h3>暂无待领养宠物</h3>
      <p>请稍后再来看看</p>
    </div>

    <div v-else class="pet-grid">
      <div v-for="pet in pets" :key="pet.id_wsh" class="card" style="overflow:hidden">
        <div class="pet-image" v-if="pet.cover_image_wsh">
          <img :src="pet.cover_image_wsh" :alt="pet.name_wsh" style="width:100%;height:200px;object-fit:cover">
        </div>
        <div class="pet-image" v-else style="height:200px;background:var(--color-muted);display:flex;align-items:center;justify-content:center;font-size:48px">🐾</div>
        <div style="padding:16px">
          <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:8px">
            <h3 style="margin:0">{{ pet.name_wsh }}</h3>
            <span :class="['badge', pet.gender_wsh === '公' ? 'badge-info' : 'badge-warning']">{{ pet.gender_wsh || '未知' }}</span>
          </div>
          <div style="font-size:14px;color:var(--color-muted-foreground);margin-bottom:8px">
            <span v-if="pet.breed_wsh">{{ pet.breed_wsh }} · </span>
            <span v-if="pet.age_wsh">{{ pet.age_wsh }}个月</span>
            <span v-if="pet.weight_wsh"> · {{ pet.weight_wsh }}kg</span>
          </div>
          <p v-if="pet.personality_wsh" style="font-size:14px;color:var(--color-muted-foreground);margin-bottom:12px">{{ pet.personality_wsh }}</p>
          <div style="display:flex;gap:4px;flex-wrap:wrap;margin-bottom:12px">
            <span v-if="pet.vaccinated_wsh === 1" class="badge badge-success">已免疫</span>
            <span v-if="pet.sterilized_wsh === 1" class="badge badge-info">已绝育</span>
          </div>
          <div style="display:flex;justify-content:space-between;align-items:center">
            <span v-if="pet.adoption_fee_wsh > 0" style="font-weight:600;color:var(--color-primary)">¥{{ pet.adoption_fee_wsh }}</span>
            <span v-else style="font-weight:600;color:var(--color-success)">免费领养</span>
            <button class="btn btn-primary btn-sm" @click="applyPet(pet.id_wsh)">申请领养</button>
          </div>
        </div>
      </div>
    </div>

    <div v-if="showApplyForm" class="modal-overlay" @mousedown.self="showApplyForm = false">
      <div class="modal" style="max-width:600px">
        <h2>申请领养</h2>
        <form @submit.prevent="submitApplication">
          <div class="form-group"><label>姓名</label><input v-model="form.applicant_name_wsh" required></div>
          <div class="form-group"><label>联系电话</label><input v-model="form.applicant_phone_wsh" required></div>
          <div class="form-group"><label>居住地址</label><input v-model="form.applicant_address_wsh" required></div>
          <div class="form-group">
            <label>住房类型</label>
            <select v-model="form.housing_type_wsh">
              <option value="自有">自有</option>
              <option value="租房">租房</option>
            </select>
          </div>
          <div class="form-group">
            <label><input type="checkbox" v-model="form.has_yard_wsh"> 有院子</label>
          </div>
          <div class="form-group"><label>家庭成员</label><input v-model="form.family_members_wsh" placeholder="如：夫妻二人，一个孩子"></div>
          <div class="form-group"><label>养宠经验</label><textarea v-model="form.pet_experience_wsh" rows="2"></textarea></div>
          <div class="form-group"><label>领养理由</label><textarea v-model="form.reason_wsh" rows="3" required></textarea></div>
          <div class="form-group"><label>经济状况</label><input v-model="form.economic_condition_wsh" placeholder="如：月收入2万+"></div>
          <div class="form-group">
            <label><input type="checkbox" v-model="form.agree_visit_wsh" checked> 同意回访</label>
          </div>
          <div class="modal-actions">
            <button type="button" class="btn btn-secondary btn-sm" @click="showApplyForm = false">取消</button>
            <button type="submit" class="btn btn-primary btn-sm" :disabled="submitting">{{ submitting ? '提交中...' : '提交申请' }}</button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useAppStore } from '@/stores/app'
import { useRouter } from 'vue-router'
import PageHero from '@/components/common/PageHero.vue'

const authStore = useAuthStore()
const appStore = useAppStore()
const router = useRouter()
const pets = ref([])
const loading = ref(true)
const showApplyForm = ref(false)
const submitting = ref(false)
const selectedPetId = ref(null)

const form = ref({
  pet_id_wsh: null,
  applicant_name_wsh: '',
  applicant_phone_wsh: '',
  applicant_address_wsh: '',
  housing_type_wsh: '自有',
  has_yard_wsh: 0,
  family_members_wsh: '',
  pet_experience_wsh: '',
  reason_wsh: '',
  economic_condition_wsh: '',
  agree_visit_wsh: 1,
})

onMounted(async () => {
  try {
    const r = await authStore.apiGet('/api/adoptions/pets')
    if (r.code === 200) pets.value = r.data
  } catch (e) {}
  finally { loading.value = false }
})

function applyPet(petId) {
  if (!authStore.isLoggedIn) {
    return router.push('/login')
  }
  selectedPetId.value = petId
  form.value.pet_id_wsh = petId
  showApplyForm.value = true
}

async function submitApplication() {
  if (!authStore.isLoggedIn) {
    return router.push('/login')
  }
  submitting.value = true
  try {
    const r = await authStore.apiPost('/api/adoptions', form.value)
    if (r.code === 200) {
      appStore.addToast('申请已提交，请等待审核', 'success')
      showApplyForm.value = false
    }
  } catch (e) {
    appStore.addToast(e.response?.data?.message || '提交失败', 'error')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.pet-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 24px;
}
</style>
