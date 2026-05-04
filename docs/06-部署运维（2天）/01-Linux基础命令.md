# Linux 基础命令

## 学习目标

- 掌握 Linux 常用文件操作和系统管理命令
- 能够使用 SSH 远程连接服务器
- 了解 Linux 目录结构和权限管理

## 核心知识点

### 1. 目录结构

```
/              # 根目录
├── bin        # 常用命令
├── etc        # 配置文件
├── home       # 用户主目录
├── var        # 日志文件
├── usr        # 应用程序
├── tmp        # 临时文件
└── opt        # 可选软件包
```

### 2. 常用命令

**文件操作**：

```bash
# 查看当前目录
pwd

# 列出文件
ls -la

# 切换目录
cd /home
cd ..       # 上级目录
cd ~        # 用户主目录

# 创建目录
mkdir myproject
mkdir -p a/b/c   # 递归创建

# 创建文件
touch hello.txt

# 复制/移动/删除
cp file1.txt file2.txt
mv file1.txt /tmp/
rm file.txt
rm -rf directory/   # 强制删除目录（慎用）

# 查看文件内容
cat file.txt        # 全部显示
head -20 file.txt   # 前20行
tail -f log.txt     # 实时追踪日志
```

**系统管理**：

```bash
# 查看进程
ps -ef | grep java

# 查看系统资源
top
free -h     # 内存
df -h       # 磁盘

# 查看端口占用
netstat -tlnp | grep 8080

# 压缩/解压
tar -czvf backup.tar.gz /path
tar -xzvf backup.tar.gz
```

**权限管理**：

```bash
# 修改权限
chmod 755 script.sh

# 修改所有者
chown user:group file.txt

# 权限数字含义
# 7 = rwx (读写执行)
# 6 = rw- (读写)
# 5 = r-x (读执行)
# 4 = r-- (只读)
```

### 3. SSH 远程连接

```bash
# 连接服务器
ssh root@192.168.1.100

# 使用密钥连接
ssh -i ~/.ssh/id_rsa root@192.168.1.100

# 传输文件
scp localfile.txt root@192.168.1.100:/tmp/
scp -r root@192.168.1.100:/opt/app ./backup/
```

### 4. Vim 基础

```bash
vim file.txt

# 模式切换
i      # 进入插入模式
Esc    # 返回命令模式
:wq    # 保存并退出
:q!    # 强制退出不保存

# 常用命令
dd     # 删除一行
yy     # 复制一行
p      # 粘贴
/xxx   # 搜索 xxx
```

## 动手练习

### 练习 1：SSH 连接服务器

1. 使用 SSH 连接云服务器
2. 在 /opt 目录下创建项目文件夹
3. 上传本地打包好的 jar 包到服务器

### 练习 2：日志排查

1. 使用 `tail -f` 实时查看应用日志
2. 使用 `grep` 搜索日志中的错误关键字
3. 使用 `ps` 查看 Java 进程是否运行

## 常见错误排查

| 错误现象 | 原因 | 解决方案 |
|---------|------|---------|
| Permission denied | 权限不足 | 使用 sudo 或修改文件权限 |
| Command not found | 命令未安装或不在 PATH | 安装软件包或检查环境变量 |
| Connection refused | 端口未开放或服务未启动 | 检查防火墙和服务状态 |

## 本节小结

Linux 是服务器部署的必备技能。掌握常用命令、文件权限和 SSH 操作，你就能独立完成服务器的日常管理和故障排查。

## 参考文档

- [Linux 命令大全](https://man7.org/linux/man-pages/)

