<template>
  <div class="policies-container">
    <div class="policies-header">
      <h3>政策查询</h3>
      <el-input
        v-model="searchQuery"
        placeholder="搜索政策..."
        class="search-input"
        @keyup.enter="searchPolicies"
      >
        <template #append>
          <el-button @click="searchPolicies">
            <el-icon><Search /></el-icon>
          </el-button>
        </template>
      </el-input>
    </div>
    
    <div class="policies-filters">
      <el-select v-model="categoryFilter" placeholder="选择分类" clearable @change="searchPolicies">
        <el-option label="奖学金" value="SCHOLARSHIP" />
        <el-option label="助学金" value="GRANT" />
        <el-option label="请假" value="LEAVE" />
        <el-option label="考试" value="EXAM" />
        <el-option label="宿舍" value="DORMITORY" />
      </el-select>
    </div>
    
    <div class="policies-list">
      <el-empty v-if="policies.length === 0" description="暂无政策数据" />
      
      <el-card v-for="policy in policies" :key="policy.id" class="policy-card">
        <template #header>
          <div class="policy-header">
            <span class="policy-name">{{ policy.name }}</span>
            <el-tag size="small" type="info">{{ policy.version }}</el-tag>
          </div>
        </template>
        
        <div class="policy-content">
          <p class="policy-summary">{{ policy.summary }}</p>
          <div class="policy-meta">
            <span>分类: {{ policy.category }}</span>
            <span>来源: {{ policy.source }}</span>
            <span>生效日期: {{ policy.effectiveDate }}</span>
          </div>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import api from '../api/auth'

interface Policy {
  id: number
  name: string
  summary: string
  category: string
  source: string
  version: string
  effectiveDate: string
}

const policies = ref<Policy[]>([])
const searchQuery = ref('')
const categoryFilter = ref('')

onMounted(() => {
  searchPolicies()
})

async function searchPolicies() {
  try {
    const params: any = {}
    if (searchQuery.value) params.query = searchQuery.value
    if (categoryFilter.value) params.category = categoryFilter.value
    
    const response = await api.get('/policies', { params })
    policies.value = response.data
  } catch (error) {
    ElMessage.error('搜索政策失败')
  }
}
</script>

<style scoped>
.policies-container {
  padding: 20px;
}

.policies-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.policies-header h3 {
  margin: 0;
}

.search-input {
  width: 400px;
}

.policies-filters {
  margin-bottom: 20px;
}

.policies-list {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.policy-card {
  cursor: pointer;
}

.policy-card:hover {
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.policy-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.policy-name {
  font-weight: bold;
  font-size: 16px;
}

.policy-content {
  color: #666;
}

.policy-summary {
  margin: 0 0 10px;
  line-height: 1.6;
}

.policy-meta {
  display: flex;
  gap: 20px;
  font-size: 14px;
  color: #999;
}
</style>