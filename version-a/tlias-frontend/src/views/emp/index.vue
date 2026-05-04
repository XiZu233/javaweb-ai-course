<template>
  <div>
    <!-- 搜索表单 -->
    <el-form :inline="true" :model="searchForm" style="margin-bottom: 20px;">
      <el-form-item label="姓名">
        <el-input v-model="searchForm.name" placeholder="请输入姓名" clearable></el-input>
      </el-form-item>
      <el-form-item label="性别">
        <el-select v-model="searchForm.gender" placeholder="请选择" clearable>
          <el-option label="男" :value="1"></el-option>
          <el-option label="女" :value="2"></el-option>
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" @click="loadData">查询</el-button>
        <el-button icon="el-icon-refresh" @click="reset">重置</el-button>
      </el-form-item>
    </el-form>

    <el-button type="primary" icon="el-icon-plus" @click="handleAdd">新增员工</el-button>
    <el-button type="danger" icon="el-icon-delete" @click="handleBatchDelete" :disabled="selectedIds.length === 0">批量删除</el-button>

    <el-table :data="empList" style="margin-top: 20px;" border @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center"></el-table-column>
      <el-table-column prop="name" label="姓名"></el-table-column>
      <el-table-column label="性别" width="80">
        <template slot-scope="scope">{{ scope.row.gender === 1 ? '男' : '女' }}</template>
      </el-table-column>
      <el-table-column prop="image" label="头像" width="80">
        <template slot-scope="scope">
          <el-avatar :size="40" :src="scope.row.image || 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'"></el-avatar>
        </template>
      </el-table-column>
      <el-table-column label="职位" width="120">
        <template slot-scope="scope">{{ jobMap[scope.row.job] }}</template>
      </el-table-column>
      <el-table-column prop="entrydate" label="入职日期" width="120"></el-table-column>
      <el-table-column prop="deptName" label="所属部门" width="120"></el-table-column>
      <el-table-column label="操作" width="180" align="center">
        <template slot-scope="scope">
          <el-button type="text" @click="handleEdit(scope.row)">编辑</el-button>
          <el-button type="text" style="color: #f56c6c;" @click="handleDelete([scope.row.id])">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      style="margin-top: 20px; text-align: right;"
      @current-change="loadData"
      :current-page="page"
      :page-size="pageSize"
      :total="total"
      layout="total, prev, pager, next"
    ></el-pagination>
  </div>
</template>

<script>
export default {
  name: 'Emp',
  data() {
    return {
      empList: [],
      page: 1,
      pageSize: 10,
      total: 0,
      searchForm: { name: '', gender: null },
      selectedIds: [],
      jobMap: { 1: '班主任', 2: '讲师', 3: '学工主管', 4: '教研主管', 5: '咨询师', 6: '其他' }
    }
  },
  created() {
    this.loadData()
  },
  methods: {
    loadData() {
      const params = {
        page: this.page,
        pageSize: this.pageSize,
        ...this.searchForm
      }
      this.$request.get('/emps', { params }).then(data => {
        this.empList = data.list
        this.total = data.total
      })
    },
    reset() {
      this.searchForm = { name: '', gender: null }
      this.page = 1
      this.loadData()
    },
    handleSelectionChange(rows) {
      this.selectedIds = rows.map(r => r.id)
    },
    handleAdd() {
      this.$message.info('新增员工功能开发中...')
    },
    handleEdit(row) {
      this.$message.info('编辑员工功能开发中...')
    },
    handleDelete(ids) {
      this.$confirm('确认删除？', '提示', { type: 'warning' }).then(() => {
        this.$request.delete(`/emps/${ids.join(',')}`).then(() => {
          this.$message.success('删除成功')
          this.loadData()
        })
      })
    },
    handleBatchDelete() {
      this.handleDelete(this.selectedIds)
    }
  }
}
</script>
