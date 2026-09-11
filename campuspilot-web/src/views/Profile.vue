<template>
  <div class="profile-container">
    <el-card class="profile-card">
      <template #header>
        <div class="card-header">
          <span>个人信息</span>
        </div>
      </template>
      
      <el-descriptions :column="2" border>
        <el-descriptions-item label="姓名">{{ studentInfo.realName }}</el-descriptions-item>
        <el-descriptions-item label="学号">{{ studentInfo.studentNo }}</el-descriptions-item>
        <el-descriptions-item label="年级">{{ studentInfo.grade }}</el-descriptions-item>
        <el-descriptions-item label="专业">{{ studentInfo.major }}</el-descriptions-item>
        <el-descriptions-item label="班级">{{ studentInfo.className }}</el-descriptions-item>
        <el-descriptions-item label="绩点">{{ studentInfo.gpa }}</el-descriptions-item>
        <el-descriptions-item label="排名">前{{ studentInfo.rankingPercent }}%</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="studentInfo.status === 'ACTIVE' ? 'success' : 'danger'">
            {{ studentInfo.status === 'ACTIVE' ? '在读' : '休学' }}
          </el-tag>
        </el-descriptions-item>
      </el-descriptions>
    </el-card>
    
    <el-card class="stats-card">
      <template #header>
        <div class="card-header">
          <span>数据统计</span>
        </div>
      </template>
      
      <el-row :gutter="20">
        <el-col :span="6">
          <div class="stat-item">
            <div class="stat-value">{{ stats.completedTasks }}</div>
            <div class="stat-label">已完成任务</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-item">
            <div class="stat-value">{{ stats.pendingTasks }}</div>
            <div class="stat-label">待处理任务</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-item">
            <div class="stat-value">{{ stats.totalChats }}</div>
            <div class="stat-label">对话次数</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-item">
            <div class="stat-value">{{ stats.eligiblePolicies }}</div>
            <div class="stat-label">符合政策数</div>
          </div>
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getCurrentUser } from '../api/auth'
import { getTasks } from '../api/task'
import { ElMessage } from 'element-plus'

const studentInfo = ref({
  realName: '',
  studentNo: '',
  grade: '',
  major: '',
  className: '',
  gpa: 0,
  rankingPercent: 0,
  status: 'ACTIVE'
})

const stats = ref({
  completedTasks: 0,
  pendingTasks: 0,
  totalChats: 0,
  eligiblePolicies: 0
})

onMounted(async () => {
  try {
    const response = await getCurrentUser()
    studentInfo.value = response.data
    
    // 加载任务统计
    const tasksResponse = await getTasks()
    const tasks = tasksResponse.data
    stats.value.completedTasks = tasks.filter((t: any) => t.status === 'COMPLETED').length
    stats.value.pendingTasks = tasks.filter((t: any) => t.status !== 'COMPLETED').length
  } catch (error) {
    ElMessage.error('加载个人信息失败')
  }
})
</script>

<style scoped>
.profile-container {
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.profile-card, .stats-card {
  width: 100%;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: bold;
}

.stat-item {
  text-align: center;
  padding: 20px;
}

.stat-value {
  font-size: 32px;
  font-weight: bold;
  color: #409eff;
}

.stat-label {
  margin-top: 10px;
  color: #666;
}
</style>