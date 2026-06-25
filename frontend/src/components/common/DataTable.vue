<template>
  <!-- 表格容器 -->
  <div class="table-wrap">
    <table>
      <!-- 表头 -->
      <thead>
        <tr>
          <!-- 动态生成表头列 -->
          <th v-for="col in columns" :key="col.label || col">{{ col.label || col }}</th>
          <!-- 操作列 -->
          <th v-if="$slots.default">操作</th>
        </tr>
      </thead>
      <!-- 表体 -->
      <tbody>
        <!-- 动态生成表格数据行 -->
        <tr v-for="(row, i) in data" :key="i">
          <td v-for="(col, j) in columns" :key="j">
            <!-- 支持 HTML 内容渲染 -->
            <span v-if="row[col.key || col] && typeof row[col.key || col] === 'string' && row[col.key || col].startsWith('<')" v-html="row[col.key || col]"></span>
            <!-- 普通文本内容 -->
            <span v-else>{{ row[col.key || col] }}</span>
          </td>
          <!-- 插槽用于自定义操作内容 -->
          <td v-if="$slots.default"><slot :row="row" /></td>
        </tr>
        <!-- 无数据时显示 -->
        <tr v-if="data.length === 0">
          <td :colspan="columns.length + 1" class="empty">暂无数据</td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script setup>
defineProps({ columns: Array, data: Array })
</script>

<style scoped>
.empty { text-align: center; padding: 32px; color: var(--color-muted-foreground); }
</style>
