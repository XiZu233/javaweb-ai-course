<template>
  <div>
    <el-button type="primary" icon="el-icon-plus" @click="handleAdd">新增部门</el-button>
    <el-table :data="deptList" style="margin-top: 20px;" border>
      <el-table-column type="index" label="序号" width="80" align="center"></el-table-column>
      <el-table-column prop="name" label="部门名称"></el-table-column>
      <el-table-column prop="createTime" label="创建时间"></el-table-column>
      <el-table-column prop="updateTime" label="更新时间"></el-table-column>
      <el-table-column label="操作" width="180" align="center">
        <template slot-scope="scope">
          <el-button type="text" @click="handleEdit(scope.row)">编辑</el-button>
          <el-button type="text" style="color: #f56c6c;" @click="handleDelete(scope.row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 新增/编辑对话框 -->
    <el-dialog :title="dialogTitle" :visible.sync="dialogVisible" width="400px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="部门名称">
          <el-input v-model="form.name"></el-input>
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submit">确定</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
export default {
  name: 'Dept',
  data() {
    return {
      deptList: [],
      dialogVisible: false,
      dialogTitle: '新增部门',
      form: { id: null, name: '' }
    }
  },
  created() {
    this.loadData()
  },
  methods: {
    loadData() {
      this.$request.get('/depts').then(data => {
        this.deptList = data
      })
    },
    handleAdd() {
      this.form = { id: null, name: '' }
      this.dialogTitle = '新增部门'
      this.dialogVisible = true
    },
    handleEdit(row) {
      this.form = { ...row }
      this.dialogTitle = '编辑部门'
      this.dialogVisible = true
    },
    submit() {
      const api = this.form.id ? this.$request.put('/depts', this.form) : this.$request.post('/depts', this.form)
      api.then(() => {
        this.$message.success(this.form.id ? '修改成功' : '新增成功')
        this.dialogVisible = false
        this.loadData()
      })
    },
    handleDelete(id) {
      this.$confirm('确认删除该部门？', '提示', { type: 'warning' }).then(() => {
        this.$request.delete(`/depts/${id}`).then(() => {
          this.$message.success('删除成功')
          this.loadData()
        })
      })
    }
  }
}
</script>
