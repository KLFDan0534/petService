<template>
  <div class="pr-page">
    <div class="pr-shell">
      <!-- ═══ Breadcrumb ═══ -->
      <nav class="pr-crumb" aria-label="面包屑">
        <router-link to="/dashboard" class="pr-crumb-link">首页</router-link>
        <span class="pr-crumb-sep" aria-hidden="true">›</span>
        <span class="pr-crumb-here">个人中心</span>
      </nav>

      <!-- ═══ Loading / error ═══ -->
      <template v-if="loading">
        <div class="ph-skeleton"></div>
        <div class="ps-skeleton-stack">
          <div class="ps-skeleton"></div>
          <div class="ps-skeleton"></div>
        </div>
      </template>

      <template v-else-if="loadError">
        <div class="pr-error">
          <p class="pr-error-title">个人中心加载失败</p>
          <p class="pr-error-desc">{{ loadError }}</p>
          <button type="button" class="cta cta-primary" @click="reloadAll">重新加载</button>
        </div>
      </template>

      <template v-else>
        <!-- ═══════════════════════════════════════════
             PROFILE HERO
             ═══════════════════════════════════════════ -->
        <section class="ph-card" aria-label="个人资料">
          <div class="ph-top">
            <div class="ph-identity">
              <button
                type="button"
                class="ph-avatar-btn"
                :disabled="avatarUploading"
                aria-label="更换头像"
                @click="triggerAvatarInput"
              >
                <img v-if="avatarUrl" :src="avatarUrl" :alt="`${displayName} 的头像`" class="ph-avatar-img">
                <span v-else class="ph-avatar-initial">{{ avatarInitial }}</span>
                <span class="ph-avatar-overlay">
                  <el-icon aria-hidden="true"><Camera /></el-icon>
                  <span>更换头像</span>
                </span>
                <span v-if="avatarUploading" class="ph-avatar-busy">
                  <span class="ph-spinner" aria-hidden="true"></span>
                </span>
              </button>
              <input
                ref="avatarInputRef"
                type="file"
                accept="image/*"
                class="ph-avatar-file"
                tabindex="-1"
                @change="onAvatarFile"
              >

              <div class="ph-id-copy">
                <p class="ph-eyebrow">
                  <span class="ph-eyebrow-idx">个人中心</span>
                  <span class="ph-eyebrow-line" aria-hidden="true"></span>
                  <span>Personal Space</span>
                </p>
                <h1 class="ph-name">{{ displayName }}</h1>
                <p class="ph-meta">
                  <span v-if="profile.username_wsh">@{{ profile.username_wsh }}</span>
                  <span v-if="profile.username_wsh && joinedLabel" class="ph-meta-sep" aria-hidden="true">·</span>
                  <span v-if="joinedLabel">{{ joinedLabel }}</span>
                </p>
                <div class="ph-tags">
                  <span v-for="role in roleTags" :key="role" class="tag-pill">{{ roleLabel(role) }}</span>
                  <span :class="['badge', `badge-${realNameBadge.tone}`]">{{ realNameBadge.label }}</span>
                </div>
              </div>
            </div>

            <div class="ph-actions">
              <button type="button" class="cta cta-dark" @click="editDialogOpen = true">编辑资料</button>
              <a href="#account" class="ph-link-btn">账户与安全</a>
            </div>
          </div>

          <!-- active stay banner -->
          <div v-if="activeStay" class="ph-stay">
            <span class="ph-stay-dot" aria-hidden="true"></span>
            <p class="ph-stay-text">
              <span class="ph-stay-name">{{ activeStay.pet_name_wsh || '你的宠物' }}</span>
              正在 {{ activeStay.merchant_name_wsh || '门店' }}
              <span class="ph-stay-sep" aria-hidden="true">·</span>
              {{ statusLabel(activeStay.status_wsh) }}
            </p>
            <router-link to="/orders" class="ph-stay-link">查看服务动态</router-link>
          </div>

          <!-- metrics -->
          <dl class="ph-metrics">
            <div class="ph-metric">
              <dt class="ph-m-label">宠物档案</dt>
              <dd class="ph-m-value">{{ summary.pets }}<span class="ph-m-unit">只</span></dd>
            </div>
            <div class="ph-metric">
              <dt class="ph-m-label">进行中订单</dt>
              <dd class="ph-m-value">{{ summary.activeOrders }}<span class="ph-m-unit">张</span></dd>
            </div>
            <div class="ph-metric">
              <dt class="ph-m-label">已完成照护</dt>
              <dd class="ph-m-value">{{ summary.completedOrders }}<span class="ph-m-unit">次</span></dd>
            </div>
            <div class="ph-metric ph-metric-brand">
              <dt class="ph-m-label">累计照护投入</dt>
              <dd class="ph-m-value ph-m-value-brand"><span class="ph-m-yen">¥</span>{{ formatMoney(summary.totalSpent) }}</dd>
            </div>
          </dl>
        </section>

        <!-- ═══════════════════════════════════════════
             01 · 我的宠物
             ═══════════════════════════════════════════ -->
        <section class="ps-section">
          <header class="ps-head">
            <div class="ps-head-copy">
              <p class="ps-eyebrow">
                <span class="ps-idx">01</span>
                <span class="ps-line" aria-hidden="true"></span>
                <span>Pet Profiles</span>
              </p>
              <h2 class="ps-title">我的宠物</h2>
              <p class="ps-desc">档案来自你的宠物信息与照护订单，下单时可直接选用。</p>
            </div>
          </header>

          <div v-if="pets.length" class="pets-grid">
            <router-link
              v-for="(pet, index) in pets.slice(0, 6)"
              :key="pet.key"
              to="/orders"
              class="pet-card"
              :style="{ transitionDelay: `${Math.min(index, 5) * 40}ms` }"
              :aria-label="`查看 ${pet.name} 的寄养记录`"
            >
              <div class="pet-media">
                <MediaWithFallback :src="pet.avatar" :alt="`${pet.name} 的照片`" class="pet-img" fallback-label="暂无照片" />
                <span v-if="pet.activeStatus" class="pet-live-badge">
                  <span class="pet-live-dot" aria-hidden="true"></span>
                  {{ statusLabel(pet.activeStatus) }}
                </span>
                <span class="pet-arrow" aria-hidden="true">↗</span>
                <div class="pet-plate">
                  <h3 class="pet-name">{{ pet.name }}</h3>
                  <p class="pet-line">{{ petLine(pet) }}</p>
                </div>
              </div>
              <div class="pet-foot">
                <span>预约 <b class="pet-num">{{ pet.stays }}</b> 次</span>
                <span>累计 <b class="pet-num">¥ {{ formatMoney(pet.spent) }}</b></span>
              </div>
            </router-link>
          </div>

          <div v-else class="ps-empty">
            <p class="ps-empty-title">还没有宠物档案</p>
            <p class="ps-empty-desc">完成首次寄养后，你的宠物会出现在这里。</p>
            <router-link to="/services" class="cta cta-outline">浏览照护服务</router-link>
          </div>
        </section>

        <!-- ═══════════════════════════════════════════
             02 · 最近订单   +   03 · 权益与入口
             ═══════════════════════════════════════════ -->
        <div class="ps-duo">
          <section class="ps-section">
            <header class="ps-head">
              <div class="ps-head-copy">
                <p class="ps-eyebrow">
                  <span class="ps-idx">02</span>
                  <span class="ps-line" aria-hidden="true"></span>
                  <span>Recent Stays</span>
                </p>
                <h2 class="ps-title">最近订单</h2>
              </div>
              <div class="ps-action">
                <router-link to="/orders" class="text-link">
                  <span class="tl-text">查看全部订单</span>
                  <span class="tl-arrow">→</span>
                </router-link>
              </div>
            </header>

            <div v-if="recentOrders.length" class="orders-list">
              <router-link v-for="order in recentOrders" :key="order.id_wsh" to="/orders" class="order-row">
                <span class="or-thumb">
                  <MediaWithFallback :src="orderServiceImage(order)" alt="" class="or-img" />
                </span>
                <span class="or-main">
                  <span class="or-name">{{ order.service_name_wsh || '宠物照护服务' }}</span>
                  <span class="or-meta">
                    <span class="or-trunc">{{ order.pet_name_wsh || '宠物' }}</span>
                    <span class="or-sep" aria-hidden="true">·</span>
                    <span>{{ orderDayRange(order) }}</span>
                  </span>
                </span>
                <span class="or-side">
                  <span class="or-price">¥ {{ formatMoney(orderAmount(order)) }}</span>
                  <span :class="['badge', `badge-${orderBadgeTone(order.status_wsh)}`]">{{ statusLabel(order.status_wsh) }}</span>
                </span>
              </router-link>
            </div>

            <div v-else class="ps-empty ps-empty-sm">
              <p class="ps-empty-title">暂无订单</p>
              <p class="ps-empty-desc">完成一次寄养后，记录会显示在这里。</p>
              <router-link to="/services" class="cta cta-outline">去预约</router-link>
            </div>
          </section>

          <section class="ps-section">
            <header class="ps-head">
              <div class="ps-head-copy">
                <p class="ps-eyebrow">
                  <span class="ps-idx">03</span>
                  <span class="ps-line" aria-hidden="true"></span>
                  <span>Benefits</span>
                </p>
                <h2 class="ps-title">权益与入口</h2>
              </div>
            </header>

            <nav class="quick-nav" aria-label="权益与常用入口">
              <ul>
                <li v-for="action in quickActions" :key="action.key">
                  <router-link :to="action.to" class="quick-row">
                    <span class="quick-icon">
                      <el-icon aria-hidden="true"><component :is="action.icon" /></el-icon>
                    </span>
                    <span class="quick-copy">
                      <span class="quick-title">{{ action.label }}</span>
                      <span class="quick-desc">{{ action.desc }}</span>
                    </span>
                    <span class="quick-arrow" aria-hidden="true">→</span>
                  </router-link>
                </li>
              </ul>
            </nav>
          </section>
        </div>

        <!-- ═══════════════════════════════════════════
             04 · 我的钱包
             ═══════════════════════════════════════════ -->
        <section class="ps-section">
          <header class="ps-head">
            <div class="ps-head-copy">
              <p class="ps-eyebrow">
                <span class="ps-idx">04</span>
                <span class="ps-line" aria-hidden="true"></span>
                <span>Wallet</span>
              </p>
              <h2 class="ps-title">我的钱包</h2>
              <p class="ps-desc">余额可直接用于支付订单，提现需经审核与打款。</p>
            </div>
          </header>

          <div class="wp-grid">
            <!-- balance panel -->
            <section class="wp-balance" aria-label="账户余额">
              <div>
                <p class="wp-b-label">可用余额</p>
                <p class="wp-b-amount">
                  <span class="wp-b-yen">¥</span>
                  <span>{{ formatMoney(walletAvailable) }}</span>
                </p>
                <div class="wp-b-sub">
                  <div>
                    <p class="wp-b-sub-label">账户总额</p>
                    <p class="wp-b-sub-val">¥ {{ formatMoney(wallet.balance_wsh) }}</p>
                  </div>
                  <div>
                    <p class="wp-b-sub-label">冻结金额</p>
                    <p class="wp-b-sub-val">¥ {{ formatMoney(wallet.frozen_amount_wsh) }}</p>
                  </div>
                </div>
              </div>
              <div>
                <div class="wp-b-actions">
                  <button type="button" class="wp-btn wp-btn-light" @click="openRecharge">充值</button>
                  <button type="button" class="wp-btn wp-btn-ghost" @click="openWithdraw">提现</button>
                </div>
                <p class="wp-b-note">冻结金额指暂不可用的部分：提现申请在审核与打款完成前会冻结对应余额，打款完成后从冻结金额中扣减。</p>
              </div>
            </section>

            <!-- ledger -->
            <section class="wp-ledger" aria-label="账户流水">
              <div class="ledger-tabs" role="tablist">
                <button
                  v-for="tab in ledgerTabs"
                  :key="tab.key"
                  type="button"
                  role="tab"
                  :aria-selected="ledgerTab === tab.key"
                  class="ledger-tab"
                  :class="{ active: ledgerTab === tab.key }"
                  @click="ledgerTab = tab.key"
                >
                  {{ tab.label }}
                  <span v-if="ledgerCounts[tab.key]" class="ledger-count">{{ ledgerCounts[tab.key] }}</span>
                </button>
              </div>

              <div v-if="ledgerTab === 'transactions'" class="ledger-body">
                <ul v-if="transactions.length" class="ledger-list">
                  <li v-for="t in transactions.slice(0, 5)" :key="t.id_wsh" class="ledger-row">
                    <div class="lr-main">
                      <p class="lr-title">{{ typeLabel(t) }}</p>
                      <p class="lr-meta">
                        <span class="lr-trunc">{{ t.description_wsh || '—' }}</span>
                        <span class="lr-sep" aria-hidden="true">·</span>
                        <span>{{ fmtTime(t.created_at_wsh) }}</span>
                      </p>
                    </div>
                    <div class="lr-side">
                      <p :class="['lr-amount', t.direction_wsh === 'in' ? 'lr-in' : 'lr-out']">
                        {{ t.direction_wsh === 'in' ? '+' : '−' }}¥ {{ formatMoney(t.amount_wsh) }}
                      </p>
                      <p v-if="t.balance_after_wsh !== undefined" class="lr-balance">余额 ¥ {{ formatMoney(t.balance_after_wsh) }}</p>
                    </div>
                  </li>
                </ul>
                <div v-else class="ledger-empty">暂无交易记录</div>
              </div>

              <div v-else-if="ledgerTab === 'withdrawals'" class="ledger-body">
                <ul v-if="withdrawals.length" class="ledger-list">
                  <li v-for="w in withdrawals.slice(0, 5)" :key="w.id_wsh" class="ledger-row">
                    <div class="lr-main">
                      <p class="lr-title">{{ w.account_wsh || w.account_name_wsh || '收款账户待补充' }}</p>
                      <p class="lr-meta">
                        <span>#{{ w.id_wsh }}</span>
                        <span class="lr-sep" aria-hidden="true">·</span>
                        <span>{{ fmtTime(w.created_at_wsh) }}</span>
                      </p>
                    </div>
                    <div class="lr-side lr-side-row">
                      <p class="lr-amount">¥ {{ formatMoney(w.amount_wsh) }}</p>
                      <span :class="['badge', `badge-${withdrawalBadge(w.status_wsh).tone}`]">{{ withdrawalBadge(w.status_wsh).label }}</span>
                    </div>
                  </li>
                </ul>
                <div v-else class="ledger-empty">暂无提现记录</div>
              </div>

              <div v-else class="ledger-body">
                <ul v-if="tips.length" class="ledger-list">
                  <li v-for="t in tips.slice(0, 5)" :key="t.id_wsh" class="ledger-row ledger-row-tip">
                    <div class="lr-tip-head">
                      <p class="lr-title">打赏 <b>¥ {{ formatMoney(t.amount_wsh) }}</b></p>
                      <p class="lr-meta">{{ fmtTime(t.created_at_wsh) }}</p>
                    </div>
                    <p v-if="t.message_wsh" class="lr-tip-msg">{{ t.message_wsh }}</p>
                  </li>
                </ul>
                <div v-else class="ledger-empty">暂无打赏记录</div>
              </div>

              <p v-if="ledgerCounts[ledgerTab] > 5" class="ledger-more">
                仅显示最近 5 条{{ ledgerTabLabel }}，共 <b>{{ ledgerCounts[ledgerTab] }}</b> 条
              </p>
            </section>
          </div>
        </section>

        <!-- ═══════════════════════════════════════════
             05 · 身份与服务
             ═══════════════════════════════════════════ -->
        <section class="ps-section">
          <header class="ps-head">
            <div class="ps-head-copy">
              <p class="ps-eyebrow">
                <span class="ps-idx">05</span>
                <span class="ps-line" aria-hidden="true"></span>
                <span>Roles</span>
              </p>
              <h2 class="ps-title">身份与服务</h2>
              <p class="ps-desc">以商家、照护师或客服身份加入平台，申请进度会显示在这里。</p>
            </div>
          </header>

          <div class="rr-grid">
            <a v-for="entry in roleEntries" :key="entry.key" :href="entry.to || undefined" class="rr-card" :class="{ 'is-static': !entry.to }" @click.prevent="goRoleEntry(entry)">
              <span class="rr-icon">
                <el-icon aria-hidden="true"><component :is="entry.icon" /></el-icon>
              </span>
              <span class="rr-copy">
                <span class="rr-title">{{ entry.title }}</span>
                <span class="rr-desc">{{ entry.desc }}</span>
              </span>
              <span v-if="entry.badge" :class="['badge', `badge-${entry.badge.tone}`]">{{ entry.badge.label }}</span>
              <span v-else class="rr-arrow" aria-hidden="true">→</span>
            </a>
          </div>
        </section>

        <!-- ═══════════════════════════════════════════
             06 · 账户与安全
             ═══════════════════════════════════════════ -->
        <section id="account" class="ps-section ps-scroll-target">
          <header class="ps-head">
            <div class="ps-head-copy">
              <p class="ps-eyebrow">
                <span class="ps-idx">06</span>
                <span class="ps-line" aria-hidden="true"></span>
                <span>Account</span>
              </p>
              <h2 class="ps-title">账户与安全</h2>
              <p class="ps-desc">手机号、邮箱、实名与支付密码分别单独确认后保存。</p>
            </div>
          </header>

          <div class="ap-panel">
            <div class="ap-grid">
              <div
                v-for="(row, index) in accountRows"
                :key="row.key"
                class="ap-row"
                :class="{ 'ap-row-last': index === accountRows.length - 1 }"
              >
                <div class="ap-copy">
                  <p class="ap-label">{{ row.label }}</p>
                  <p class="ap-value">
                    <span class="ap-value-text">{{ row.value }}</span>
                    <span v-if="row.badge" :class="['badge', `badge-${row.badge.tone}`]">{{ row.badge.label }}</span>
                  </p>
                  <p v-if="row.note" class="ap-note">{{ row.note }}</p>
                </div>
                <button v-if="row.action" type="button" class="ap-action" @click="row.action.onClick()">
                  {{ row.action.label }}
                </button>
              </div>
            </div>
          </div>

          <div class="ap-delete">
            <div class="ap-delete-copy">
              <p class="ap-delete-title">删除账号</p>
              <p class="ap-delete-desc">删除后资料、宠物档案与订单记录将无法恢复。若还有进行中的寄养订单，请先联系门店处理。</p>
            </div>
            <button type="button" class="ap-delete-btn" :disabled="deleting" @click="deleteDialogOpen = true">
              {{ deleting ? '删除中...' : '删除账号' }}
            </button>
          </div>
        </section>
      </template>
    </div>

    <!-- ═══════════════════════════════════════════
         DIALOGS
         ═══════════════════════════════════════════ -->

    <!-- 编辑资料 -->
    <div v-if="editDialogOpen" class="dlg-overlay" @click.self="editDialogOpen = false">
      <div class="dlg-panel" role="dialog" aria-modal="true" aria-label="编辑资料">
        <div class="dlg-head">
          <h3 class="dlg-title">编辑资料</h3>
          <button type="button" class="dlg-close" aria-label="关闭" @click="editDialogOpen = false">✕</button>
        </div>
        <div class="dlg-body">
          <div class="dlg-avatar-row">
            <button type="button" class="dlg-avatar" :disabled="avatarUploading" @click="triggerAvatarInput">
              <img v-if="avatarUrl" :src="avatarUrl" alt="头像">
              <span v-else>{{ avatarInitial }}</span>
              <span class="dlg-avatar-overlay"><el-icon aria-hidden="true"><Camera /></el-icon></span>
            </button>
            <p class="dlg-avatar-hint">点击头像上传，支持 JPG / PNG，不超过 5MB</p>
          </div>
          <div class="dlg-field">
            <label class="dlg-label" for="dlg-nickname">昵称</label>
            <input id="dlg-nickname" v-model="profile.nickname_wsh" class="dlg-input" placeholder="请输入昵称">
          </div>
          <div class="dlg-field">
            <label class="dlg-label" for="dlg-gender">性别</label>
            <select id="dlg-gender" v-model.number="profile.gender_wsh" class="dlg-input">
              <option :value="0">未知</option>
              <option :value="1">男</option>
              <option :value="2">女</option>
            </select>
          </div>
        </div>
        <div class="dlg-foot">
          <button type="button" class="dlg-cancel" @click="editDialogOpen = false">取消</button>
          <button type="button" class="cta cta-primary" :disabled="submitting" @click="submitProfileEdit">
            {{ submitting ? '保存中...' : '保存修改' }}
          </button>
        </div>
      </div>
    </div>

    <!-- 充值 -->
    <div v-if="rechargeDialogOpen" class="dlg-overlay" @click.self="rechargeDialogOpen = false">
      <div class="dlg-panel" role="dialog" aria-modal="true" aria-label="充值">
        <div class="dlg-head">
          <h3 class="dlg-title">余额充值</h3>
          <button type="button" class="dlg-close" aria-label="关闭" @click="rechargeDialogOpen = false">✕</button>
        </div>
        <div class="dlg-body">
          <div class="dlg-balance">
            <span>当前余额</span>
            <b>¥ {{ formatMoney(wallet.balance_wsh) }}</b>
            <small>冻结金额 ¥ {{ formatMoney(wallet.frozen_amount_wsh) }}</small>
          </div>
          <div class="dlg-field">
            <label class="dlg-label" for="dlg-recharge-amount">充值金额 (¥)</label>
            <input id="dlg-recharge-amount" v-model="rechargeAmount" type="number" min="0.01" step="0.01" class="dlg-input" placeholder="输入金额">
          </div>
          <div class="dlg-field">
            <label class="dlg-label" for="dlg-recharge-method">支付方式</label>
            <select id="dlg-recharge-method" v-model="rechargeMethod" class="dlg-input">
              <option value="wechat">微信支付</option>
              <option value="alipay">支付宝</option>
              <option value="bank">银行卡</option>
            </select>
          </div>
          <p class="dlg-note">充值成功后即时增加余额并生成充值记录，金额不能为 0 或负数。</p>
        </div>
        <div class="dlg-foot">
          <button type="button" class="dlg-cancel" @click="rechargeDialogOpen = false">取消</button>
          <button type="button" class="cta cta-primary" :disabled="rechargeSubmitting" @click="submitRecharge">
            {{ rechargeSubmitting ? '充值中...' : '确认充值' }}
          </button>
        </div>
      </div>
    </div>

    <!-- 提现 -->
    <div v-if="withdrawDialogOpen" class="dlg-overlay" @click.self="withdrawDialogOpen = false">
      <div class="dlg-panel" role="dialog" aria-modal="true" aria-label="提现">
        <div class="dlg-head">
          <h3 class="dlg-title">申请提现</h3>
          <button type="button" class="dlg-close" aria-label="关闭" @click="withdrawDialogOpen = false">✕</button>
        </div>
        <div class="dlg-body">
          <div class="dlg-balance">
            <span>可用余额</span>
            <b>¥ {{ formatMoney(walletAvailable) }}</b>
          </div>
          <div class="dlg-field">
            <label class="dlg-label" for="dlg-withdraw-amount">提现金额 (¥)</label>
            <input id="dlg-withdraw-amount" v-model="withdrawAmount" type="number" step="0.01" class="dlg-input" placeholder="输入金额" :max="walletAvailable">
          </div>
          <div class="dlg-field">
            <label class="dlg-label" for="dlg-withdraw-account">收款账户</label>
            <input id="dlg-withdraw-account" v-model="withdrawAccount" class="dlg-input" placeholder="支付宝 / 银行卡号">
          </div>
          <p class="dlg-note">提交后需经审核与打款，审核期间对应余额将被冻结。</p>
        </div>
        <div class="dlg-foot">
          <button type="button" class="dlg-cancel" @click="withdrawDialogOpen = false">取消</button>
          <button
            type="button"
            class="cta cta-primary"
            :disabled="withdrawSubmitting || !withdrawAmount || !withdrawAccount"
            @click="submitWithdraw"
          >
            {{ withdrawSubmitting ? '提交中...' : '申请提现' }}
          </button>
        </div>
      </div>
    </div>

    <!-- 删除账号确认 -->
    <div v-if="deleteDialogOpen" class="dlg-overlay" @click.self="deleteDialogOpen = false">
      <div class="dlg-panel" role="dialog" aria-modal="true" aria-label="确认删除账号">
        <div class="dlg-head">
          <h3 class="dlg-title">确认删除账号？</h3>
          <button type="button" class="dlg-close" aria-label="关闭" @click="deleteDialogOpen = false">✕</button>
        </div>
        <div class="dlg-body">
          <p class="dlg-warn">此操作不可恢复，资料、宠物档案与订单记录都会一并删除。</p>
          <p class="dlg-detail">
            {{ profile.nickname_wsh || profile.username_wsh || '当前账号' }} ·
            {{ summary.pets }} 份宠物档案 ·
            {{ summary.completedOrders }} 次已完成照护
          </p>
        </div>
        <div class="dlg-foot">
          <button type="button" class="dlg-cancel" @click="deleteDialogOpen = false">取消</button>
          <button type="button" class="ap-delete-btn" :disabled="deleting" @click="confirmDeleteAccount">
            {{ deleting ? '删除中...' : '确认删除' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, reactive, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import {
  ArrowRight,
  Camera,
  ChatDotRound,
  Headset,
  Medal,
  Message,
  Service,
  Shop,
  Ticket,
  UserFilled,
  Warning,
} from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { useAppStore } from '@/stores/app'
import { deleteCurrentUser } from '@/api/auth'
import { getMyCustomerServiceApplications } from '@/api/merchantCustomerService'
import { getMyWallet, getMyTransactions, getMyWithdrawals, createWithdrawal, rechargeWallet, getMyTips } from '@/api/wallet'
import { getUserStatistics } from '@/api/statistics'
import { getPets } from '@/api/pet'
import { getOrders } from '@/api/order'
import { TransactionStatus, WithdrawalStatus, enrichWithStatus } from '@/constants/statusMaps'
import request from '@/utils/request'
import MediaWithFallback from '@/components/common/MediaWithFallback.vue'

const router = useRouter()
const authStore = useAuthStore()
const appStore = useAppStore()

/* ── status vocabulary (mirrors Orders/orderMeta) ── */
const ORDER_STATUS_LABEL = {
  pending: '待付款', paid: '已支付', confirmed: '待送达', delivered: '已送达', received: '已接收',
  in_progress: '服务中', completed: '已完成', cancelled: '已取消', refunding: '退款中', refunded: '已退款',
}
const ACTIVE_STATUSES = ['pending', 'paid', 'confirmed', 'delivered', 'received', 'in_progress']
const STAYING_STATUSES = ['delivered', 'received', 'in_progress']
const NON_BILLED = ['pending', 'cancelled', 'refunded']

const ROLE_LABEL = {
  OWNER: '宠物家长', USER: '宠物家长', MERCHANT: '商家', KEEPER: '照护师',
  CS: '客服', CUSTOMER_SERVICE: '客服', ADMIN: '管理员',
}
function roleLabel(role) { return ROLE_LABEL[String(role).toUpperCase()] || role }

const GENDER_LABEL = { 0: '未设置', 1: '男', 2: '女' }

function statusLabel(status) {
  return ORDER_STATUS_LABEL[String(status ?? '')] || status || '未知状态'
}
function orderBadgeTone(status) {
  const s = String(status ?? '')
  if (['pending', 'refunding'].includes(s)) return 'action'
  if (['delivered', 'received', 'in_progress'].includes(s)) return 'active'
  if (s === 'completed') return 'done'
  if (['cancelled', 'refunded'].includes(s)) return 'closed'
  return 'queued'
}

/* ── presentation helpers ── */
const TYPE_MAP = {
  recharge: '充值', admin_adjust: '管理员调整', order_pay: '订单支付',
  order_refund: '退款', withdrawal: '提现', membership: '会员', tip: '打赏',
}
function typeLabel(t) { return TYPE_MAP[t.type_wsh] || t.business_type_wsh || t.type_wsh }

function realNameBadgeMeta(status) {
  const map = {
    0: { label: '未实名', tone: 'closed' }, 1: { label: '实名审核中', tone: 'action' },
    2: { label: '已实名', tone: 'active' }, 3: { label: '实名被驳回', tone: 'action' },
  }
  return map[Number(status || 0)] || map[0]
}
function withdrawalBadge(status) {
  const map = {
    pending: { label: '待审核', tone: 'action' }, approved: { label: '已通过', tone: 'queued' },
    completed: { label: '已打款', tone: 'active' }, rejected: { label: '已驳回', tone: 'closed' },
    cancelled: { label: '已取消', tone: 'closed' },
  }
  return map[String(status ?? '')] || { label: status || '未知状态', tone: 'closed' }
}

function formatMoney(value) {
  const n = Number(value || 0)
  return Number.isFinite(n) ? n.toLocaleString('zh-CN', { maximumFractionDigits: 2 }) : '0.00'
}
function fmtTime(value) {
  return value ? String(value).replace('T', ' ').slice(0, 16) : '-'
}
function time(value) {
  const stamp = value ? new Date(value).getTime() : NaN
  return Number.isNaN(stamp) ? 0 : stamp
}
function orderAmount(order) {
  return Number(order.final_amount_wsh || order.total_amount_wsh || 0) || 0
}
function parseImages(value) {
  return String(value || '').split(',').map(u => u.trim()).filter(Boolean)
}
function orderServiceImage(order) {
  const images = parseImages(order.service_images_wsh)
  return images[0] || order.pet_avatar_wsh || ''
}
function orderDayRange(order) {
  const compact = (value) => {
    const day = value ? String(value).slice(0, 10) : ''
    return day ? day.slice(5).replace('-', '.') : '-'
  }
  const start = compact(order.start_date_wsh)
  const end = compact(order.end_date_wsh)
  if (start === '-' && end === '-') return '-'
  return `${start} — ${end}`
}
function normalizeList(data) {
  if (Array.isArray(data)) return data
  if (Array.isArray(data?.list)) return data.list
  return []
}

/* ── state ── */
const loading = ref(true)
const loadError = ref('')
const submitting = ref(false)
const deleting = ref(false)
const avatarUploading = ref(false)
const avatarInputRef = ref(null)

const profile = reactive({
  username_wsh: '', nickname_wsh: '', phone_wsh: '', avatar_wsh: '', gender_wsh: 0,
  email_wsh: '', real_name_wsh: '', id_card_no_wsh: '', real_name_status_wsh: 0,
  payment_password_set_wsh: false, created_at_wsh: '',
})
const stats = ref({ pets: 0, activeOrders: 0, completedOrders: 0, totalSpent: 0 })
const wallet = ref({})
const transactions = ref([])
const withdrawals = ref([])
const tips = ref([])
const orders = ref([])
const pets = ref([])
const merchantStatus = ref(null)
const keeperStatus = ref(null)
const keeperLoading = ref(true)
const customerServiceApplications = ref([])

/* ledger */
const ledgerTab = ref('transactions')
const ledgerTabs = [
  { key: 'transactions', label: '交易记录' },
  { key: 'withdrawals', label: '提现记录' },
  { key: 'tips', label: '打赏记录' },
]

/* dialogs */
const editDialogOpen = ref(false)
const rechargeDialogOpen = ref(false)
const withdrawDialogOpen = ref(false)
const deleteDialogOpen = ref(false)
const rechargeAmount = ref('')
const rechargeMethod = ref('wechat')
const rechargeSubmitting = ref(false)
const withdrawAmount = ref('')
const withdrawAccount = ref('')
const withdrawSubmitting = ref(false)

/* ── computed ── */
const avatarUrl = computed(() => profile.avatar_wsh || authStore.user?.avatar_wsh || '')
const displayName = computed(() => profile.nickname_wsh || authStore.user?.nickname_wsh || authStore.user?.username_wsh || '宠物家长')
const avatarInitial = computed(() => displayName.value.slice(0, 1).toUpperCase())
const joinedLabel = computed(() => {
  const value = profile.created_at_wsh || authStore.user?.created_at_wsh
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return ''
  return `${date.getFullYear()} 年 ${date.getMonth() + 1} 月加入`
})
const roleTags = computed(() => {
  const roles = authStore.user?.roles_wsh?.length ? authStore.user.roles_wsh : ['OWNER']
  return roles
})
const realNameBadge = computed(() => realNameBadgeMeta(profile.real_name_status_wsh))

const recentOrders = computed(() =>
  [...orders.value]
    .sort((a, b) => time(b.created_at_wsh) - time(a.created_at_wsh) || (b.id_wsh || 0) - (a.id_wsh || 0))
    .slice(0, 4)
)
const activeStay = computed(() =>
  orders.value.find(o => STAYING_STATUSES.includes(String(o.status_wsh))) ?? null
)

const summary = computed(() => {
  const list = orders.value
  const billed = list.filter(o => !NON_BILLED.includes(String(o.status_wsh)))
  return {
    pets: stats.value.pets ?? pets.value.length,
    activeOrders: stats.value.activeOrders ?? list.filter(o => ACTIVE_STATUSES.includes(String(o.status_wsh))).length,
    completedOrders: stats.value.completedOrders ?? list.filter(o => o.status_wsh === 'completed').length,
    totalSpent: stats.value.totalSpent ?? billed.reduce((total, o) => total + orderAmount(o), 0),
  }
})

const walletAvailable = computed(() =>
  Math.max(Number(wallet.value.balance_wsh || 0) - Number(wallet.value.frozen_amount_wsh || 0), 0)
)

const ledgerCounts = computed(() => ({
  transactions: transactions.value.length,
  withdrawals: withdrawals.value.length,
  tips: tips.value.length,
}))
const ledgerTabLabel = computed(() =>
  ledgerTabs.find(t => t.key === ledgerTab.value)?.label || ''
)

/* quick actions */
const quickActions = [
  { key: 'orders', label: '我的订单', desc: '进度、实拍与费用明细', to: '/orders', icon: Ticket },
  { key: 'coupons', label: '我的优惠券', desc: '可领取与已拥有的券', to: '/coupons', icon: Ticket },
  { key: 'membership', label: '会员中心', desc: '套餐、权益与会员订单', to: '/membership', icon: Medal },
  { key: 'services', label: '浏览照护服务', desc: '寄养、上门与美容', to: '/services', icon: Service },
  { key: 'tickets', label: '我的工单', desc: '申诉与客服沟通记录', to: '/tickets', icon: ChatDotRound },
  { key: 'complaints', label: '我的投诉', desc: '跟踪对订单与门店的投诉', to: '/complaints', icon: Warning },
]

/* role entries */
function merchantEntry(status) {
  if (status === 0) return { title: '商家审核中', desc: '平台正在核验你的入驻资料', to: '/merchants', badge: { label: '待审核', tone: 'action' } }
  if (status === 1) return { title: '商家中心', desc: '管理门店信息、服务与订单', to: '/merchant/dashboard' }
  if (status === 2) return { title: '入驻申请被拒绝', desc: '可补充资料后重新提交', to: '/merchants', badge: { label: '已拒绝', tone: 'closed' } }
  return { title: '申请成为商家', desc: '入驻平台，发布宠物寄养服务', to: '/merchants' }
}
function keeperEntry(status) {
  if (status === 0) return { title: '照护师审核中', desc: '商户正在审核你的申请', to: '/keeper-apply', badge: { label: '待审核', tone: 'action' } }
  if (status === 1) return { title: '照护师工作台', desc: '管理排班、订单与服务记录', to: '/keeper-workflow', badge: { label: '在线', tone: 'active' } }
  if (status === 3) return { title: '照护师工作台', desc: '当前为离线状态，可进入工作台切换', to: '/keeper-workflow', badge: { label: '离线', tone: 'closed' } }
  if (status === 4) return { title: '照护师工作台', desc: '当前为忙碌状态，可进入工作台切换', to: '/keeper-workflow', badge: { label: '忙碌', tone: 'action' } }
  if (status === 2) return { title: '照护师申请被拒绝', desc: '可补充资料后重新提交', to: '/keeper-apply', badge: { label: '已拒绝', tone: 'closed' } }
  return { title: '申请成为照护师', desc: '加入商户，提供宠物照护服务', to: '/keeper-apply' }
}
function csEntry(application) {
  const status = application?.status_wsh
  const merchantName = application?.merchant_name_wsh || `商家 #${application?.merchant_id_wsh ?? '-'}`
  if (!application) return { title: '申请成为商家客服', desc: '选择要服务的商家，由商家负责人审核', to: '/customer-service/apply' }
  const map = {
    pending: { label: '待审核', tone: 'action' }, approved: { label: '已通过', tone: 'active' },
    rejected: { label: '已拒绝', tone: 'closed' }, resigned: { label: '已退出', tone: 'closed' },
    terminated: { label: '已终止', tone: 'closed' },
  }
  return {
    title: status === 'approved' ? '商家客服已开通' : '客服申请进度',
    desc: merchantName,
    to: '/customer-service/apply',
    badge: map[status],
  }
}
const roleEntries = computed(() => [
  { key: 'merchant', ...merchantEntry(merchantStatus.value === null ? null : Number(merchantStatus.value)), icon: Shop },
  { key: 'keeper', ...keeperEntry(keeperStatus.value === null ? null : Number(keeperStatus.value)), icon: UserFilled },
  { key: 'cs', ...csEntry(customerServiceApplications.value[0] || null), icon: Headset },
])

/* account rows */
const accountRows = computed(() => {
  const verified = Number(profile.real_name_status_wsh || 0) === 2
  const realName = realNameBadgeMeta(profile.real_name_status_wsh)
  return [
    { key: 'username', label: '用户名', value: profile.username_wsh || '—', note: '由系统管理，不可修改' },
    {
      key: 'nickname', label: '昵称', value: profile.nickname_wsh || '未设置',
      action: { label: '修改', onClick: () => { editDialogOpen.value = true } },
    },
    {
      key: 'gender', label: '性别', value: GENDER_LABEL[Number(profile.gender_wsh || 0)] ?? '未设置',
      action: { label: '修改', onClick: () => { editDialogOpen.value = true } },
    },
    {
      key: 'phone', label: '手机号', value: profile.phone_wsh || '未绑定', note: '用于订单联系与安全通知',
      action: { label: profile.phone_wsh ? '修改' : '绑定', onClick: () => goAccountEdit('phone') },
    },
    {
      key: 'email', label: '邮箱', value: profile.email_wsh || '未绑定', note: '用于账号通知与找回密码',
      action: { label: profile.email_wsh ? '修改' : '绑定', onClick: () => goAccountEdit('email') },
    },
    {
      key: 'realName', label: '实名认证', value: profile.real_name_wsh || '未提交', badge: realName,
      action: verified ? undefined : { label: '去认证', onClick: () => goAccountEdit('real-name') },
    },
    {
      key: 'paymentPassword', label: '支付密码', value: profile.payment_password_set_wsh ? '已设置' : '未设置', note: '授权自动支付时校验',
      action: {
        label: profile.payment_password_set_wsh ? '修改' : '设置',
        onClick: () => goAccountEdit('payment-password'),
      },
    },
  ]
})

/* ── data loading ── */
async function loadDashboard() {
  loading.value = true
  loadError.value = ''
  try {
    await Promise.all([
      request.get('/users/me'),
      request.get('/statistics/user'),
    ]).then(([ur, sr]) => {
      if (ur.data.code === 200) syncUser(ur.data.data)
      if (sr.data.code === 200) stats.value = sr.data.data
    })
  } catch (e) {
    loadError.value = '无法连接账户服务，请稍后重试。'
  } finally {
    loading.value = false
  }
  void loadCommunity()
}

async function loadCommunity() {
  const tasks = [
    loadMerchantStatus(),
    loadKeeperStatus(),
    loadCustomerServiceApplications(),
    loadWallet(),
    loadTransactions(),
    loadWithdrawals(),
    loadTips(),
    loadOrders(),
    loadPets(),
  ]
  await Promise.all(tasks.map(t => t.catch(() => {})))
}

async function loadOrders() {
  try {
    const res = await getOrders()
    if (res?.code === 200) orders.value = normalizeList(res.data)
  } catch (e) { orders.value = [] }
}

async function loadPets() {
  let base = []
  try {
    const res = await getPets()
    if (res?.code === 200) base = normalizeList(res.data)
  } catch (e) { base = [] }

  const fromOrders = derivePetsFromOrders(orders.value)
  if (base.length) {
    pets.value = mergePetStats(base, orders.value)
  } else {
    pets.value = fromOrders
  }
}

function derivePetsFromOrders(list) {
  const sorted = [...list].sort(
    (a, b) => time(b.start_date_wsh) - time(a.start_date_wsh) || (b.id_wsh || 0) - (a.id_wsh || 0)
  )
  const map = new Map()
  sorted.forEach((order) => {
    if (!order.pet_name_wsh && !order.pet_id_wsh) return
    const key = String(order.pet_id_wsh ?? order.pet_name_wsh)
    const billed = !NON_BILLED.includes(String(order.status_wsh))
    const staying = STAYING_STATUSES.includes(String(order.status_wsh))
    const existing = map.get(key)
    if (existing) {
      existing.stays += 1
      if (billed) existing.spent += orderAmount(order)
      if (!existing.avatar) existing.avatar = order.pet_avatar_wsh
      if (!existing.activeStatus && staying) existing.activeStatus = String(order.status_wsh)
      return
    }
    map.set(key, {
      key,
      id: order.pet_id_wsh,
      name: order.pet_name_wsh || '未命名宠物',
      avatar: order.pet_avatar_wsh,
      breed: order.pet_breed_wsh,
      age: order.pet_age_wsh,
      weight: order.pet_weight_wsh,
      stays: 1,
      spent: billed ? orderAmount(order) : 0,
      activeStatus: staying ? String(order.status_wsh) : undefined,
    })
  })
  return [...map.values()]
}

function mergePetStats(basePets, orderList) {
  const stayMap = {}
  const billed = orderList.filter(o => !NON_BILLED.includes(String(o.status_wsh)))
  billed.forEach((order) => {
    const key = String(order.pet_id_wsh ?? order.pet_name_wsh)
    if (!order.pet_name_wsh && !order.pet_id_wsh) return
    stayMap[key] = stayMap[key] || { stays: 0, spent: 0 }
    stayMap[key].stays += 1
    stayMap[key].spent += orderAmount(order)
  })
  return basePets.map(p => {
    const key = String(p.id_wsh ?? p.name_wsh)
    const s = stayMap[key] || stayMap[String(p.name_wsh)] || { stays: 0, spent: 0 }
    return {
      key,
      name: p.name_wsh || p.name || '未命名宠物',
      avatar: p.avatar_wsh || p.avatar || '',
      breed: p.breed_wsh || p.breed,
      age: p.age_wsh ?? p.age,
      weight: p.weight_wsh ?? p.weight,
      stays: Number(p.stay_count_wsh ?? s.stays ?? 0),
      spent: Number(p.total_spent_wsh ?? s.spent ?? 0),
      activeStatus: p.active_status_wsh,
    }
  })
}

function petLine(pet) {
  const parts = [
    pet.breed,
    pet.age ? `${pet.age} 岁` : '',
    pet.weight ? `${pet.weight} kg` : '',
  ].filter(Boolean)
  return parts.join(' · ') || '资料待完善'
}

async function loadMerchantStatus() {
  try {
    const r = await request.get('/merchants/my')
    if (r.data.code === 200 && r.data.data) merchantStatus.value = r.data.data.status_wsh
  } catch (e) {}
}
async function loadKeeperStatus() {
  keeperLoading.value = true
  try {
    const r = await request.get('/api/keepers/my-application')
    if (r.data.code === 200 && r.data.data) keeperStatus.value = r.data.data.status_wsh
    else keeperStatus.value = null
  } catch (e) { keeperStatus.value = null } finally { keeperLoading.value = false }
}
async function loadCustomerServiceApplications() {
  try {
    const response = await getMyCustomerServiceApplications()
    if (response.code === 200) customerServiceApplications.value = response.data || []
  } catch (e) { customerServiceApplications.value = [] }
}
async function loadWallet() {
  try {
    const res = await getMyWallet()
    if (res.code === 200 && res.data) wallet.value = res.data
  } catch (e) {}
}
async function loadTransactions() {
  try {
    const res = await getMyTransactions()
    if (res.code === 200) transactions.value = normalizeList(res.data).map(t => enrichWithStatus(t, 'status_wsh', TransactionStatus))
  } catch (e) { appStore.addToast('交易记录加载失败', 'error') }
}
async function loadWithdrawals() {
  try {
    const res = await getMyWithdrawals()
    if (res.code === 200) withdrawals.value = normalizeList(res.data).map(w => enrichWithStatus(w, 'status_wsh', WithdrawalStatus))
  } catch (e) { appStore.addToast('提现记录加载失败', 'error') }
}
async function loadTips() {
  try {
    const res = await getMyTips()
    if (res.code === 200) tips.value = normalizeList(res.data)
  } catch (e) { appStore.addToast('打赏记录加载失败', 'error') }
}

async function reloadAll() {
  await loadDashboard()
}

/* ── actions ── */
function triggerAvatarInput() { avatarInputRef.value?.click() }
function onAvatarFile(event) {
  const file = event.target.files?.[0]
  if (file) void uploadAvatar(file)
  event.target.value = ''
}
async function uploadAvatar(file) {
  if (!file.type.startsWith('image/')) { appStore.addToast('请选择图片文件', 'error'); return }
  if (file.size > 5 * 1024 * 1024) { appStore.addToast('头像需小于 5MB', 'error'); return }
  avatarUploading.value = true
  try {
    const formData = new FormData()
    formData.append('file', file)
    const r = await request.post('/files/upload?directory=avatars', formData)
    if (r.data.code === 200 && r.data.data?.url_wsh) {
      profile.avatar_wsh = r.data.data.url_wsh
      const updateRes = await request.put('/users/me', { avatar_wsh: profile.avatar_wsh })
      if (updateRes.data.code === 200) syncUser(updateRes.data.data || { avatar_wsh: profile.avatar_wsh })
      appStore.addToast('头像上传成功', 'success')
    }
  } catch (error) { appStore.addToast('头像上传失败', 'error') } finally { avatarUploading.value = false }
}

async function submitProfileEdit() {
  if (submitting.value) return
  submitting.value = true
  try {
    const payload = { nickname_wsh: profile.nickname_wsh, avatar_wsh: profile.avatar_wsh, gender_wsh: profile.gender_wsh }
    const r = await request.put('/users/me', payload)
    if (r.data.code === 200) {
      appStore.addToast('保存成功', 'success')
      syncUser(r.data.data || profile)
      editDialogOpen.value = false
    }
  } catch (e) { appStore.addToast('保存失败', 'error') } finally { submitting.value = false }
}

function openRecharge() { rechargeDialogOpen.value = true }
function openWithdraw() { withdrawDialogOpen.value = true }

async function submitRecharge() {
  if (!rechargeAmount.value || Number(rechargeAmount.value) <= 0) {
    appStore.addToast('请输入大于 0 的充值金额', 'warning'); return
  }
  const reqId = `recharge-fe-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`
  rechargeSubmitting.value = true
  try {
    const res = await rechargeWallet({ amount_wsh: Number(Number(rechargeAmount.value).toFixed(2)), request_id_wsh: reqId })
    if (res.code === 200) {
      if (res.data) wallet.value = res.data
      rechargeAmount.value = ''
      appStore.addToast('充值成功，余额已到账', 'success')
      rechargeDialogOpen.value = false
      loadWallet()
      loadTransactions()
    } else { appStore.addToast(res.msg || '充值失败', 'error') }
  } catch (e) { appStore.addToast(e?.message || '充值失败', 'error') } finally { rechargeSubmitting.value = false }
}

async function submitWithdraw() {
  if (!withdrawAmount.value || !withdrawAccount.value) return
  if (Number(withdrawAmount.value) > walletAvailable.value) {
    appStore.addToast('提现金额超出可用余额', 'error'); return
  }
  withdrawSubmitting.value = true
  try {
    const res = await createWithdrawal({ amount_wsh: withdrawAmount.value, account_name_wsh: withdrawAccount.value, bank_name_wsh: '', bank_card_wsh: '' })
    if (res.code === 200) {
      appStore.addToast('提现申请已提交，审核期间金额将被冻结', 'success')
      withdrawAmount.value = ''
      withdrawAccount.value = ''
      withdrawDialogOpen.value = false
      await loadWallet()
      await loadWithdrawals()
    }
  } catch (e) { appStore.addToast('提交失败', 'error') } finally { withdrawSubmitting.value = false }
}

function goRoleEntry(entry) {
  if (!entry.to) return
  router.push(entry.to)
}

function goAccountEdit(type) { router.push('/profile/' + type) }

async function confirmDeleteAccount() {
  if (deleting.value) return
  deleting.value = true
  try {
    const r = await deleteCurrentUser()
    if (r.code === 200) {
      appStore.addToast('账号已删除', 'success')
      authStore.clearAuth()
      router.replace('/login')
    }
  } catch (e) { appStore.addToast(e.response?.data?.message || '删除失败', 'error') } finally { deleting.value = false }
}

function syncUser(userData) {
  if (!userData) return
  Object.assign(profile, userData)
  if (authStore.user) {
    authStore.user = { ...authStore.user, ...userData }
    localStorage.setItem('user', JSON.stringify(authStore.user))
  }
}

/* deep link: /profile#account */
onMounted(() => {
  void loadDashboard()
  nextTick(() => {
    const hash = window.location.hash
    if (hash && !loading.value) {
      const target = document.getElementById(hash.slice(1))
      if (target) target.scrollIntoView({ behavior: 'smooth', block: 'start' })
    }
  })
})
</script>

<style scoped>
/* ═══════════════════════════════════════════════════════
   Uses shared --ref-* tokens from assets/css/design-tokens.css.
   Dark mode is handled globally via html[data-theme="dark"].
   Intentional dark "paper" panels use a fixed #17130f slab.
   ═══════════════════════════════════════════════════════ */
.pr-page {
  --paper: #17130f;
  --cream-fixed: #f5efe7;
  --r-tag: 6px;
  --r-btn: 10px;
  --r-card: 14px;
  --r-panel: 20px;
  --r-frame: 26px;
  width: 100%;
  padding: 6px 0 72px;
  min-height: 60vh;
  background: var(--ref-canvas);
  color: var(--ref-ink);
}

.pr-shell { max-width: 1180px; margin: 0 auto; padding: 0 24px; }

/* ═══ Breadcrumb ═══ */
.pr-crumb {
  display: flex; align-items: center; gap: 8px;
  padding: 6px 0; font-size: 12.5px; color: var(--ref-muted);
}
.pr-crumb-link { color: var(--ref-muted); text-decoration: none; }
.pr-crumb-link:hover { color: var(--ref-ink); }
.pr-crumb-sep { color: var(--ref-line); }
.pr-crumb-here { color: var(--ref-ink-soft); }

/* ═══ Badges ═══ */
.badge {
  display: inline-flex; align-items: center; gap: 6px;
  border-radius: var(--r-tag); border: 1px solid transparent;
  padding: 4px 10px; font-size: 11px; font-weight: 500; line-height: 1; white-space: nowrap;
}
.badge-action { background: var(--ref-brand); color: #fff; }
.badge-queued { background: var(--ref-surface); color: var(--ref-ink); border-color: color-mix(in srgb, var(--ref-ink) 20%, transparent); }
.badge-active { background: color-mix(in srgb, var(--ref-moss, #3f5347) 12%, transparent); color: var(--ref-moss, #3f5347); border-color: color-mix(in srgb, var(--ref-moss, #3f5347) 25%, transparent); }
.badge-done { background: color-mix(in srgb, var(--ref-ink) 5%, transparent); color: var(--ref-ink-soft); }
.badge-closed { background: transparent; color: var(--ref-muted); border-color: var(--ref-line); }
.tag-pill {
  display: inline-flex; align-items: center;
  border-radius: var(--r-tag); border: 1px solid var(--ref-line);
  background: color-mix(in srgb, var(--ref-cream) 70%, var(--ref-surface));
  padding: 4px 10px; font-size: 11px; font-weight: 500; line-height: 1; color: var(--ref-ink-soft);
}

/* ═══ CTA ═══ */
.cta {
  display: inline-flex; align-items: center; justify-content: center; gap: 8px;
  height: 42px; padding: 0 18px; border-radius: var(--r-btn);
  font-size: 13px; font-weight: 500; cursor: pointer; border: 1px solid transparent;
  transition: background 0.15s, border-color 0.15s, color 0.15s, transform 0.15s;
}
.cta:hover { transform: translateY(-1px); }
.cta:disabled { opacity: 0.5; cursor: not-allowed; transform: none; }
.cta-primary { background: var(--ref-brand); color: #fff; }
.cta-primary:hover:not(:disabled) { background: var(--ref-brand-deep); }
.cta-dark { background: var(--paper); color: var(--cream-fixed); }
.cta-dark:hover { background: #000; }
.cta-outline { background: var(--ref-surface); color: var(--ref-ink); border-color: var(--ref-line); }
.cta-outline:hover { border-color: color-mix(in srgb, var(--ref-ink) 35%, transparent); }
.text-link {
  display: inline-flex; align-items: center; gap: 6px;
  font-size: 13px; font-weight: 500; color: var(--ref-ink);
  text-decoration: none; transition: color 0.15s;
}
.text-link:hover { color: var(--ref-brand); }
.tl-text { border-bottom: 1px solid color-mix(in srgb, var(--ref-ink) 25%, transparent); padding-bottom: 2px; }
.text-link:hover .tl-text { border-color: var(--ref-brand); }
.tl-arrow { transition: transform 0.15s; }
.text-link:hover .tl-arrow { transform: translateX(4px); }

/* ═══ PROFILE HERO ═══ */
.ph-card {
  position: relative; overflow: hidden;
  border: 1px solid var(--ref-line); border-radius: var(--r-frame);
  background: var(--ref-surface);
  box-shadow: 0 2px 4px -2px rgba(13, 33, 26, 0.08);
}
.ph-top {
  display: flex; flex-wrap: wrap; align-items: flex-start; justify-content: space-between;
  gap: 28px 40px; padding: 28px 28px 32px;
}
.ph-identity { display: flex; min-width: 0; gap: 24px; }
.ph-avatar-btn {
  position: relative; display: block; width: 104px; height: 104px; flex-shrink: 0;
  overflow: hidden; border-radius: var(--r-panel);
  border: 1px solid var(--ref-line); background: var(--ref-sand);
  cursor: pointer; padding: 0;
  transition: border-color 0.2s, box-shadow 0.2s;
}
.ph-avatar-btn:hover:not(:disabled) {
  border-color: color-mix(in srgb, var(--ref-ink) 20%, transparent);
  box-shadow: 0 18px 40px -26px color-mix(in srgb, var(--ref-ink) 55%, transparent);
}
.ph-avatar-btn:disabled { opacity: 0.7; cursor: wait; }
.ph-avatar-img { width: 100%; height: 100%; object-fit: cover; transition: transform 0.3s cubic-bezier(0.23, 1, 0.32, 1); }
.ph-avatar-btn:hover .ph-avatar-img { transform: scale(1.05); }
.ph-avatar-initial {
  display: flex; width: 100%; height: 100%; align-items: center; justify-content: center;
  font-family: var(--ref-font-display); font-size: 2rem; color: var(--ref-ink-soft);
}
.ph-avatar-overlay {
  position: absolute; inset: 0; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 4px;
  background: color-mix(in srgb, var(--paper) 55%, transparent); color: var(--cream-fixed);
  font-size: 11px; opacity: 0; transition: opacity 0.15s;
}
.ph-avatar-btn:hover .ph-avatar-overlay, .ph-avatar-btn:focus-visible .ph-avatar-overlay { opacity: 1; }
.ph-avatar-overlay .el-icon { font-size: 15px; }
.ph-avatar-busy {
  position: absolute; inset: 0; display: flex; align-items: center; justify-content: center;
  background: color-mix(in srgb, var(--paper) 60%, transparent);
}
.ph-spinner {
  width: 20px; height: 20px; border-radius: 50%;
  border: 2px solid rgba(255, 255, 255, 0.25); border-left-color: var(--cream-fixed);
  animation: pr-spin 0.8s linear infinite;
}
.ph-avatar-file { display: none; }
.ph-id-copy { min-width: 0; }
.ph-eyebrow {
  display: flex; align-items: center; gap: 10px;
  font-size: 9.5px; letter-spacing: 0.28em; text-transform: uppercase; color: var(--ref-muted);
}
.ph-eyebrow-line { width: 24px; height: 1px; background: var(--ref-line); }
.ph-eyebrow-idx { letter-spacing: 0.28em; }
.ph-name {
  margin: 14px 0 0; font-family: var(--ref-font-display);
  font-size: clamp(30px, 3.6vw, 42px); font-weight: 500; line-height: 1.1;
  letter-spacing: -0.01em; color: var(--ref-ink); text-wrap: balance;
}
.ph-meta {
  display: flex; flex-wrap: wrap; align-items: center; gap: 8px;
  margin: 10px 0 0; font-size: 13px; color: var(--ref-muted);
}
.ph-meta-sep { color: var(--ref-line); }
.ph-tags { display: flex; flex-wrap: wrap; align-items: center; gap: 8px; margin-top: 14px; }
.ph-actions { display: flex; flex-wrap: wrap; align-items: center; gap: 10px; flex-shrink: 0; padding-top: 4px; }
.ph-link-btn {
  display: inline-flex; align-items: center; height: 42px; padding: 0 16px;
  border: 1px solid var(--ref-line); border-radius: var(--r-btn);
  color: var(--ref-ink-soft); font-size: 13px; font-weight: 500; text-decoration: none;
  transition: border-color 0.15s, color 0.15s;
}
.ph-link-btn:hover { border-color: color-mix(in srgb, var(--ref-ink) 30%, transparent); color: var(--ref-ink); }

/* active stay */
.ph-stay {
  display: flex; flex-wrap: wrap; align-items: center; gap: 10px 18px;
  padding: 14px 28px; border-top: 1px solid var(--ref-line);
  background: color-mix(in srgb, var(--ref-cream) 60%, transparent);
}
.ph-stay-dot { width: 6px; height: 6px; flex-shrink: 0; border-radius: 50%; background: var(--ref-brand); }
.ph-stay-text { margin: 0; min-width: 0; font-size: 13px; color: var(--ref-ink-soft); }
.ph-stay-name { font-weight: 500; color: var(--ref-ink); }
.ph-stay-sep { margin: 0 6px; color: var(--ref-line); }
.ph-stay-link { margin-left: auto; font-size: 13px; font-weight: 500; color: var(--ref-ink); text-decoration: none; transition: color 0.15s; }
.ph-stay-link:hover { color: var(--ref-brand); }

/* metrics */
.ph-metrics {
  display: grid; grid-template-columns: repeat(4, 1fr);
  border-top: 1px solid var(--ref-line); margin: 0;
}
.ph-metric {
  padding: 22px 28px;
  border-left: 1px solid var(--ref-line);
}
.ph-metric:first-child { border-left: 0; }
.ph-m-label { font-size: 10.5px; letter-spacing: 0.16em; text-transform: uppercase; color: var(--ref-muted); }
.ph-m-value {
  margin: 10px 0 0; font-family: var(--ref-font-display);
  font-size: 28px; font-weight: 400; line-height: 1; letter-spacing: -0.01em;
  color: var(--ref-ink); font-variant-numeric: tabular-nums;
}
.ph-m-unit { margin-left: 5px; font-size: 11px; color: var(--ref-muted); }
.ph-metric-brand { background: color-mix(in srgb, var(--ref-cream) 60%, transparent); }
.ph-m-value-brand { color: var(--ref-brand); font-size: 30px; }
.ph-m-yen { font-size: 12px; margin-right: 4px; color: color-mix(in srgb, var(--ref-brand) 70%, transparent); }

/* ═══ SECTION SHELL ═══ */
.ps-section { margin-top: 48px; }
.ps-scroll-target { scroll-margin-top: 32px; }
.ps-head {
  display: flex; flex-wrap: wrap; align-items: flex-end; justify-content: space-between;
  gap: 16px 24px;
}
.ps-head-copy { min-width: 0; }
.ps-eyebrow {
  display: flex; align-items: center; gap: 10px;
  font-size: 10px; letter-spacing: 0.22em; text-transform: uppercase; color: var(--ref-muted);
}
.ps-idx { font-variant-numeric: tabular-nums; }
.ps-line { width: 24px; height: 1px; background: var(--ref-line); }
.ps-title {
  margin: 10px 0 0; font-family: var(--ref-font-display);
  font-size: 24px; font-weight: 400; line-height: 1.2; letter-spacing: -0.02em; color: var(--ref-ink);
}
.ps-desc { margin: 8px 0 0; max-width: 520px; font-size: 13px; line-height: 1.7; color: var(--ref-ink-soft); opacity: 0.8; }
.ps-action { padding-bottom: 4px; flex-shrink: 0; }
.ps-duo { display: grid; grid-template-columns: 1.55fr 1fr; gap: 24px 40px; }

/* ═══ PETS ═══ */
.pets-grid {
  display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; margin-top: 20px;
}
.pet-card {
  position: relative; display: block; overflow: hidden;
  border: 1px solid var(--ref-line); border-radius: var(--r-card);
  background: var(--ref-surface); text-decoration: none;
  transition: transform 0.2s cubic-bezier(0.23, 1, 0.32, 1), box-shadow 0.2s, border-color 0.2s;
}
.pet-card:hover {
  transform: translateY(-4px); border-color: color-mix(in srgb, var(--ref-ink) 15%, transparent);
  box-shadow: 0 28px 60px -40px color-mix(in srgb, var(--ref-ink) 45%, transparent);
}
.pet-media { position: relative; overflow: hidden; }
.pet-img { aspect-ratio: 4 / 5; }
.pet-img :deep(.media-image) { transition: transform 0.3s cubic-bezier(0.23, 1, 0.32, 1); }
.pet-card:hover .pet-img :deep(.media-image) { transform: scale(1.06); }
.pet-live-badge {
  position: absolute; top: 14px; left: 14px;
  display: inline-flex; align-items: center; gap: 6px;
  padding: 5px 10px; border-radius: var(--r-tag);
  background: var(--ref-brand); color: #fff; font-size: 11px; font-weight: 500;
}
.pet-live-dot { width: 6px; height: 6px; border-radius: 50%; background: rgba(255, 255, 255, 0.85); }
.pet-arrow {
  position: absolute; right: 14px; bottom: 58px;
  display: flex; align-items: center; justify-content: center;
  width: 34px; height: 34px; border-radius: 50%;
  background: var(--ref-surface); color: var(--ref-ink); font-size: 15px;
  opacity: 0; transform: translateY(6px);
  box-shadow: 0 10px 24px -14px color-mix(in srgb, var(--ref-ink) 60%, transparent);
  transition: opacity 0.2s, transform 0.2s;
}
.pet-card:hover .pet-arrow { opacity: 1; transform: translateY(0); }
.pet-plate {
  position: absolute; inset-inline: 0; bottom: 0;
  padding: 14px 16px;
  background: color-mix(in srgb, var(--paper) 55%, transparent); backdrop-filter: blur(6px);
}
.pet-name { margin: 0; font-family: var(--ref-font-display); font-size: 19px; font-weight: 500; color: var(--cream-fixed); }
.pet-line { margin: 4px 0 0; font-size: 11px; color: rgba(255, 255, 255, 0.75); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.pet-foot {
  display: flex; align-items: center; justify-content: space-between; gap: 12px;
  padding: 12px 16px; font-size: 11px; color: var(--ref-muted);
}
.pet-num { color: var(--ref-ink-soft); font-variant-numeric: tabular-nums; font-weight: 500; }

/* ═══ RECENT ORDERS ═══ */
.orders-list {
  margin-top: 20px; overflow: hidden;
  border: 1px solid var(--ref-line); border-radius: var(--r-card); background: var(--ref-surface);
}
.order-row {
  display: flex; align-items: center; gap: 16px;
  padding: 16px 20px; border-bottom: 1px solid var(--ref-line);
  text-decoration: none; transition: background 0.15s;
}
.order-row:last-child { border-bottom: 0; }
.order-row:hover { background: color-mix(in srgb, var(--ref-cream) 50%, transparent); }
.or-thumb {
  width: 56px; height: 56px; flex-shrink: 0; overflow: hidden; border-radius: var(--r-btn);
  background: var(--ref-sand);
}
.or-thumb :deep(.media-image) { transition: transform 0.3s cubic-bezier(0.23, 1, 0.32, 1); }
.order-row:hover .or-thumb :deep(.media-image) { transform: scale(1.06); }
.or-main { min-width: 0; flex: 1; }
.or-name {
  display: block; font-size: 14px; font-weight: 500; letter-spacing: -0.01em; color: var(--ref-ink);
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
  transition: color 0.15s;
}
.order-row:hover .or-name { color: var(--ref-brand); }
.or-meta {
  display: flex; align-items: center; gap: 8px; margin-top: 6px;
  font-size: 11px; color: var(--ref-muted);
}
.or-trunc { white-space: nowrap; overflow: hidden; text-overflow: ellipsis; max-width: 120px; }
.or-sep { color: var(--ref-line); }
.or-side { display: flex; flex-direction: column; align-items: flex-end; gap: 8px; flex-shrink: 0; }
.or-price { font-size: 13.5px; font-weight: 500; color: var(--ref-ink); font-variant-numeric: tabular-nums; }

/* ═══ QUICK ACTIONS ═══ */
.quick-nav { margin-top: 20px; overflow: hidden; border: 1px solid var(--ref-line); border-radius: var(--r-card); background: var(--ref-surface); }
.quick-nav ul { margin: 0; padding: 0; list-style: none; }
.quick-row {
  display: flex; align-items: center; gap: 14px;
  padding: 14px 20px; border-bottom: 1px solid var(--ref-line);
  text-decoration: none; transition: background 0.15s;
}
.quick-row:last-child { border-bottom: 0; }
.quick-row:hover { background: color-mix(in srgb, var(--ref-cream) 50%, transparent); }
.quick-icon {
  display: flex; align-items: center; justify-content: center;
  width: 36px; height: 36px; flex-shrink: 0; border-radius: var(--r-btn);
  background: var(--ref-sand); color: var(--ref-brand); font-size: 16px;
  transition: background 0.15s, color 0.15s;
}
.quick-row:hover .quick-icon { background: var(--ref-brand); color: #fff; }
.quick-copy { min-width: 0; flex: 1; }
.quick-title { display: block; font-size: 13.5px; font-weight: 500; letter-spacing: -0.01em; color: var(--ref-ink); transition: color 0.15s; }
.quick-row:hover .quick-title { color: var(--ref-brand); }
.quick-desc { display: block; margin-top: 2px; font-size: 11px; color: var(--ref-muted); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.quick-arrow { flex-shrink: 0; font-size: 14px; color: var(--ref-muted); transition: transform 0.15s, color 0.15s; }
.quick-row:hover .quick-arrow { transform: translateX(3px); color: var(--ref-brand); }

/* ═══ WALLET ═══ */
.wp-grid { display: grid; grid-template-columns: minmax(0, 320px) 1fr; gap: 16px; margin-top: 20px; }
.wp-balance {
  display: flex; flex-direction: column; justify-content: space-between; gap: 32px;
  padding: 24px; border-radius: var(--r-panel); background: var(--paper); color: var(--cream-fixed);
}
.wp-b-label { margin: 0; font-size: 10px; letter-spacing: 0.22em; text-transform: uppercase; color: rgba(255, 255, 255, 0.45); }
.wp-b-amount {
  margin: 10px 0 0; display: flex; align-items: baseline; gap: 6px;
  font-family: var(--ref-font-display); font-size: 38px; font-weight: 400; line-height: 1; letter-spacing: -0.02em;
  font-variant-numeric: tabular-nums;
}
.wp-b-yen { font-size: 14px; color: rgba(255, 255, 255, 0.55); }
.wp-b-sub {
  display: grid; grid-template-columns: 1fr 1fr; gap: 16px;
  margin-top: 20px; padding-top: 16px; border-top: 1px solid rgba(255, 255, 255, 0.12);
}
.wp-b-sub-label { margin: 0; font-size: 10px; color: rgba(255, 255, 255, 0.45); }
.wp-b-sub-val { margin: 6px 0 0; font-size: 14px; font-variant-numeric: tabular-nums; }
.wp-b-actions { display: flex; gap: 10px; }
.wp-btn {
  height: 40px; flex: 1; border-radius: var(--r-btn);
  font-size: 13px; font-weight: 500; cursor: pointer; transition: background 0.15s, border-color 0.15s, transform 0.15s;
}
.wp-btn:hover { transform: translateY(-1px); }
.wp-btn-light { background: var(--cream-fixed); color: var(--paper); border: 1px solid transparent; }
.wp-btn-light:hover { background: #fff; }
.wp-btn-ghost { background: transparent; color: var(--cream-fixed); border: 1px solid rgba(255, 255, 255, 0.25); }
.wp-btn-ghost:hover { border-color: rgba(255, 255, 255, 0.45); background: rgba(255, 255, 255, 0.1); }
.wp-b-note { margin: 16px 0 0; font-size: 11px; line-height: 1.7; color: rgba(255, 255, 255, 0.45); }

.wp-ledger { overflow: hidden; border: 1px solid var(--ref-line); border-radius: var(--r-card); background: var(--ref-surface); }
.ledger-tabs { display: flex; gap: 4px; overflow-x: auto; padding: 0 8px; border-bottom: 1px solid var(--ref-line); scrollbar-width: none; }
.ledger-tabs::-webkit-scrollbar { display: none; }
.ledger-tab {
  position: relative; flex-shrink: 0; padding: 14px 14px 12px;
  background: none; border: none; cursor: pointer;
  font-size: 13.5px; letter-spacing: -0.01em; color: var(--ref-muted); transition: color 0.15s;
}
.ledger-tab:hover { color: var(--ref-ink-soft); }
.ledger-tab.active { color: var(--ref-ink); }
.ledger-count {
  margin-left: 6px; font-size: 11px; color: var(--ref-muted); font-variant-numeric: tabular-nums;
}
.ledger-tab.active .ledger-count { color: var(--ref-brand); }
.ledger-tab.active::after {
  content: ''; position: absolute; inset-inline: 12px; bottom: -1px; height: 2px;
  border-radius: 2px; background: var(--ref-brand);
}
.ledger-body { min-height: 220px; }
.ledger-list { margin: 0; padding: 0; list-style: none; }
.ledger-row {
  display: flex; align-items: center; justify-content: space-between; gap: 16px;
  padding: 14px 20px; border-bottom: 1px solid var(--ref-line);
}
.ledger-row:last-child { border-bottom: 0; }
.lr-main { min-width: 0; }
.lr-title { margin: 0; font-size: 13.5px; letter-spacing: -0.01em; color: var(--ref-ink); }
.lr-title b { font-variant-numeric: tabular-nums; }
.lr-meta {
  display: flex; align-items: center; gap: 6px; margin: 4px 0 0;
  font-size: 11px; color: var(--ref-muted);
}
.lr-trunc { white-space: nowrap; overflow: hidden; text-overflow: ellipsis; max-width: 200px; }
.lr-sep { color: var(--ref-line); }
.lr-side { flex-shrink: 0; text-align: right; }
.lr-side-row { display: flex; align-items: center; gap: 12px; }
.lr-amount { margin: 0; font-size: 13.5px; font-weight: 500; font-variant-numeric: tabular-nums; }
.lr-in { color: var(--ref-moss, #3f5347); }
.lr-out { color: var(--ref-ink); }
.lr-balance { margin: 4px 0 0; font-size: 11px; color: var(--ref-muted); font-variant-numeric: tabular-nums; }
.ledger-row-tip { display: block; }
.lr-tip-head { display: flex; align-items: baseline; justify-content: space-between; gap: 12px; }
.lr-tip-msg { margin: 6px 0 0; font-size: 11.5px; line-height: 1.6; color: var(--ref-muted); }
.ledger-empty {
  display: flex; align-items: center; justify-content: center;
  min-height: 220px; font-size: 13px; color: var(--ref-muted);
}
.ledger-more { margin: 0; padding: 12px 20px; border-top: 1px solid var(--ref-line); font-size: 11px; color: var(--ref-muted); }
.ledger-more b { color: var(--ref-ink-soft); font-variant-numeric: tabular-nums; }

/* ═══ ROLES ═══ */
.rr-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; margin-top: 20px; }
.rr-card {
  display: flex; align-items: center; gap: 14px;
  padding: 16px 20px; border: 1px solid var(--ref-line); border-radius: var(--r-card);
  background: var(--ref-surface); text-decoration: none;
  transition: transform 0.2s, box-shadow 0.2s, border-color 0.2s;
}
a.rr-card:hover {
  transform: translateY(-2px); border-color: color-mix(in srgb, var(--ref-ink) 15%, transparent);
  box-shadow: 0 22px 50px -38px color-mix(in srgb, var(--ref-ink) 45%, transparent);
}
.rr-card.is-static { cursor: default; }
.rr-icon {
  display: flex; align-items: center; justify-content: center;
  width: 40px; height: 40px; flex-shrink: 0; border-radius: var(--r-btn);
  background: var(--ref-sand); color: var(--ref-brand); font-size: 17px;
}
.rr-copy { min-width: 0; flex: 1; }
.rr-title { display: block; font-size: 14px; font-weight: 500; letter-spacing: -0.01em; color: var(--ref-ink); }
.rr-desc { display: block; margin-top: 4px; font-size: 11px; line-height: 1.5; color: var(--ref-muted); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.rr-arrow { flex-shrink: 0; font-size: 14px; color: var(--ref-muted); }
a.rr-card:hover .rr-arrow { transform: translateX(3px); color: var(--ref-brand); }

/* ═══ ACCOUNT PANEL ═══ */
.ap-panel { margin-top: 20px; overflow: hidden; border: 1px solid var(--ref-line); border-radius: var(--r-card); background: var(--ref-surface); }
.ap-grid { display: grid; grid-template-columns: repeat(2, 1fr); }
.ap-row {
  display: flex; align-items: flex-start; justify-content: space-between; gap: 16px;
  padding: 16px 20px; border-bottom: 1px solid var(--ref-line);
}
.ap-row:nth-child(odd) { border-right: 1px solid var(--ref-line); }
.ap-row-last { border-bottom: 0; }
.ap-copy { min-width: 0; }
.ap-label { margin: 0; font-size: 10px; letter-spacing: 0.16em; text-transform: uppercase; color: var(--ref-muted); }
.ap-value {
  display: flex; flex-wrap: wrap; align-items: center; gap: 8px;
  margin: 8px 0 0; font-size: 14px; letter-spacing: -0.01em; color: var(--ref-ink);
}
.ap-value-text { white-space: nowrap; overflow: hidden; text-overflow: ellipsis; max-width: 100%; }
.ap-note { margin: 6px 0 0; font-size: 11px; color: var(--ref-muted); }
.ap-action {
  flex-shrink: 0; padding: 20px 0 0; background: none; border: none; cursor: pointer;
  font-size: 12.5px; font-weight: 500; color: var(--ref-brand); transition: color 0.15s;
}
.ap-action:hover { color: var(--ref-brand-deep); }
.ap-delete {
  display: flex; flex-wrap: wrap; align-items: center; justify-content: space-between; gap: 16px;
  margin-top: 16px; padding: 16px 20px;
  border: 1px solid color-mix(in srgb, #b3402a 20%, transparent); border-radius: var(--r-card);
  background: color-mix(in srgb, #b3402a 4%, var(--ref-surface));
}
.ap-delete-copy { min-width: 0; }
.ap-delete-title { margin: 0; font-size: 13.5px; font-weight: 500; color: var(--ref-ink); }
.ap-delete-desc { margin: 6px 0 0; max-width: 480px; font-size: 11px; line-height: 1.6; color: var(--ref-muted); }
.ap-delete-btn {
  display: inline-flex; align-items: center; justify-content: center;
  height: 38px; padding: 0 16px; flex-shrink: 0;
  border-radius: var(--r-btn); border: 1px solid color-mix(in srgb, #b3402a 35%, transparent);
  background: transparent; color: #a03422; font-size: 13px; font-weight: 500; cursor: pointer;
  transition: background 0.15s, transform 0.15s;
}
.ap-delete-btn:hover:not(:disabled) { background: color-mix(in srgb, #b3402a 10%, transparent); transform: translateY(-1px); }
.ap-delete-btn:disabled { opacity: 0.5; cursor: not-allowed; }

/* ═══ EMPTY / ERROR / SKELETON ═══ */
.ps-empty {
  margin-top: 20px; padding: 48px 24px;
  border: 1px dashed var(--ref-line); border-radius: var(--r-card);
  background: var(--ref-surface); text-align: center;
}
.ps-empty-sm { padding: 36px 20px; }
.ps-empty-title { margin: 0; font-family: var(--ref-font-display); font-size: 18px; font-weight: 500; color: var(--ref-ink); }
.ps-empty-desc { margin: 8px 0 0; font-size: 13px; color: var(--ref-muted); }
.ps-empty .cta { margin-top: 18px; }
.pr-error {
  padding: 64px 24px; border: 1px dashed var(--ref-line); border-radius: var(--r-frame);
  background: var(--ref-surface); text-align: center;
}
.pr-error-title { margin: 0; font-family: var(--ref-font-display); font-size: 22px; font-weight: 500; color: var(--ref-ink); }
.pr-error-desc { margin: 10px 0 0; font-size: 13.5px; color: var(--ref-muted); }
.pr-error .cta { margin-top: 22px; }
.ph-skeleton {
  height: 320px; border-radius: var(--r-frame);
  border: 1px solid var(--ref-line);
  background: linear-gradient(90deg, var(--ref-sand) 25%, var(--ref-surface) 50%, var(--ref-sand) 75%);
  background-size: 200% 100%; animation: pr-shimmer 1.3s linear infinite;
}
.ps-skeleton-stack { display: grid; gap: 20px; margin-top: 32px; }
.ps-skeleton { height: 180px; border-radius: var(--r-card); border: 1px solid var(--ref-line); background: var(--ref-sand); animation: pr-pulse 1.4s ease-in-out infinite; }

/* ═══ DIALOG ═══ */
.dlg-overlay {
  position: fixed; inset: 0; z-index: 2000;
  display: flex; align-items: center; justify-content: center;
  padding: 20px; background: rgba(10, 8, 6, 0.5); backdrop-filter: blur(2px);
  animation: pr-fade 0.15s ease;
}
.dlg-panel {
  width: 100%; max-width: 420px; max-height: min(90vh, 720px); overflow-y: auto;
  border-radius: var(--r-panel); background: var(--ref-surface);
  border: 1px solid var(--ref-line);
  box-shadow: 0 40px 80px -40px color-mix(in srgb, var(--ref-ink) 60%, transparent);
  animation: pr-pop 0.18s cubic-bezier(0.23, 1, 0.32, 1);
}
.dlg-head {
  display: flex; align-items: center; justify-content: space-between;
  padding: 20px 24px 0;
}
.dlg-title { margin: 0; font-family: var(--ref-font-display); font-size: 20px; font-weight: 500; color: var(--ref-ink); }
.dlg-close {
  display: flex; align-items: center; justify-content: center;
  width: 32px; height: 32px; border-radius: 8px; background: transparent; border: none;
  color: var(--ref-muted); font-size: 13px; cursor: pointer; transition: background 0.15s, color 0.15s;
}
.dlg-close:hover { background: var(--ref-sand); color: var(--ref-ink); }
.dlg-body { padding: 20px 24px 8px; display: grid; gap: 16px; }
.dlg-avatar-row { display: flex; align-items: center; gap: 14px; }
.dlg-avatar {
  position: relative; width: 64px; height: 64px; flex-shrink: 0; overflow: hidden;
  border-radius: 14px; border: 1px solid var(--ref-line); background: var(--ref-sand);
  font-family: var(--ref-font-display); font-size: 20px; color: var(--ref-ink-soft);
  display: flex; align-items: center; justify-content: center; cursor: pointer;
}
.dlg-avatar img { width: 100%; height: 100%; object-fit: cover; }
.dlg-avatar-overlay {
  position: absolute; inset: 0; display: flex; align-items: center; justify-content: center;
  background: color-mix(in srgb, var(--paper) 50%, transparent); color: var(--cream-fixed);
  font-size: 16px; opacity: 0; transition: opacity 0.15s;
}
.dlg-avatar:hover .dlg-avatar-overlay { opacity: 1; }
.dlg-avatar-hint { margin: 0; font-size: 12px; color: var(--ref-muted); line-height: 1.6; }
.dlg-field { display: grid; gap: 8px; }
.dlg-label { font-size: 12.5px; font-weight: 500; color: var(--ref-ink-soft); }
.dlg-input {
  width: 100%; height: 42px; padding: 0 14px;
  border: 1px solid var(--ref-line); border-radius: var(--r-btn);
  background: var(--ref-surface); color: var(--ref-ink); font-size: 14px;
  transition: border-color 0.15s, box-shadow 0.15s;
}
.dlg-input::placeholder { color: var(--ref-muted); }
.dlg-input:focus { outline: none; border-color: color-mix(in srgb, var(--ref-ink) 35%, transparent); box-shadow: 0 0 0 3px color-mix(in srgb, var(--ref-brand) 12%, transparent); }
.dlg-balance {
  display: grid; gap: 4px; padding: 16px;
  border: 1px solid var(--ref-line); border-radius: var(--r-card);
  background: var(--ref-sand);
}
.dlg-balance span, .dlg-balance small { font-size: 12px; color: var(--ref-muted); }
.dlg-balance b { font-family: var(--ref-font-display); font-size: 28px; font-weight: 400; color: var(--ref-brand); font-variant-numeric: tabular-nums; }
.dlg-note { margin: 0; font-size: 11.5px; line-height: 1.7; color: var(--ref-muted); }
.dlg-warn { margin: 0; font-size: 14px; line-height: 1.7; color: var(--ref-ink-soft); }
.dlg-detail { margin: 10px 0 0; padding: 12px 14px; border-radius: var(--r-btn); background: var(--ref-sand); font-size: 12.5px; line-height: 1.6; color: var(--ref-ink-soft); }
.dlg-foot {
  display: flex; justify-content: flex-end; gap: 10px;
  padding: 16px 24px 24px;
}
.dlg-cancel {
  height: 42px; padding: 0 18px; border-radius: var(--r-btn);
  background: var(--ref-surface); border: 1px solid var(--ref-line);
  color: var(--ref-ink-soft); font-size: 13px; font-weight: 500; cursor: pointer;
  transition: background 0.15s, border-color 0.15s;
}
.dlg-cancel:hover { background: var(--ref-sand); border-color: color-mix(in srgb, var(--ref-ink) 30%, transparent); }

/* ═══ ANIMATIONS ═══ */
@keyframes pr-spin { to { transform: rotate(360deg); } }
@keyframes pr-shimmer { to { background-position: -200% 0; } }
@keyframes pr-pulse { 0%, 100% { opacity: 1; } 50% { opacity: 0.55; } }
@keyframes pr-fade { from { opacity: 0; } to { opacity: 1; } }
@keyframes pr-pop { from { opacity: 0; transform: translateY(10px) scale(0.98); } to { opacity: 1; transform: none; } }

/* ═══ RESPONSIVE ═══ */
@media (max-width: 1100px) {
  .ps-duo { grid-template-columns: 1fr; gap: 0; }
  .rr-grid { grid-template-columns: 1fr; }
  .wp-grid { grid-template-columns: 1fr; }
}
@media (max-width: 900px) {
  .ph-metrics { grid-template-columns: repeat(2, 1fr); }
  .ph-metric:nth-child(3) { border-left: 0; }
  .pets-grid { grid-template-columns: repeat(2, 1fr); }
  .ap-grid { grid-template-columns: 1fr; }
  .ap-row:nth-child(odd) { border-right: 0; }
  .ph-actions { width: 100%; }
}
@media (max-width: 560px) {
  .pr-shell { padding: 0 16px; }
  .ph-top { padding: 20px 20px 24px; }
  .ph-identity { flex-direction: column; gap: 16px; }
  .ph-avatar-btn { width: 88px; height: 88px; }
  .ph-stay { padding: 12px 20px; }
  .ph-metric { padding: 18px 20px; }
  .ph-m-value { font-size: 24px; }
  .pets-grid { grid-template-columns: 1fr; }
  .pet-img { aspect-ratio: 4 / 3; }
  .ps-section { margin-top: 40px; }
  .ps-duo { gap: 0; }
  .ledger-row { padding: 13px 16px; }
}
@media (prefers-reduced-motion: reduce) {
  .ph-skeleton { animation: none; }
  .ps-skeleton { animation: none; }
  .ph-spinner { animation-duration: 1.4s; }
  .pet-img :deep(.media-image), .or-thumb :deep(.media-image) { transition: none; }
}
</style>
