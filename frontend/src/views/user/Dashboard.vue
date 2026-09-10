<template>
  <div class="db">
    <!-- ═══════════════════════════════════════════
         HERO — 2-column grid, canvas background
         ═══════════════════════════════════════════ -->
    <section id="top" class="hero-section">
      <div class="shell">
        <div class="hero-grid">
          <!-- Left: copy -->
          <div class="hero-left">
            <Reveal :delay="0.05">
              <div class="hero-pill">
                <span class="pill-dot"></span>
                <span class="pill-dot-inner"></span>
                <span class="pill-text">今日门店在营 · 夜间值守中</span>
              </div>
            </Reveal>

            <h1 class="hero-title">
              <Reveal tag="span" :y="30" :delay="0.1"><span class="hero-line">每一次托付，</span></Reveal>
              <Reveal tag="span" :y="30" :delay="0.17"><span class="hero-line">都被认真照护。</span></Reveal>
            </h1>

            <Reveal :delay="0.28">
              <p class="hero-desc">
                从入住健康核验到每日照护日报，{{ serviceCountDisplay }} 套寄养与照护方案，
                由实名认证门店与照护师共同完成。你出门的每一天，都能看见它的真实状态。
              </p>
            </Reveal>

            <Reveal :delay="0.34">
              <div class="hero-ctas">
                <button class="cta cta-primary" type="button" @click="startBooking">
                  立即预约寄养
                  <span class="cta-arrow">→</span>
                </button>
                <button class="cta cta-outline" type="button" @click="scrollTo('#services')">
                  浏览照护方案
                </button>
              </div>
            </Reveal>

            <Reveal :delay="0.42">
              <dl class="hero-stats">
                <div>
                  <dd class="stat-val">{{ serviceCountDisplay }}</dd>
                  <dt class="stat-label">在售照护方案</dt>
                </div>
                <div>
                  <dd class="stat-val">{{ providerCountDisplay }}</dd>
                  <dt class="stat-label">认证门店与照护师</dt>
                </div>
                <div>
                  <dd class="stat-val">{{ averageRatingDisplay }}</dd>
                  <dt class="stat-label">家长综合评分</dt>
                </div>
              </dl>
            </Reveal>
          </div>

          <!-- Right: images -->
          <div class="hero-right">
            <Reveal :delay="0.16" :y="20">
              <div class="hero-image-wrap">
              <div
                class="hero-main-img"
                @mousemove="onHeroPointerMove"
                @mouseleave="onHeroPointerLeave"
              >
                <div
                  class="hero-img-inner"
                  :style="{ transform: `translate(${heroShiftX}px, ${heroShiftY}px) scale(${heroScale})` }"
                >
                  <MediaWithFallback
                    :src="heroImage"
                    alt="宠物寄养套房内正在休息的宠物"
                    loading="eager"
                  />
                </div>
              </div>

              <!-- keeper overlay card -->
              <div class="hero-keeper-card" v-if="providers.length">
                <div class="keeper-avatar-sm">
                  <MediaWithFallback
                    v-if="providers[0]?.avatar_wsh"
                    :src="providers[0].avatar_wsh"
                    :alt="`${providers[0].name_wsh}的照片`"
                  />
                  <span v-else class="keeper-avatar-placeholder"></span>
                </div>
                <div class="keeper-card-text">
                  <p class="kc-label">今日值守照护师</p>
                  <p class="kc-name">
                    {{ providers[0]?.name_wsh ?? '值守中' }}
                    <span class="kc-level">{{ providers[0]?.level_wsh ?? '实名认证' }}</span>
                  </p>
                </div>
              </div>

              <!-- small detail image -->
              <div class="hero-detail-img" v-if="detailImage">
                <MediaWithFallback :src="detailImage" alt="家长与宠物相处的细节" />
              </div>
            </div>
            </Reveal>
          </div>
        </div>
      </div>
    </section>

    <!-- ═══════════════════════════════════════════
         01 · 照护方案 — Featured + Stack list
         ═══════════════════════════════════════════ -->
    <section id="services" class="section">
      <div class="shell">
        <SectionHeading
          index="01"
          label="照护方案"
          title="按毛孩子的节奏，选择合适的照护方式"
          description="寄养、洗护、上门陪伴与行为训练由同一套照护标准贯穿，价格与档期实时同步门店。"
        >
          <template #action>
            <TextLink @click="$router.push('/services')">查看全部方案</TextLink>
          </template>
        </SectionHeading>

        <Reveal>
        <!-- loading -->
        <div v-if="loading" class="svc-skeleton">
          <div class="sk-featured"></div>
          <div class="sk-stack">
            <div v-for="i in 4" :key="i" class="sk-row"></div>
          </div>
        </div>

        <!-- empty -->
        <p v-else-if="!services.length" class="empty-dashed">
          暂无可预约的照护方案，请稍后再试或直接联系门店。
        </p>

        <!-- loaded: featured + stack -->
        <div v-else-if="services.length" class="svc-layout">
          <!-- Featured card -->
          <div class="svc-featured">
            <article class="feature-card">
              <button
                class="feature-btn"
                type="button"
                :aria-label="`查看 ${featuredServices[0]?.name_wsh} 详情`"
                @click="goDetail(featuredServices[0]?.id_wsh)"
              >
                <div class="feature-img-wrap">
                  <MediaWithFallback
                    :src="featuredServices[0]?.firstImage"
                    :fallback-src="localFallbackForService(featuredServices[0])"
                    :alt="featuredServices[0]?.name_wsh"
                    class="feature-img"
                  />
                  <span class="feature-badge">
                    <el-icon><component :is="serviceIcon(featuredServices[0])" /></el-icon>
                    {{ categoryLabel(featuredServices[0]) }}
                  </span>
                </div>
                <div class="feature-body">
                  <h3 class="feature-name">{{ featuredServices[0]?.name_wsh }}</h3>
                  <p class="feature-desc">{{ featuredServices[0]?.description_wsh || '门店照护师全程跟进，入住流程与状态记录可随时查看。' }}</p>
                  <div class="feature-footer">
                    <p class="feature-price">
                      <span class="fp-unit">¥</span>
                      <span class="fp-val">{{ formatMoney(featuredServices[0]?.price_wsh) }}</span>
                      <span class="fp-per">/ {{ unitLabel(featuredServices[0]?.unit_wsh) }}</span>
                    </p>
                    <span class="feature-link">
                      <span v-if="featuredServices[0]?.distance_m_wsh" class="feature-dist">
                        <el-icon><Location /></el-icon>
                        {{ formatServiceDistance(featuredServices[0]?.distance_m_wsh) }}
                      </span>
                      <span class="link-text">
                        查看详情
                        <span class="link-arrow">↗</span>
                      </span>
                    </span>
                  </div>
                </div>
              </button>
            </article>
          </div>

          <!-- Stack list -->
          <div class="svc-stack">
            <button
              v-for="svc in featuredServices.slice(1)"
              :key="svc.id_wsh"
              class="service-row"
              type="button"
              :aria-label="`查看 ${svc.name_wsh} 详情`"
              @click="goDetail(svc.id_wsh)"
            >
              <span class="sr-thumb">
                <MediaWithFallback
                  :src="svc.firstImage"
                  :fallback-src="localFallbackForService(svc)"
                  :alt="svc.name_wsh"
                />
              </span>
              <span class="sr-info">
                <span class="sr-name">{{ svc.name_wsh }}</span>
                <span class="sr-meta">
                  <el-icon><component :is="serviceIcon(svc)" /></el-icon>
                  {{ categoryLabel(svc) }}
                  <template v-if="svc.distance_m_wsh">
                    <span class="sr-sep"></span>
                    {{ formatServiceDistance(svc.distance_m_wsh) }}
                  </template>
                </span>
              </span>
              <span class="sr-price">
                <span class="sr-price-val">¥{{ formatMoney(svc.price_wsh) }}</span>
                <span class="sr-price-unit">/ {{ unitLabel(svc.unit_wsh) }}</span>
              </span>
              <span class="sr-arrow">↗</span>
            </button>
          </div>
        </div>
        </Reveal>
      </div>
    </section>

    <!-- ═══════════════════════════════════════════
         02 · 照护现场 — uneven photo gallery
         ═══════════════════════════════════════════ -->
    <section ref="galleryRef" class="gallery-band" aria-label="照护现场">
      <div class="shell">
        <Reveal>
        <div class="gb-head">
          <div class="gb-eyebrow">
            <span class="gb-line"></span>
            <h2 class="gb-title">照护现场</h2>
          </div>
          <p class="gb-desc">
            实拍来自各门店的套房、洗护间与活动场，未做布景与后期修饰。
          </p>
        </div>

        <div v-if="loading" class="gb-grid gb-grid-loading">
          <div
            v-for="i in 6"
            :key="i"
            class="gb-tile gb-loading"
            :class="galleryLayout[(i - 1) % 6].span"
          ></div>
        </div>

        <div v-else-if="galleryImages.length" class="gb-grid">
          <figure
            v-for="(img, index) in galleryImages"
            :key="img.key"
            class="gb-tile"
            :class="[galleryLayout[index % galleryLayout.length].span, galleryLayout[index % galleryLayout.length].aspect]"
            :style="galleryTileStyle(galleryLayout[index % galleryLayout.length].shift)"
          >
            <MediaWithFallback
              :src="img.url"
              :alt="`${img.service?.name_wsh || '门店'} 现场照片`"
              class="gb-media"
            />
            <figcaption class="gb-caption">
              {{ img.service?.name_wsh || '门店现场' }}
              <span class="gb-caption-cat">{{ categoryLabel(img.service) }}</span>
            </figcaption>
          </figure>
        </div>
        </Reveal>
      </div>
    </section>

    <!-- ═══════════════════════════════════════════
         03 · 门店与照护师
         ═══════════════════════════════════════════ -->
    <section id="providers" class="section section-tinted">
      <div class="shell">
        <SectionHeading
          index="02"
          label="门店与照护师"
          title="认识正在照顾它的那个人"
          description="每一位照护师都完成实名认证与急救培训，门店资质、在护数量与值守排班对家长公开。"
        />

        <Reveal>
        <!-- Merchant rows -->
        <div class="merchant-table">
          <div
            v-for="(m, idx) in merchants.slice(0, 3)"
            :key="m.id_wsh"
            class="mt-row"
          >
            <span class="mt-idx">0{{ idx + 1 }}</span>
            <span class="mt-main">
              <span class="mt-name">{{ m.name_wsh || '服务商家' }}</span>
              <span class="mt-addr">
                <el-icon><Location /></el-icon>
                {{ m.address_wsh || '地址待补充' }}
              </span>
            </span>
            <span class="mt-desc">{{ m.description_wsh || '提供寄养、洗护与上门陪伴服务' }}</span>
            <span class="mt-side">
              <span v-if="m.distance_m_wsh" class="mt-dist">{{ formatServiceDistance(m.distance_m_wsh) }}</span>
              <span v-if="m.phone_wsh" class="mt-phone">
                <el-icon><Phone /></el-icon>
                {{ m.phone_wsh }}
              </span>
              <span class="mt-arrow">↗</span>
            </span>
          </div>
        </div>

        <!-- Keepers -->
        <div class="keeper-section-head">
          <h3 class="sub-heading">值守中的照护师</h3>
          <p class="sub-heading-right">共 <span>{{ loading ? '--' : providers.length }}</span> 位</p>
        </div>

        <div class="keeper-grid">
          <div
            v-for="keeper in providers.slice(0, 4)"
            :key="keeper.id_wsh"
            class="keeper-card"
          >
            <div class="kc-img">
              <MediaWithFallback
                :src="keeper.avatar_wsh"
                :alt="`${keeper.name_wsh}的照片`"
                :placeholder="keeper.name_wsh"
              />
              <div class="kc-name-plate">
                <p class="kc-name-text">{{ keeper.name_wsh || '照护师' }}</p>
                <p class="kc-cert-text">{{ keeper.level_wsh || '实名认证' }}</p>
              </div>
              <div class="kc-overlay">
                <p class="kc-overlay-name">{{ keeper.name_wsh || '照护师' }}</p>
                <p class="kc-overlay-level">{{ keeper.level_wsh || '实名认证' }}</p>
                <p class="kc-overlay-intro">{{ keeper.intro_wsh || '负责入住适应、日常照护与状态记录。' }}</p>
                <p class="kc-overlay-meta">
                  <span v-if="keeper.years_wsh">{{ keeper.years_wsh }} 年经验</span>
                  <template v-if="keeper.years_wsh && keeper.service_count_wsh">
                    <span class="meta-sep"></span>
                  </template>
                  <span v-if="keeper.service_count_wsh">{{ keeper.service_count_wsh.toLocaleString() }} 次照护</span>
                </p>
              </div>
            </div>
          </div>
        </div>

        <p v-if="!loading && !providers.length" class="empty-dashed">
          照护师排班加载中，请稍后查看。
        </p>
        </Reveal>
      </div>
    </section>

    <!-- ═══════════════════════════════════════════
         04 · 安心保障 — fixed dark paper section
         ═══════════════════════════════════════════ -->
    <section id="trust" class="section section-paper">
      <div class="shell">
        <Reveal>
        <div class="trust-grid">
          <div class="trust-left">
            <SectionHeading
              index="03"
              label="安心保障"
              tone="dark"
              title="把担心交给流程，而不是运气。"
              description="寄养最难的不是环境，而是看不见的那几天。我们把每一个环节都写成可核查的记录。"
            />
            <button class="cta cta-light" type="button" @click="startBooking">
              查看可预约档期
              <span class="cta-arrow">→</span>
            </button>
          </div>

          <div class="trust-right">
            <div v-for="(g, idx) in guarantees" :key="g.title" class="guarantee" :class="{ 'guarantee-left': idx % 2 === 1 }">
              <div class="guarantee-icon">
                <el-icon :size="20"><component :is="g.icon" /></el-icon>
              </div>
              <h3>{{ g.title }}</h3>
              <p>{{ g.body }}</p>
            </div>
          </div>
          </div>
        </Reveal>
      </div>
    </section>

    <!-- ═══════════════════════════════════════════
         05 · 照护流程 — horizontal rail
         ═══════════════════════════════════════════ -->
    <section id="process" class="section">
      <div class="shell">
        <SectionHeading
          index="04"
          label="照护流程"
          title="从预约到接回，四步走完"
          description="每一步都有对应的负责人和时间承诺，不需要家长反复追问进度。"
        />

        <Reveal>
        <div class="process-wrap">
          <div class="process-rail" aria-hidden="true"></div>
          <div class="process-grid">
            <div v-for="step in processSteps" :key="step.index" class="process-step">
              <span class="ps-dot" aria-hidden="true"></span>
              <span class="ps-num">{{ step.index }}</span>
              <h3 class="ps-title">{{ step.title }}</h3>
              <p class="ps-body">{{ step.body }}</p>
            </div>
          </div>
        </div>
        </Reveal>
      </div>
    </section>

    <!-- ═══════════════════════════════════════════
         06 · 家长评价 — featured + supporting
         ═══════════════════════════════════════════ -->
    <section id="testimonials" class="section section-tinted">
      <div class="shell">
        <SectionHeading
          index="05"
          label="家长评价"
          title="接回它的那一刻，才是真正的评分"
          description="评价来自完成寄养的真实订单，按时间倒序展示，不做筛选与置顶。"
        >
          <template #action>
            <div class="rating-display">
              <p class="rating-val">{{ loading ? '--' : averageRatingDisplay }}</p>
              <p class="rating-sub">综合评分 / 5.0</p>
            </div>
          </template>
        </SectionHeading>

        <Reveal>
        <!-- loading -->
        <div v-if="testimonialsLoading" class="testi-skeleton">
          <div class="sk-card-lg"></div>
          <div class="testi-stack">
            <div class="sk-card-sm"></div>
            <div class="sk-card-sm"></div>
          </div>
        </div>

        <!-- empty -->
        <p v-else-if="!testimonials.length" class="empty-dashed">
          还没有评价。完成第一次寄养后，你的记录会出现在这里。
        </p>

        <!-- loaded -->
        <div v-else-if="testimonials.length" class="testi-layout">
          <!-- Featured (first testimonial) -->
          <figure class="testi-featured">
            <div>
              <div class="stars-row">
                <span v-for="s in 5" :key="s" class="star" :class="{ 'star-fill': s <= testimonials[0].score_wsh }">★</span>
              </div>
              <blockquote class="tq-big">"{{ testimonials[0].content_wsh }}"</blockquote>
            </div>
            <figcaption class="t-fig">
              <span class="t-author">{{ testimonials[0].targetLabel || '平台服务' }}</span>
              <span class="t-sep"></span>
              <span class="t-date">{{ formatDate(testimonials[0].created_at_wsh) }}</span>
              <span class="t-badge">已完成订单评价</span>
            </figcaption>
          </figure>

          <!-- Supporting -->
          <div class="testi-stack">
            <figure
              v-for="item in testimonials.slice(1, 3)"
              :key="item.key ?? item.id_wsh"
              class="testi-card"
            >
              <div>
                <div class="stars-row stars-sm">
                  <span v-for="s in 5" :key="s" class="star star-sm" :class="{ 'star-fill': s <= item.score_wsh }">★</span>
                </div>
                <blockquote class="tq-sm">{{ item.content_wsh }}</blockquote>
              </div>
              <figcaption class="t-fig-sm">
                <span>{{ item.targetLabel || '平台服务' }}</span>
                <span class="t-sep"></span>
                <span>{{ formatDate(item.created_at_wsh) }}</span>
              </figcaption>
            </figure>
          </div>
          </div>
        </Reveal>
      </div>
    </section>

    <!-- ═══════════════════════════════════════════
         07 · 预约 — canvas section + dark rounded card
         ═══════════════════════════════════════════ -->
    <section id="booking" class="section">
      <div class="shell">
        <Reveal>
        <div class="booking-card">
          <div class="booking-form-side">
            <p class="bk-label">开始预约</p>
            <h2 class="bk-title">
              先占住档期，<br>细节稍后再确认。
            </h2>
            <p class="bk-desc">
              选择方案与预计入住日期，门店会在 30 分钟内联系你核对宠物信息与健康记录。
            </p>

            <form class="bk-form" @submit.prevent="startBooking">
              <div class="bk-field">
                <label for="bk-svc" class="bk-f-label">照护方案</label>
                <div class="bk-select-wrap">
                  <select id="bk-svc" v-model="selectedServiceId" class="bk-select" :disabled="loading || !services.length">
                    <option value="" disabled>请选择服务方案</option>
                    <option v-for="svc in services" :key="svc.id_wsh" :value="String(svc.id_wsh)">{{ svc.name_wsh }}</option>
                  </select>
                  <span class="bk-chevron">▾</span>
                </div>
              </div>

              <div class="bk-field">
                <label for="bk-date" class="bk-f-label">预计入住日期</label>
                <input id="bk-date" v-model="checkInDate" type="date" class="bk-date" :min="minDate" />
              </div>

              <div class="bk-price-row">
                <p v-if="selectedService" class="bk-price">
                  <span class="bk-p-label">预计</span>
                  <span class="bk-p-val">¥{{ formatMoney(selectedService.price_wsh) }}</span>
                  <span class="bk-p-unit">/ {{ unitLabel(selectedService.unit_wsh) }}</span>
                </p>
                <p v-else class="bk-price bk-price-empty">选择方案后显示价格</p>
                <button class="cta cta-primary" type="submit" :disabled="!selectedService">
                  开始预约 →
                </button>
              </div>
            </form>

            <ul class="bk-assurances">
              <li v-for="item in bookingAssurances" :key="item">
                <el-icon><CircleCheckFilled /></el-icon>
                {{ item }}
              </li>
            </ul>
          </div>

          <div class="booking-image-side">
            <MediaWithFallback
              :src="bookingImage"
              alt="门店照护环境"
            />
          </div>
        </div>
        </Reveal>
      </div>
    </section>

    <!-- ═══════════════════════════════════════════
         FOOTER — light canvas footer
         ═══════════════════════════════════════════ -->
    <footer class="site-footer">
      <div class="shell">
        <div class="footer-grid">
          <div class="footer-brand">
            <div class="footer-logo-row">
              <span class="footer-logo-mark">🐾</span>
              <span class="footer-logo-text">
                <span class="footer-name">宠物寄养平台</span>
                <span class="footer-en">寄养服务</span>
              </span>
            </div>
            <p class="footer-tagline">为您的毛孩子提供一个安全、贴心的寄养之家。</p>
            <ul class="footer-contact">
              <li><el-icon><ChatDotRound /></el-icon> support@petboarding.com</li>
              <li><el-icon><Phone /></el-icon> 400-000-0000</li>
            </ul>
          </div>

          <nav v-for="col in footerColumns" :key="col.title" class="footer-col" :aria-label="col.title">
            <h4 class="footer-heading">{{ col.title }}</h4>
            <ul class="footer-list">
              <li v-for="link in col.links" :key="link.label">
                <router-link v-if="link.to" :to="link.to">{{ link.label }}</router-link>
                <a v-else :href="link.href || '#top'">{{ link.label }}</a>
              </li>
            </ul>
          </nav>
        </div>
        <div class="footer-bottom">
          <p>© {{ currentYear }} 宠物寄养平台 版权所有</p>
          <div class="footer-legal">
            <a href="#top">服务条款</a>
            <a href="#top">隐私政策</a>
          </div>
        </div>
      </div>
    </footer>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import {
  ChatDotRound,
  CircleCheckFilled,
  Clock,
  FirstAidKit,
  House,
  Location,
  Medal,
  Notebook,
  Phone,
  Scissor,
  Service,
  Stamp,
  StarFilled,
  View,
} from '@element-plus/icons-vue'
import { getKeepers } from '@/api/keeper'
import { getMerchants } from '@/api/merchant'
import { getRatings } from '@/api/rating'
import { useAppStore } from '@/stores/app'
import request from '@/utils/request'
import MediaWithFallback from '@/components/common/MediaWithFallback.vue'
import Reveal from '@/components/common/Reveal.vue'
import {
  BOOKING_IMAGE,
  DETAIL_IMAGE,
  GALLERY_IMAGES,
  HERO_IMAGE,
  KEEPER_AVATARS,
  coverForService,
  localFallbackForService,
} from '@/data/localPhotos'
import SectionHeading from '@/components/dashboard/SectionHeading.vue'
import TextLink from '@/components/dashboard/TextLink.vue'
import { unitLabel } from '@/domain/BookingUnit'
import { useServiceDistance, formatServiceDistance } from '@/composables/useServiceDistance'

const router = useRouter()
const appStore = useAppStore()
const { attachDistances } = useServiceDistance()

/* ── hero parallax ── */
const heroShiftX = ref(0)
const heroShiftY = ref(0)
const heroScale = ref(1)

function onHeroPointerMove(e) {
  if (window.matchMedia?.('(pointer: coarse)').matches) return
  const rect = e.currentTarget.getBoundingClientRect()
  const px = (e.clientX - rect.left) / rect.width - 0.5
  const py = (e.clientY - rect.top) / rect.height - 0.5
  heroShiftX.value = px * 14
  heroShiftY.value = py * 14
  heroScale.value = 1.04
}

function onHeroPointerLeave() {
  heroShiftX.value = 0
  heroShiftY.value = 0
  heroScale.value = 1
}

/* ── gallery parallax ── */
const galleryRef = ref(null)
const galleryProgress = ref(0)
let galleryRafId = null
const prefersReducedMotion = () =>
  window.matchMedia?.('(prefers-reduced-motion: reduce)').matches ?? false

const GALLERY_LAYOUT = [
  { span: 'gb-span-5', aspect: 'gb-aspect-45', shift: -34 },
  { span: 'gb-span-4', aspect: 'gb-aspect-43', shift: 18 },
  { span: 'gb-span-3', aspect: 'gb-aspect-34', shift: -22 },
  { span: 'gb-span-4', aspect: 'gb-aspect-43', shift: 24 },
  { span: 'gb-span-5', aspect: 'gb-aspect-1610', shift: -16 },
  { span: 'gb-span-3', aspect: 'gb-aspect-square', shift: 28 },
]

function updateGalleryProgress() {
  const el = galleryRef.value
  if (!el) return
  if (prefersReducedMotion()) {
    galleryProgress.value = 0
    return
  }
  const rect = el.getBoundingClientRect()
  const vh = window.innerHeight || 1
  const span = rect.height + vh
  galleryProgress.value = span > 0 ? Math.min(1, Math.max(0, (vh - rect.top) / span)) : 0
}

function onGalleryScroll() {
  if (galleryRafId) return
  galleryRafId = requestAnimationFrame(() => {
    galleryRafId = null
    updateGalleryProgress()
  })
}

function galleryTileStyle(shift) {
  if (!shift || prefersReducedMotion()) return undefined
  return { transform: `translateY(${(galleryProgress.value * shift).toFixed(1)}px)` }
}

/* ── state ── */
const services = ref([])
const testimonials = ref([])
const providers = ref([])
const merchants = ref([])
const loading = ref(true)
const testimonialsLoading = ref(true)
const selectedServiceId = ref('')
const checkInDate = ref(tomorrowISO())
const minDate = new Date().toISOString().slice(0, 10)

function tomorrowISO() {
  const date = new Date()
  date.setDate(date.getDate() + 1)
  return date.toISOString().slice(0, 10)
}

/* ── constants ── */

const guarantees = [
  { icon: Stamp, title: '实名认证与背景审核', body: '照护师需通过身份核验、从业经历审查与犬猫急救培训，资质在门店页公开可查。' },
  { icon: View, title: '24 小时值守与监控回看', body: '夜间值守不断档，家长可申请寄养期间任意时段的监控回看。' },
  { icon: Notebook, title: '每日照护日报', body: '进食、排便、活动与情绪逐项记录，附当日实拍照片，出门也能看见它的状态。' },
  { icon: FirstAidKit, title: '意外医疗保障', body: '寄养期间意外伤病由平台先行垫付，驻店兽医 15 分钟内响应并同步家长。' },
]

const processSteps = [
  { index: '01', title: '选择方案与门店', body: '按距离、价格与服务类型筛选，方案详情写明照护标准与可接收体型。' },
  { index: '02', title: '预约与健康核验', body: '提交预约后门店 30 分钟内确认，入住前完成疫苗、体检与用药记录核对。' },
  { index: '03', title: '入住与每日日报', body: '照护师负责适应期陪伴，每天推送日报与照片，异常状况即时电话同步。' },
  { index: '04', title: '接回与回访评价', body: '离店时交付照护记录与洗护建议，48 小时内回访状态并邀请评价。' },
]

const bookingAssurances = [
  '入住前 24 小时可免费取消',
  '支持先到店参观再确认',
  '平台资金托管，离店后结算',
]

const footerColumns = [
  {
    title: '照护服务',
    links: [
      { label: '标准寄养', href: '#services' },
      { label: '品质寄养套房', href: '#services' },
      { label: '美容护理', href: '#services' },
      { label: '遛宠陪伴', href: '#services' },
      { label: '行为训练', href: '#services' },
    ],
  },
  {
    title: '平台',
    links: [
      { label: '门店资质', href: '#providers' },
      { label: '照护师认证标准', href: '#providers' },
      { label: '照护日报说明', href: '#services' },
      { label: '成为合作门店', href: '#services' },
    ],
  },
  {
    title: '帮助',
    links: [
      { label: '预约与取消规则', href: '#services' },
      { label: '入住材料清单', href: '#services' },
      { label: '意外医疗保障', href: '#trust' },
      { label: '联系客服', href: 'mailto:support@petboarding.com' },
    ],
  },
  {
    title: '我的账户',
    links: [
      { label: '个人中心', to: '/profile' },
      { label: '账户与安全', to: '/profile#account' },
      { label: '我的订单', to: '/orders' },
      { label: '全部服务', to: '/services' },
    ],
  },
]

const serviceTypeMap = {
  BOARDING_STANDARD: '标准寄养',
  BOARDING_VIP: '品质寄养',
  GROOMING_BASIC: '美容护理',
  TRAINING_BASIC: '行为训练',
  WALK_STANDARD: '遛宠陪伴',
  MEDICAL_CHECKUP: '健康护理',
}

const iconMap = {
  BOARDING_STANDARD: House,
  BOARDING_VIP: StarFilled,
  GROOMING_BASIC: Scissor,
  TRAINING_BASIC: Medal,
  WALK_STANDARD: Location,
  MEDICAL_CHECKUP: FirstAidKit,
}

/* ── computed ── */

const featuredServices = computed(() => services.value.slice(0, 6))

const galleryLayout = GALLERY_LAYOUT

const galleryImages = computed(() =>
  GALLERY_IMAGES.map((url, index) => ({ key: `local-gallery-${index}`, service: services.value[index] || null, url }))
)

const heroImage = computed(() => galleryImages.value[0]?.url || HERO_IMAGE)

const detailImage = computed(() => DETAIL_IMAGE)

const bookingImage = computed(() => BOOKING_IMAGE)

const selectedService = computed(() =>
  services.value.find(s => String(s.id_wsh) === String(selectedServiceId.value)) || null
)

const serviceCountDisplay = computed(() => loading.value ? '--' : String(services.value.length))
const providerCountDisplay = computed(() => testimonialsLoading.value ? '--' : String(providers.value.length + merchants.value.length))
const averageRatingDisplay = computed(() => {
  const scores = testimonials.value.map(t => Number(t.score_wsh)).filter(Number.isFinite)
  if (!scores.length) return '暂无'
  return (scores.reduce((a, b) => a + b, 0) / scores.length).toFixed(1)
})

const currentYear = new Date().getFullYear()

/* ── helpers ── */

function normalizeList(data) {
  if (Array.isArray(data)) return data
  if (Array.isArray(data?.list)) return data.list
  return []
}

function parseImageUrls(value) {
  return String(value || '').split(',').map(u => u.trim()).filter(Boolean)
}

function hydrateServices(data) {
  services.value = normalizeList(data).map(svc => {
    const first = coverForService(svc)
    return { ...svc, imageUrls: [first], firstImage: first }
  })
  if (!selectedServiceId.value && services.value.length) {
    selectedServiceId.value = String(services.value[0].id_wsh)
  }
}

const normType = svc => svc?.type_wsh || ''
function categoryLabel(svc) {
  return svc?.category_name_wsh || serviceTypeMap[normType(svc)] || '照护服务'
}
function serviceIcon(svc) {
  return iconMap[normType(svc)] || Service
}
function formatMoney(value) {
  const n = Number(value)
  return Number.isFinite(n) ? n.toLocaleString('zh-CN', { maximumFractionDigits: 2 }) : '--'
}
function formatDate(value) {
  if (!value) return ''
  const d = new Date(value)
  return Number.isNaN(d.getTime()) ? '' : d.toLocaleDateString('zh-CN')
}

/* ── nav ── */

function goDetail(id) { if (id) router.push(`/services/${id}`) }
function createOrder(svc) {
  if (!svc?.id_wsh) return
  router.push({ path: `/services/${svc.id_wsh}`, query: { book: '1' } })
}
function startBooking() {
  if (selectedService.value) { createOrder(selectedService.value); return }
  scrollTo('#services')
}
function scrollTo(sel) {
  const el = document.querySelector(sel)
  if (!el) return
  el.scrollIntoView({ behavior: window.matchMedia?.('(prefers-reduced-motion: reduce)').matches ? 'auto' : 'smooth', block: 'start' })
}

/* ── data ── */

async function loadDashboard() {
  loading.value = true
  try {
    const res = await request.get('/services')
    if (res.data.code === 200) hydrateServices(res.data.data)
  } catch {
    services.value = []
    appStore.addToast('加载服务方案失败，请稍后重试', 'error')
  } finally { loading.value = false }
  void loadCommunity()
}

async function loadCommunity() {
  testimonialsLoading.value = true
  try {
    const [merchantRes, keeperRes] = await Promise.all([getMerchants(), getKeepers()])
    merchants.value = normalizeList(merchantRes?.data)
    providers.value = normalizeList(keeperRes?.data).map((k, i) => ({
      ...k,
      avatar_wsh: KEEPER_AVATARS[i % KEEPER_AVATARS.length],
    }))
    void attachDistances(services.value, merchants.value)

    const merchantIds = new Set(services.value.map(s => s.merchant_id_wsh).filter(Boolean))
    const targets = [
      ...merchants.value.filter(m => !merchantIds.size || merchantIds.has(m.id_wsh)).slice(0, 3)
        .map(m => ({ id: m.id_wsh, type: 'merchant', label: m.name_wsh || '服务商家' })),
      ...providers.value.slice(0, 3)
        .map(k => ({ id: k.id_wsh, type: 'keeper', label: k.name_wsh || '专业看护伙伴' })),
    ]

    const results = await Promise.allSettled(targets.map(async t => {
      const res = await getRatings({ targetId: t.id, targetType: t.type })
      if (res?.code !== 200) return []
      return normalizeList(res.data).map(r => ({ ...r, targetLabel: t.label, key: `${t.type}-${r.id_wsh}` }))
    }))

    testimonials.value = results
      .flatMap(r => r.status === 'fulfilled' ? r.value : [])
      .filter(r => r.content_wsh && Number(r.score_wsh) > 0)
      .sort((a, b) => new Date(b.created_at_wsh || 0) - new Date(a.created_at_wsh || 0))
      .slice(0, 3)
  } catch {
    testimonials.value = []
    providers.value = []
    merchants.value = []
  } finally { testimonialsLoading.value = false }
}

/* ── lifecycle ── */

watch(() => services.value.length, () => {
  if (!selectedServiceId.value && services.value.length) {
    selectedServiceId.value = String(services.value[0].id_wsh)
  }
})

onMounted(() => {
  void loadDashboard()
  window.addEventListener('scroll', onGalleryScroll, { passive: true })
  window.addEventListener('resize', onGalleryScroll, { passive: true })
  updateGalleryProgress()
})

onUnmounted(() => {
  window.removeEventListener('scroll', onGalleryScroll)
  window.removeEventListener('resize', onGalleryScroll)
  if (galleryRafId) cancelAnimationFrame(galleryRafId)
})
</script>

<style scoped>
/* ═══════════════════════════════════════════════════════
   Uses shared --ref-* tokens from assets/css/design-tokens.css.
   Dark mode is handled globally via html[data-theme="dark"].
   Intentional dark "paper" sections use a fixed #17130f slab
   (same in both themes, matching the reference).
   ═══════════════════════════════════════════════════════ */
.db {
  --paper: #17130f;
  --cream-fixed: #f5efe7;
  --r-tag: 3px;
  --r-btn: 10px;
  --r-card: 14px;
  --r-panel: 20px;
  --r-frame: 26px;
  font-family: 'Inter', 'Noto Serif SC', system-ui, sans-serif;
  color: var(--ref-ink);
  background: var(--ref-canvas);
  -webkit-font-smoothing: antialiased;
  min-height: 100vh;
}

/* ═══ LAYOUT ═══ */
.shell {
  width: min(78rem, 100% - 40px);
  margin: 0 auto;
}
.section { padding: 80px 0; }
.section-tinted { background: color-mix(in srgb, var(--ref-cream) 45%, var(--ref-canvas)); }
.section-paper { background: var(--paper); color: var(--cream-fixed); }

/* ═══ SectionHeading (inline) ═══ */
.sh-wrap {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  justify-content: space-between;
  gap: 40px 24px;
  margin-bottom: 48px;
}
.sh-left { max-width: 640px; }
.sh-meta {
  display: flex;
  align-items: center;
  gap: 12px;
}
.sh-idx {
  font-size: 12px;
  font-variant-numeric: tabular-nums;
  color: var(--ref-muted);
}
.sh-line {
  width: 24px;
  height: 1px;
  background: var(--ref-line);
}
.sh-lbl {
  font-size: 13px;
  font-weight: 500;
  color: var(--ref-ink-soft);
}
.section-paper .sh-idx,
.section-paper .sh-lbl { color: rgba(255, 255, 255, 0.45); }
.section-paper .sh-line { background: rgba(255, 255, 255, 0.2); }
.sh-title {
  margin-top: 16px;
  font-family: var(--ref-font-display);
  font-size: clamp(28px, 3.4vw, 42px);
  font-weight: 400;
  line-height: 1.15;
  letter-spacing: -0.045em;
  text-wrap: balance;
}
.section-paper .sh-title { color: var(--cream-fixed); }
.sh-desc {
  margin-top: 16px;
  max-width: 560px;
  font-size: 15px;
  line-height: 1.65;
  color: var(--ref-ink-soft);
  opacity: 0.8;
}
.section-paper .sh-desc { color: rgba(255, 255, 255, 0.55); }
.sh-action { padding-bottom: 4px; flex-shrink: 0; }

/* ═══ TextLink ═══ */
.text-link {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 500;
  color: var(--ref-ink);
  background: none;
  border: none;
  padding: 0;
  cursor: pointer;
  text-decoration: none;
  transition: color 0.15s;
}
.text-link:hover { color: var(--ref-brand); }
.tl-text {
  border-bottom: 1px solid color-mix(in srgb, var(--ref-ink) 25%, transparent);
  padding-bottom: 2px;
  transition: border-color 0.15s;
}
.text-link:hover .tl-text { border-color: var(--ref-brand); }
.tl-arrow {
  font-size: 14px;
  transition: transform 0.15s;
}
.text-link:hover .tl-arrow { transform: translateX(4px); }

/* ═══ CTA buttons ═══ */
.cta {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  height: 48px;
  padding: 0 24px;
  font-size: 14px;
  font-weight: 500;
  border-radius: var(--r-btn);
  letter-spacing: -0.01em;
  cursor: pointer;
  transition: background 0.15s, border-color 0.15s, color 0.15s, transform 0.15s;
  border: 1px solid transparent;
}
.cta:hover { transform: translateY(-1px); }
.cta:active { transform: translateY(0) scale(0.985); }
.cta:disabled { opacity: 0.45; cursor: not-allowed; transform: none; }
.cta-primary {
  background: var(--ref-brand);
  color: #fff;
}
.cta-primary:hover:not(:disabled) { background: var(--ref-brand-deep); }
.cta-outline {
  background: var(--ref-surface);
  color: var(--ref-ink);
  border-color: var(--ref-line);
}
.cta-outline:hover { border-color: color-mix(in srgb, var(--ref-ink) 35%, transparent); background: color-mix(in srgb, var(--ref-surface) 70%, var(--ref-sand)); }
.cta-light {
  background: var(--cream-fixed);
  color: var(--paper);
}
.cta-light:hover { background: #fff; }
.cta-arrow {
  transition: transform 0.15s;
}
.cta:hover .cta-arrow { transform: translateX(4px); }

/* ═══ HERO ═══ */
.hero-section {
  padding: 136px 0 96px;
  background: var(--ref-canvas);
}
.hero-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  align-items: center;
  gap: 48px;
}
.hero-left { max-width: 640px; }

/* pill */
.hero-pill {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  padding: 6px 16px 6px 10px;
  border: 1px solid var(--ref-line);
  border-radius: 999px;
  background: color-mix(in srgb, var(--ref-surface) 80%, transparent);
}
.pill-dot {
  position: relative;
  display: block;
  width: 6px;
  height: 6px;
}
.pill-dot::before,
.pill-dot-inner {
  content: '';
  position: absolute;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--ref-brand);
  opacity: 0.6;
}
.pill-dot-inner {
  position: relative;
  display: block;
  background: var(--ref-brand);
  opacity: 1;
}
.pill-text {
  font-size: 13px;
  color: var(--ref-ink-soft);
}

/* title */
.hero-title {
  margin-top: 28px;
  font-family: var(--ref-font-display);
  font-size: clamp(40px, 5.6vw, 68px);
  font-weight: 400;
  line-height: 1.06;
  letter-spacing: -0.045em;
}
.hero-line {
  display: block;
  overflow: hidden;
}

/* desc */
.hero-desc {
  margin-top: 24px;
  max-width: 520px;
  font-size: 15px;
  line-height: 1.75;
  color: var(--ref-ink-soft);
  opacity: 0.85;
}

/* ctas */
.hero-ctas {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 36px;
}

/* stats */
.hero-stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  max-width: 520px;
  margin-top: 48px;
  padding-top: 24px;
  border-top: 1px solid var(--ref-line);
}
.hero-stats > div:first-child { padding-right: 20px; }
.hero-stats > div:not(:first-child) { padding-left: 20px; border-left: 1px solid var(--ref-line); }
.stat-val {
  font-family: var(--ref-font-display);
  font-size: 28px;
  font-weight: 400;
  line-height: 1;
  letter-spacing: -0.02em;
  color: var(--ref-ink);
  font-variant-numeric: tabular-nums;
}
.stat-label {
  margin-top: 8px;
  font-size: 12px;
  line-height: 1.3;
  color: var(--ref-muted);
}

/* hero right: images */
.hero-right { display: flex; justify-content: flex-end; }
.hero-image-wrap {
  position: relative;
  width: 100%;
  max-width: 480px;
}
.hero-main-img {
  overflow: hidden;
  border-radius: var(--r-frame);
  background: var(--ref-sand);
  cursor: default;
}
.hero-main-img :deep(.media-frame) {
  aspect-ratio: 4 / 5;
}
.hero-img-inner {
  width: 100%;
  height: 100%;
  transition: transform 0.35s cubic-bezier(0.23, 1, 0.32, 1);
  will-change: transform;
}

/* keeper overlay */
.hero-keeper-card {
  position: absolute;
  bottom: 20px;
  left: 0;
  right: 0;
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 0 16px;
  padding: 12px 16px;
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: var(--r-panel);
  background: rgba(23, 19, 15, 0.78);
  backdrop-filter: blur(8px);
}
.keeper-avatar-sm {
  width: 36px;
  height: 36px;
  flex-shrink: 0;
  border-radius: 50%;
  overflow: hidden;
  background: rgba(255, 255, 255, 0.15);
}
.keeper-avatar-sm :deep(.media-frame) { border-radius: 50%; }
.keeper-avatar-placeholder {
  display: block;
  width: 100%;
  height: 100%;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.2);
}
.kc-label {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.5);
}
.kc-name {
  margin-top: 4px;
  font-size: 13px;
  font-weight: 500;
  color: var(--cream-fixed);
}
.kc-level {
  margin-left: 8px;
  font-size: 11px;
  font-weight: 400;
  color: rgba(255, 255, 255, 0.45);
}

/* detail image */
.hero-detail-img {
  position: absolute;
  bottom: -32px;
  left: -40px;
  width: 160px;
  overflow: hidden;
  border-radius: var(--r-panel);
  border: 5px solid var(--ref-canvas);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
}
.hero-detail-img :deep(.media-frame) {
  aspect-ratio: 1;
}

/* ═══ SERVICES ═══ */
.svc-skeleton {
  display: grid;
  grid-template-columns: 7fr 5fr;
  gap: 24px;
  margin-top: 48px;
}
.sk-featured { height: 26rem; background: var(--ref-sand); border-radius: var(--r-card); }
.sk-stack { display: flex; flex-direction: column; gap: 12px; }
.sk-row { height: 6rem; background: var(--ref-sand); border-radius: var(--r-card); }

.svc-layout {
  display: grid;
  grid-template-columns: 7fr 5fr;
  gap: 24px;
  margin-top: 48px;
}
.svc-stack {
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border: 1px solid var(--ref-line);
  border-radius: var(--r-card);
  background: var(--ref-surface);
  box-shadow: 0 2px 4px -2px rgba(13, 33, 26, 0.1);
}

/* Feature card */
.feature-card {
  height: 100%;
  overflow: hidden;
  border: 1px solid var(--ref-line);
  border-radius: var(--r-card);
  background: var(--ref-surface);
  box-shadow: 0 2px 4px -2px rgba(13, 33, 26, 0.1);
  transition: transform 0.2s cubic-bezier(0.23, 1, 0.32, 1), box-shadow 0.2s, border-color 0.2s;
}
.feature-card:hover {
  transform: translateY(-4px);
  border-color: color-mix(in srgb, var(--ref-ink) 15%, transparent);
  box-shadow: 0 28px 60px -40px color-mix(in srgb, var(--ref-ink) 45%, transparent);
}
.feature-btn {
  display: flex;
  flex-direction: column;
  width: 100%;
  height: 100%;
  padding: 0;
  border: none;
  background: none;
  text-align: left;
  cursor: pointer;
  font: inherit;
}
.feature-img-wrap {
  position: relative;
  overflow: hidden;
}
.feature-img-wrap :deep(.media-frame) {
  aspect-ratio: 16 / 10;
}
.feature-img-wrap :deep(.media-image) {
  transition: transform 0.3s cubic-bezier(0.23, 1, 0.32, 1);
}
.feature-card:hover :deep(.media-image) {
  transform: scale(1.05);
}
.feature-badge {
  position: absolute;
  top: 16px;
  left: 16px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 10px;
  border-radius: var(--r-tag);
  background: rgba(23, 19, 15, 0.8);
  backdrop-filter: blur(4px);
  font-size: 11px;
  color: var(--cream-fixed);
}
.feature-badge .el-icon { font-size: 12px; }
.feature-body {
  display: flex;
  flex-direction: column;
  flex: 1;
  padding: 24px 28px 28px;
  gap: 20px;
}
.feature-name {
  font-family: var(--ref-font-display);
  font-size: 25px;
  font-weight: 400;
  line-height: 1.2;
  letter-spacing: -0.02em;
  color: var(--ref-ink);
}
.feature-desc {
  font-size: 14px;
  line-height: 1.6;
  color: var(--ref-ink-soft);
  opacity: 0.8;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.feature-footer {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  margin-top: auto;
  padding-top: 20px;
  border-top: 1px solid var(--ref-line);
}
.feature-price {
  display: flex;
  align-items: baseline;
  gap: 4px;
}
.fp-unit {
  font-size: 13px;
  color: var(--ref-muted);
}
.fp-val {
  font-family: var(--ref-font-display);
  font-size: 30px;
  font-weight: 400;
  line-height: 1;
  letter-spacing: -0.02em;
  color: var(--ref-ink);
  font-variant-numeric: tabular-nums;
}
.fp-per {
  font-size: 13px;
  color: var(--ref-muted);
}
.feature-link {
  display: flex;
  align-items: center;
  gap: 16px;
  font-size: 13px;
  color: var(--ref-ink-soft);
}
.feature-dist {
  display: flex;
  align-items: center;
  gap: 6px;
  color: var(--ref-muted);
}
.feature-dist .el-icon { font-size: 14px; }
.link-text {
  display: flex;
  align-items: center;
  gap: 6px;
  font-weight: 500;
  color: var(--ref-ink);
  transition: color 0.15s;
}
.feature-card:hover .link-text { color: var(--ref-brand); }
.link-arrow {
  transition: transform 0.15s;
}
.feature-card:hover .link-arrow { transform: translate(2px, -2px); }

/* Service row */
.service-row {
  display: flex;
  align-items: center;
  gap: 16px;
  width: 100%;
  padding: 16px;
  border: none;
  background: none;
  cursor: pointer;
  font: inherit;
  text-align: left;
  transition: background 0.15s;
  border-bottom: 1px solid var(--ref-line);
}
.service-row:last-child { border-bottom: 0; }
.service-row:hover { background: color-mix(in srgb, var(--ref-sand) 60%, transparent); }
.sr-thumb {
  width: 56px;
  height: 56px;
  flex-shrink: 0;
  border-radius: 8px;
  overflow: hidden;
  background: var(--ref-sand);
}
.sr-thumb :deep(.media-image) {
  transition: transform 0.3s cubic-bezier(0.23, 1, 0.32, 1);
}
.service-row:hover .sr-thumb :deep(.media-image) {
  transform: scale(1.08);
}
.sr-info {
  flex: 1;
  min-width: 0;
}
.sr-name {
  display: block;
  font-size: 14.5px;
  font-weight: 500;
  color: var(--ref-ink);
  letter-spacing: -0.01em;
}
.sr-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 6px;
  font-size: 11px;
  color: var(--ref-muted);
}
.sr-meta .el-icon { font-size: 12px; flex-shrink: 0; }
.sr-sep {
  width: 1px;
  height: 10px;
  background: var(--ref-line);
}
.sr-price { flex-shrink: 0; text-align: right; }
.sr-price-val {
  display: block;
  font-size: 14.5px;
  font-weight: 500;
  color: var(--ref-ink);
  font-variant-numeric: tabular-nums;
}
.sr-price-unit {
  margin-top: 4px;
  display: block;
  font-size: 11px;
  color: var(--ref-muted);
}
.sr-arrow {
  flex-shrink: 0;
  font-size: 16px;
  color: var(--ref-muted);
  transition: transform 0.15s, color 0.15s;
}
.service-row:hover .sr-arrow {
  transform: translate(2px, -2px);
  color: var(--ref-brand);
}

/* ═══ GALLERY BAND ═══ */
.gallery-band {
  border-top: 1px solid var(--ref-line);
  border-bottom: 1px solid var(--ref-line);
  background: var(--ref-surface);
  padding: 64px 0 80px;
}
.gb-head {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  justify-content: space-between;
  gap: 12px 24px;
}
.gb-eyebrow {
  display: flex;
  align-items: center;
  gap: 12px;
}
.gb-line {
  width: 24px;
  height: 1px;
  background: var(--ref-line);
}
.gb-title {
  font-family: var(--ref-font-display);
  font-size: 15px;
  font-weight: 500;
  color: var(--ref-ink-soft);
}
.gb-desc {
  max-width: 420px;
  font-size: 13.5px;
  line-height: 1.7;
  color: var(--ref-muted);
}
.gb-grid {
  display: grid;
  grid-template-columns: repeat(12, 1fr);
  gap: 16px;
  margin-top: 32px;
}
.gb-tile {
  position: relative;
  overflow: hidden;
  border-radius: var(--r-card);
  background: var(--ref-sand);
  will-change: transform;
}
.gb-tile :deep(.media-image) {
  transition: transform 0.3s cubic-bezier(0.23, 1, 0.32, 1);
}
.gb-tile:hover :deep(.media-image) {
  transform: scale(1.05);
}
.gb-span-5 { grid-column: span 5; }
.gb-span-4 { grid-column: span 4; }
.gb-span-3 { grid-column: span 3; }
.gb-aspect-45 { aspect-ratio: 4 / 5; }
.gb-aspect-43 { aspect-ratio: 4 / 3; }
.gb-aspect-34 { aspect-ratio: 3 / 4; }
.gb-aspect-1610 { aspect-ratio: 16 / 10; }
.gb-aspect-square { aspect-ratio: 1 / 1; }
.gb-caption {
  position: absolute;
  bottom: 12px;
  left: 12px;
  padding: 6px 10px;
  border-radius: var(--r-tag);
  background: rgba(23, 19, 15, 0.76);
  backdrop-filter: blur(4px);
  font-size: 12px;
  color: var(--cream-fixed);
  opacity: 0;
  transform: translateY(4px);
  transition: opacity 0.2s cubic-bezier(0.23, 1, 0.32, 1), transform 0.2s cubic-bezier(0.23, 1, 0.32, 1);
}
.gb-tile:hover .gb-caption {
  opacity: 1;
  transform: translateY(0);
}
.gb-caption-cat {
  margin-left: 8px;
  font-size: 11px;
  color: rgba(255, 255, 255, 0.55);
}
.gb-loading {
  background: var(--ref-sand);
  animation: db-pulse 1.4s ease-in-out infinite;
}

/* ═══ MERCHANT TABLE ═══ */
.merchant-table { margin-top: 12px; border-top: 1px solid var(--ref-line); }
.mt-row {
  display: flex;
  align-items: center;
  gap: 32px;
  padding: 24px 4px;
  border-bottom: 1px solid var(--ref-line);
  transition: background 0.15s;
  cursor: pointer;
}
.mt-row:hover { background: color-mix(in srgb, var(--ref-surface) 70%, var(--ref-sand)); }
.mt-idx {
  width: 32px;
  flex-shrink: 0;
  font-size: 11px;
  color: var(--ref-muted);
  opacity: 0.6;
  font-variant-numeric: tabular-nums;
}
.mt-main {
  flex: 1;
  min-width: 0;
}
.mt-name {
  display: block;
  font-size: 15.5px;
  font-weight: 500;
  color: var(--ref-ink);
  letter-spacing: -0.01em;
}
.mt-addr {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 6px;
  font-size: 13px;
  color: var(--ref-muted);
}
.mt-addr .el-icon { font-size: 14px; }
.mt-desc {
  font-size: 13px;
  line-height: 1.6;
  color: var(--ref-ink-soft);
  opacity: 0.75;
  max-width: 260px;
}
.mt-side {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-shrink: 0;
}
.mt-dist {
  font-size: 11px;
  padding: 5px 10px;
  border: 1px solid var(--ref-line);
  border-radius: var(--r-tag);
  background: var(--ref-surface);
  color: var(--ref-ink-soft);
  font-variant-numeric: tabular-nums;
}
.mt-phone {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--ref-ink-soft);
}
.mt-phone .el-icon { font-size: 14px; }
.mt-arrow {
  font-size: 16px;
  color: var(--ref-muted);
  transition: transform 0.15s, color 0.15s;
}
.mt-row:hover .mt-arrow {
  transform: translate(2px, -2px);
  color: var(--ref-brand);
}

/* ═══ KEEPERS ═══ */
.keeper-section-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin: 56px 0 24px;
}
.sub-heading {
  font-family: var(--ref-font-display);
  font-size: 20px;
  font-weight: 400;
  color: var(--ref-ink);
}
.sub-heading-right {
  font-size: 13px;
  color: var(--ref-muted);
}
.keeper-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}
.keeper-card { border-radius: var(--r-card); overflow: hidden; }
.kc-img {
  position: relative;
  aspect-ratio: 3 / 4;
  overflow: hidden;
  border-radius: var(--r-card);
  background: var(--ref-sand);
}
.kc-img :deep(.media-image) {
  transition: transform 0.3s cubic-bezier(0.23, 1, 0.32, 1);
}
.kc-img:hover :deep(.media-image) {
  transform: scale(1.06);
}
.kc-name-plate {
  position: absolute;
  bottom: 12px;
  left: 12px;
  padding: 6px 10px;
  border-radius: var(--r-tag);
  background: rgba(23, 19, 15, 0.72);
  backdrop-filter: blur(4px);
  transition: opacity 0.2s;
}
.kc-img:hover .kc-name-plate { opacity: 0; }
.kc-name-text {
  font-size: 13px;
  font-weight: 500;
  color: var(--cream-fixed);
  line-height: 1;
}
.kc-cert-text {
  margin-top: 6px;
  font-size: 10px;
  color: rgba(255, 255, 255, 0.5);
}
.kc-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  padding: 16px;
  background: rgba(23, 19, 15, 0.72);
  backdrop-filter: blur(2px);
  opacity: 0;
  transition: opacity 0.2s cubic-bezier(0.23, 1, 0.32, 1);
}
.kc-img:hover .kc-overlay { opacity: 1; }
.kc-overlay-name {
  font-family: var(--ref-font-display);
  font-size: 18px;
  font-weight: 400;
  color: var(--cream-fixed);
}
.kc-overlay-level {
  margin-top: 8px;
  font-size: 11px;
  color: rgba(255, 255, 255, 0.55);
}
.kc-overlay-intro {
  margin-top: 12px;
  font-size: 12px;
  line-height: 1.6;
  color: rgba(255, 255, 255, 0.7);
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.kc-overlay-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 16px;
  font-size: 11px;
  color: rgba(255, 255, 255, 0.5);
  font-variant-numeric: tabular-nums;
}
.meta-sep {
  width: 1px;
  height: 10px;
  background: rgba(255, 255, 255, 0.2);
}

/* ═══ TRUST ═══ */
.trust-grid {
  display: grid;
  grid-template-columns: 5fr 7fr;
  gap: 64px;
  align-items: start;
}
.trust-left .cta { margin-top: 36px; }
.trust-right {
  display: grid;
  grid-template-columns: 1fr 1fr;
}
.guarantee {
  padding: 28px 28px 28px 0;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
}
.guarantee-left {
  border-left: 1px solid rgba(255, 255, 255, 0.1);
  padding-left: 28px;
}
.guarantee-icon {
  color: var(--ref-brand);
  opacity: 0.8;
}
.guarantee h3 {
  margin-top: 20px;
  font-size: 15px;
  font-weight: 500;
  color: var(--cream-fixed);
}
.guarantee p {
  margin-top: 10px;
  font-size: 13.5px;
  line-height: 1.65;
  color: rgba(255, 255, 255, 0.55);
}

/* ═══ PROCESS ═══ */
.process-wrap { position: relative; margin-top: 8px; }
.process-rail {
  position: absolute;
  left: 0;
  top: 6px;
  height: 1px;
  width: 100%;
  background: var(--ref-line);
}
.process-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 32px;
}
.process-step {
  position: relative;
  padding-left: 20px;
  border-left: 1px solid var(--ref-line);
}
.ps-dot {
  position: absolute;
  left: -4px;
  top: 0;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--ref-brand);
}
.ps-num {
  display: block;
  font-family: var(--ref-font-display);
  font-size: 36px;
  font-weight: 400;
  line-height: 1;
  letter-spacing: -0.02em;
  color: var(--ref-ink);
  opacity: 0.15;
}
.ps-title {
  margin-top: 12px;
  font-size: 15px;
  font-weight: 500;
  color: var(--ref-ink);
}
.ps-body {
  margin-top: 10px;
  font-size: 13.5px;
  line-height: 1.6;
  color: var(--ref-muted);
}

/* ═══ TESTIMONIALS ═══ */
.testi-skeleton {
  display: grid;
  grid-template-columns: 7fr 5fr;
  gap: 24px;
  margin-top: 48px;
}
.sk-card-lg { height: 280px; background: var(--ref-sand); border-radius: var(--r-card); }
.testi-stack { display: flex; flex-direction: column; gap: 16px; }
.sk-card-sm { flex: 1; min-height: 130px; background: var(--ref-sand); border-radius: var(--r-card); }

.testi-layout {
  display: grid;
  grid-template-columns: 7fr 5fr;
  gap: 24px;
  margin-top: 48px;
}
.testi-featured,
.testi-card {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  background: var(--ref-surface);
  border: 1px solid var(--ref-line);
  border-radius: var(--r-card);
  box-shadow: 0 2px 4px -2px rgba(13, 33, 26, 0.1);
}
.testi-featured { padding: 28px 36px 28px; }
.testi-card { padding: 24px; background: color-mix(in srgb, var(--ref-canvas) 70%, transparent); }

/* stars */
.stars-row {
  display: flex;
  gap: 4px;
}
.star {
  font-size: 16px;
  color: var(--ref-line);
}
.star-sm { font-size: 14px; }
.star-fill { color: var(--ref-brand); }

.tq-big {
  margin: 24px 0;
  font-family: var(--ref-font-display);
  font-size: clamp(18px, 2vw, 22px);
  line-height: 1.6;
  letter-spacing: -0.01em;
  color: var(--ref-ink);
}
.tq-sm {
  margin: 16px 0;
  font-size: 14px;
  line-height: 1.65;
  color: var(--ref-ink-soft);
  display: -webkit-box;
  -webkit-line-clamp: 4;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.t-fig {
  display: flex;
  align-items: center;
  gap: 12px;
  padding-top: 20px;
  border-top: 1px solid var(--ref-line);
  font-size: 13px;
  color: var(--ref-muted);
}
.t-author { font-weight: 500; color: var(--ref-ink); }
.t-sep { width: 1px; height: 12px; background: var(--ref-line); }
.t-badge { margin-left: auto; font-size: 12px; }
.t-fig-sm {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: auto;
  padding-top: 14px;
  border-top: 1px solid var(--ref-line);
  font-size: 12px;
  color: var(--ref-muted);
}

/* rating display */
.rating-display { text-align: right; }
.rating-val {
  font-family: var(--ref-font-display);
  font-size: 32px;
  font-weight: 400;
  line-height: 1;
  letter-spacing: -0.02em;
  color: var(--ref-ink);
  font-variant-numeric: tabular-nums;
}
.rating-sub {
  margin-top: 8px;
  font-size: 12px;
  color: var(--ref-muted);
}

/* ═══ BOOKING — dark rounded card on canvas ═══ */
.booking-card {
  display: grid;
  grid-template-columns: 1fr 1fr;
  overflow: hidden;
  border-radius: var(--r-frame);
  background: var(--paper);
}
.booking-form-side { padding: 48px; }
.bk-label {
  font-size: 12px;
  font-weight: 500;
  text-transform: uppercase;
  letter-spacing: 0.16em;
  color: rgba(255, 255, 255, 0.45);
}
.bk-title {
  margin: 16px 0;
  font-family: var(--ref-font-display);
  font-size: clamp(24px, 3vw, 36px);
  font-weight: 400;
  line-height: 1.2;
  letter-spacing: -0.03em;
  color: var(--cream-fixed);
}
.bk-desc {
  font-size: 14px;
  line-height: 1.7;
  color: rgba(255, 255, 255, 0.5);
  max-width: 420px;
}
.bk-form { margin-top: 36px; }
.bk-field { margin-bottom: 28px; }
.bk-f-label {
  display: block;
  margin-bottom: 8px;
  font-size: 11px;
  font-weight: 500;
  text-transform: uppercase;
  letter-spacing: 0.16em;
  color: rgba(255, 255, 255, 0.4);
}
.bk-select-wrap { position: relative; }
.bk-select,
.bk-date {
  width: 100%;
  height: 48px;
  appearance: none;
  border: none;
  border-bottom: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 0;
  background: transparent;
  font-size: 15px;
  color: var(--cream-fixed);
  outline: none;
  padding-right: 24px;
  font-family: inherit;
  cursor: pointer;
}
.bk-date {
  color-scheme: dark;
  padding-right: 8px;
}
.bk-select:focus,
.bk-date:focus { border-bottom-color: var(--ref-brand); }
.bk-select option { color: var(--ref-ink); background: #fff; }
.bk-select:disabled { opacity: 0.45; }
.bk-chevron {
  position: absolute;
  right: 4px;
  top: 50%;
  transform: translateY(-50%);
  color: rgba(255, 255, 255, 0.4);
  font-size: 14px;
  pointer-events: none;
}
.bk-price-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding-top: 12px;
}
.bk-price { color: rgba(255, 255, 255, 0.5); font-size: 14px; }
.bk-price-empty { font-size: 12px; }
.bk-p-label { font-size: 12px; margin-right: 6px; }
.bk-p-val {
  font-family: var(--ref-font-display);
  font-size: 28px;
  font-weight: 400;
  color: var(--cream-fixed);
  line-height: 1;
  font-variant-numeric: tabular-nums;
}
.bk-p-unit {
  font-size: 12px;
  margin-left: 4px;
  color: rgba(255, 255, 255, 0.45);
}
.bk-assurances {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-top: 40px;
  padding-top: 24px;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  list-style: none;
  padding-left: 0;
}
.bk-assurances li {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.5);
}
.bk-assurances .el-icon {
  color: var(--ref-brand);
  font-size: 15px;
}
.booking-image-side {
  overflow: hidden;
}
.booking-image-side :deep(.media-frame) {
  height: 100%;
  min-height: 480px;
}

/* ═══ EMPTY ═══ */
.empty-dashed {
  margin-top: 48px;
  padding: 64px;
  border: 1px dashed var(--ref-line);
  border-radius: var(--r-card);
  background: var(--ref-surface);
  text-align: center;
  font-size: 14px;
  color: var(--ref-muted);
}

/* ═══ FOOTER — light canvas ═══ */
.site-footer {
  border-top: 1px solid var(--ref-line);
  background: var(--ref-canvas);
  color: var(--ref-ink-soft);
  padding: 48px 0 0;
}
.footer-grid {
  display: grid;
  grid-template-columns: 1.5fr 1fr 1fr 1fr;
  gap: 32px;
  padding-bottom: 40px;
}
.footer-logo-row {
  display: flex;
  align-items: center;
  gap: 12px;
}
.footer-logo-mark {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 9px;
  border: 1px solid color-mix(in srgb, var(--ref-ink) 15%, transparent);
  background: var(--ref-surface);
  font-size: 16px;
}
.footer-logo-text { display: flex; flex-direction: column; line-height: 1.15; }
.footer-name {
  font-size: 15px;
  font-weight: 500;
  color: var(--ref-ink);
  letter-spacing: -0.01em;
}
.footer-en {
  margin-top: 4px;
  font-size: 9px;
  letter-spacing: 0.28em;
  text-transform: uppercase;
  color: var(--ref-muted);
}
.footer-tagline {
  margin-top: 16px;
  max-width: 280px;
  font-size: 13.5px;
  line-height: 1.7;
  color: var(--ref-ink-soft);
  opacity: 0.75;
}
.footer-contact {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 20px;
  padding: 0;
  list-style: none;
  font-size: 13.5px;
  color: var(--ref-ink-soft);
}
.footer-contact li {
  display: flex;
  align-items: center;
  gap: 8px;
}
.footer-contact .el-icon { flex-shrink: 0; font-size: 14px; color: var(--ref-brand); }
.footer-heading {
  margin-bottom: 16px;
  font-size: 13px;
  font-weight: 500;
  color: var(--ref-ink);
}
.footer-list {
  display: grid;
  gap: 10px;
  padding: 0;
  margin: 0;
  list-style: none;
  font-size: 13px;
  color: var(--ref-muted);
}
.footer-list a {
  color: var(--ref-muted);
  text-decoration: none;
  transition: color 0.15s;
}
.footer-list a:hover { color: var(--ref-ink); }
.footer-bottom {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 8px 24px;
  padding: 18px 0;
  border-top: 1px solid var(--ref-line);
  font-size: 12px;
  color: var(--ref-muted);
}
.footer-legal {
  display: flex;
  gap: 20px;
}
.footer-legal a {
  color: var(--ref-muted);
  text-decoration: none;
  transition: color 0.15s;
}
.footer-legal a:hover { color: var(--ref-ink); }

/* ═══ ANIMATIONS ═══ */
@keyframes db-pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.55; }
}

/* ═══ RESPONSIVE ═══ */
@media (max-width: 1100px) {
  .hero-grid { grid-template-columns: 1fr; gap: 48px; }
  .hero-right { justify-content: center; }
  .hero-image-wrap { max-width: 400px; }
  .hero-detail-img { display: none; }
  .svc-layout,
  .svc-skeleton,
  .testi-layout,
  .testi-skeleton { grid-template-columns: repeat(2, 1fr); }
  .keeper-grid { grid-template-columns: repeat(3, 1fr); }
  .trust-grid { grid-template-columns: 1fr; gap: 40px; }
  .process-grid { grid-template-columns: repeat(2, 1fr); }
}
@media (max-width: 720px) {
  .shell { width: min(100% - 32px, 78rem); }
  .section { padding: 56px 0; }
  .hero-section { padding: 80px 0 56px; }
  .hero-title { font-size: 36px; }
  .sh-wrap { display: block; margin-bottom: 24px; }
  .sh-title { font-size: 28px; }
  .sh-desc { margin-top: 12px; }
  .sh-action { margin-top: 12px; }
  .svc-layout,
  .svc-skeleton,
  .testi-layout,
  .testi-skeleton { grid-template-columns: 1fr; }
  .keeper-grid { grid-template-columns: repeat(2, 1fr); }
  .process-grid { grid-template-columns: 1fr; }
  .trust-right { grid-template-columns: 1fr; }
  .booking-card { grid-template-columns: 1fr; }
  .booking-form-side { padding: 32px 24px; }
  .booking-image-side :deep(.media-frame) { min-height: 280px; }
  .footer-grid { grid-template-columns: 1fr 1fr; gap: 24px; }
  .mt-row { flex-wrap: wrap; gap: 8px; }
  .mt-desc { max-width: none; width: 100%; order: 3; }

  /* gallery: 2-col uniform grid below md */
  .gallery-band { padding: 48px 0 56px; }
  .gb-grid { grid-template-columns: repeat(2, 1fr); gap: 12px; }
  .gb-tile { aspect-ratio: 4 / 5 !important; }
  .gb-span-5,
  .gb-span-4,
  .gb-span-3 { grid-column: auto; }
}
@media (max-width: 480px) {
  .keeper-grid { grid-template-columns: 1fr; }
  .footer-grid { grid-template-columns: 1fr; }
  .hero-stats { grid-template-columns: 1fr; gap: 16px; }
  .hero-stats > div { padding: 0; border: 0; }
}
@media (prefers-reduced-motion: reduce) {
  .feature-img-wrap :deep(.media-image),
  .kc-img :deep(.media-image),
  .sr-thumb :deep(.media-image),
  .gb-tile :deep(.media-image),
  .gb-loading { transition: none; animation: none; }
}
</style>
