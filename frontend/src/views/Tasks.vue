<template>
  <div class="tasks-container">
    <div class="tasks-header">
      <h3>我的任务</h3>
      <el-button type="primary" @click="showCreateDialog = true">
        <el-icon><Plus /></el-icon>
        新建任务
      </el-button>
    </div>
    
    <div class="tasks-filters">
      <el-radio-group v-model="statusFilter">
        <el-radio-button label="">全部</el-radio-button>
        <el-radio-button label="PENDING">待处理</el-radio-button>
        <el-radio-button label="IN_PROGRESS">进行中</el-radio-button>
        <el-radio-button label="COMPLETED">已完成</el-radio-button>
      </el-radio-group>
    </div>
    
    <div class="tasks-list">
      <el-table :data="filteredTasks" style="width: 100%">
        <el-table-column prop="title" label="任务名称" min-width="200" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="priority" label="优先级" width="100">
          <template #default="{ row }">
            <el-tag :type="getPriorityType(row.priority)">
              {{ row.priority }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="deadline" label="截止日期" width="120" />
        <el-table-column label="操作" width="200">
          <template #default="{ row }">
            <el-button 
              v-if="row.status !== 'COMPLETED'"
              type="primary" 
              size="small"
              @click="handleComplete(row.id)"
            >
              完成
            </el-button>
            <el-button 
              v-if="row.status !== 'COMPLETED'"
              type="danger" 
              size="small"
              @click="handleCancel(row.id)"
            >
              取消
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
    
    <el-dialog v-model="showCreateDialog" title="新建任务" width="500px">
      <el-form :model="newTask" :rules="rules" ref="formRef">
        <el-form-item label="任务名称" prop="title">
          <el-input v-model="newTask.title" placeholder="请输入任务名称" />
        </el-form-item>
        <el-form-item label="任务描述">
          <el-input v-model="newTask.description" type="textarea" placeholder="请输入任务描述" />
        </el-form-item>
        <el-form-item label="截止日期">
          <el-date-picker v-model="newTask.deadline" type="date" placeholder="选择日期" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" @click="handleCreate" :loading="creating">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { getTasks, createTask, completeTask, cancelTask, type Task } from '../api/task'
import { ElMessage, ElMessageBox } from 'element-plus'

const tasks = ref<Task[]>([])
const statusFilter = ref('')
const showCreateDialog = ref(false)
const creating = ref(false)
const formRef = ref()
const newTask = ref({
  title: '',
  description: '',
  deadline: ''
})

const rules = {
  title: [{ required: true, message: '请输入任务名称', trigger: 'blur' }]
}

const filteredTasks = computed(() => {
  if (!statusFilter.value) return tasks.value
  return tasks.value.filter(task => task.status === statusFilter.value)
})

onMounted(() => {
  loadTasks()
})

async function loadTasks() {
  try {
    const response = await getTasks()
    tasks.value = response.data
  } catch (error) {
    ElMessage.error('加载任务失败')
  }
}

async function handleCreate() {
  if (!formRef.value) return
  
  await formRef.value.validate(async (valid: boolean) => {
    if (valid) {
      creating.value = true
      try {
        await createTask({
          title: newTask.value.title,
          description: newTask.value.description,
          deadline: newTask.value.deadline
        })
        ElMessage.success('任务创建成功')
        showCreateDialog.value = false
        newTask.value = { title: '', description: '', deadline: '' }
        await loadTasks()
      } catch (error) {
        ElMessage.error('创建任务失败')
      } finally {
        creating.value = false
      }
    }
  })
}

async function handleComplete(id: number) {
  try {
    await ElMessageBox.confirm('确定要标记此任务为已完成吗？', '确认', {
      type: 'info'
    })
    await completeTask(id)
    ElMessage.success('任务已完成')
    await loadTasks()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('操作失败')
    }
  }
}

async function handleCancel(id: number) {
  try {
    await ElMessageBox.confirm('确定要取消此任务吗？', '确认', {
      type: 'warning'
    })
    await cancelTask(id)
    ElMessage.success('任务已取消')
    await loadTasks()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('操作失败')
    }
  }
}

function getStatusType(status: string) {
  switch (status) {
    case 'COMPLETED': return 'success'
    case 'IN_PROGRESS': return 'warning'
    default: return 'info'
  }
}

function getStatusText(status: string) {
  switch (status) {
    case 'COMPLETED': return '已完成'
    case 'IN_PROGRESS': return '进行中'
    case 'CANCELLED': return '已取消'
    default: return '待处理'
  }
}

function getPriorityType(priority: string) {
  switch (priority) {
    case 'HIGH': return 'danger'
    case 'MEDIUM': return 'warning'
    default: return 'info'
  }
}
</script>

<style scoped>
.tasks-container {
  padding: 20px;
}

.tasks-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.tasks-header h3 {
  margin: 0;
}

.tasks-filters {
  margin-bottom: 20px;
}

.tasks-list {
  background: white;
  border-radius: 8px;
  padding: 20px;
}
</style>