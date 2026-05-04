# Linux 基础命令

---

## 学习目标

- 理解为什么服务器普遍使用 Linux，建立操作系统层面的全局认知
- 掌握文件与目录的增删改查操作，能够自如地在 Linux 文件系统中导航
- 理解 Linux 权限模型（rwx），能够使用 chmod、chown 管理文件访问权限
- 掌握进程查看与管理系统资源的基本命令，能够排查服务运行状态
- 掌握网络诊断命令（ping、curl、netstat），能够定位网络连通性问题
- 掌握文本处理三剑客（grep、find、sed）的入门用法，能够进行日志搜索与简单过滤
- 掌握 Vim 的基础操作，能够在服务器上直接编辑配置文件
- 掌握压缩打包命令 tar/zip，能够备份和传输项目文件

---

## 核心知识点

### 1. 为什么必须学 Linux？

#### 1.1 是什么

Linux 是一种开源的类 Unix 操作系统内核，由 Linus Torvalds 于 1991 年发布。基于 Linux 内核发展出了众多发行版（Distribution），如 Ubuntu、CentOS、Debian、AlmaLinux 等。

#### 1.2 为什么需要它

想象你开发了一个 Web 应用，在自己的 Windows 笔记本上运行得好好的。但当你要把它部署到互联网上让用户访问时，你面临一个选择：

- **Windows 服务器**：需要购买授权，图形界面占用大量资源，远程桌面操作卡顿
- **Linux 服务器**：免费开源，命令行操作轻量高效，全球 90% 以上的云服务器都运行 Linux

**真实场景类比**：

> 你开了一家餐厅。Windows 服务器就像请了一个全能但昂贵的经理，每天需要豪华办公室（图形界面）。Linux 服务器就像一个专业、高效、不要工资的厨师长，只需要一个灶台（命令行）就能运转整个厨房。作为程序员，你要学会和这位厨师长打交道。

#### 1.3 Linux 目录结构速览

Linux 的一切文件都组织在一棵倒置的目录树中，根目录是 `/`。

```
/                          <-- 根目录，一切文件的起点
|
├── bin/                   <-- Binary，存放最常用的命令（如 ls, cat, pwd）
├── sbin/                  <-- System Binary，系统管理员命令（如 fdisk, reboot）
├── etc/                   <-- Etcetera，配置文件大本营
│   ├── nginx/nginx.conf   <-- Nginx 配置
│   ├── mysql/my.cnf       <-- MySQL 配置
│   └── hosts              <-- 本地 DNS 映射
├── home/                  <-- 普通用户的家目录
│   ├── alice/             <-- 用户 alice 的个人文件
│   └── bob/
├── root/                  <-- root 超级管理员的家目录
├── var/                   <-- Variable，经常变化的文件
│   ├── log/               <-- 系统日志、应用日志
│   └── www/               <-- Web 站点文件
├── usr/                   <-- Unix System Resources，应用程序安装目录
│   ├── bin/               <-- 用户安装的软件命令
│   ├── local/             <-- 手动编译安装的软件
│   └── share/             <-- 共享数据
├── tmp/                   <-- Temporary，临时文件，重启后可能清空
├── opt/                   <-- Optional，可选的第三方软件包
└── proc/                  <-- Process，虚拟文件系统，存放进程信息
```

**记忆口诀**：

```
/bin 命令忙，/etc 配四方
/home 用户住，/var 日志长
/usr 装软件，/tmp 临时放
/opt 第三方，/proc 看进程
```

---

### 2. 文件操作命令

#### 2.1 导航命令：pwd、cd、ls

**是什么**：这些是最基础的"我在哪、去哪、看啥"命令。

**为什么需要它**：在 Linux 中，所有操作都基于当前所在的位置。不知道自己在哪，就像在没有地图的城市里乱走。

**绝对路径 vs 相对路径**：

| 类型 | 说明 | 示例 |
|------|------|------|
| 绝对路径 | 从根目录 `/` 开始的完整路径 | `/home/alice/projects/myapp` |
| 相对路径 | 相对于当前目录的路径 | `./src/main.java`（当前目录下）、`../config.yml`（上级目录） |

```bash
# pwd = Print Working Directory，打印当前所在目录
pwd
# 输出示例：/home/student/myproject

# cd = Change Directory，切换目录
cd /var/log          # 切换到 /var/log 目录（绝对路径）
cd ~                 # 切换到当前用户的家目录，~ 是家目录的简写
cd ..                # 切换到上级目录，.. 表示上级
cd ../..             # 向上跳两级
cd -                 # 回到刚才所在的目录（快速来回切换）
cd ./nginx           # 进入当前目录下的 nginx 子目录，./ 表示当前目录

# ls = List，列出目录内容
ls                   # 简单列出当前目录的文件和文件夹
ls -l                # 长格式显示，包含权限、大小、修改时间等详细信息
ls -la               # 显示所有文件（包括以 . 开头的隐藏文件）
ls -lh               # 人类可读格式显示文件大小（K、M、G 而不是纯字节）
ls -ltr              # 按修改时间倒序排列，最新的在最后
ls /etc/nginx        # 列出指定目录的内容，不用先 cd 进去
```

#### 2.2 创建与删除：mkdir、touch、rm

```bash
# mkdir = Make Directory，创建目录
mkdir myproject              # 在当前目录创建名为 myproject 的文件夹
mkdir -p a/b/c/d             # -p = parents，递归创建多级目录（如果父目录不存在会自动创建）
mkdir -v dir1 dir2 dir3      # -v = verbose，显示创建过程的详细信息

# touch = 触摸，创建空文件或更新文件时间戳
touch hello.txt              # 创建一个空文件 hello.txt
touch app.log                # 如果 app.log 已存在，则更新其修改时间为现在

# rm = Remove，删除文件或目录（⚠️ 危险命令，删除后无法恢复）
rm file.txt                  # 删除单个文件
rm file1.txt file2.txt       # 删除多个文件
rm -i file.txt               # -i = interactive，删除前询问确认（安全习惯）
rm -r myfolder/              # -r = recursive，递归删除目录及其内部所有内容
rm -rf myfolder/             # -f = force，强制删除不询问（极度危险，新手慎用）
# ⚠️ 警告：rm -rf / 会删除整个系统，永远不要执行！
```

#### 2.3 复制与移动：cp、mv

```bash
# cp = Copy，复制文件或目录
cp source.txt dest.txt       # 复制文件，如果 dest.txt 存在则覆盖
cp -i source.txt dest.txt    # -i = interactive，覆盖前询问
cp -r mydir/ backup/         # -r = recursive，复制整个目录
cp -v *.log /tmp/            # -v = verbose，显示复制过程；* 是通配符，匹配所有 .log 文件

# mv = Move，移动或重命名文件/目录
mv oldname.txt newname.txt   # 重命名文件
mv file.txt /tmp/            # 移动文件到 /tmp 目录
mv -i file.txt /tmp/         # 如果目标位置已存在同名文件，询问是否覆盖
mv mydir/ /backup/           # 移动整个目录
```

#### 2.4 查看文件内容：cat、head、tail、more、less

```bash
# cat = Concatenate，连接并显示文件全部内容（适合小文件）
cat config.ini               # 显示 config.ini 的全部内容
cat -n app.log               # -n = number，显示行号

# head = 显示文件头部（默认前 10 行）
head app.log                 # 显示前 10 行
head -n 5 app.log            # -n 5 = 显示前 5 行
head -20 app.log             # 简写形式，显示前 20 行

# tail = 显示文件尾部（默认后 10 行），排查日志的神器
tail app.log                 # 显示最后 10 行
tail -f app.log              # -f = follow，实时追踪文件新增内容（监控日志必备）
tail -n 50 -f app.log        # 显示最后 50 行并持续追踪
# 按 Ctrl + C 退出追踪模式

# more / less = 分页查看大文件（less 更强大，支持上下翻页和搜索）
less /var/log/syslog         # 打开大文件，按空格翻页，按 q 退出
# 在 less 中：
#   /keyword  向下搜索 keyword
#   ?keyword  向上搜索 keyword
#   n         跳到下一个匹配
#   N         跳到上一个匹配
#   q         退出
```

---

### 3. 权限管理

#### 3.1 是什么

Linux 是一个多用户系统，权限管理决定了"谁能对文件做什么"。

#### 3.2 为什么需要它

**真实场景类比**：

> 想象你住在一栋公寓里。每个住户（用户）有自己的房间（家目录），但公寓还有公共区域如走廊、电梯。权限管理就是门禁系统：
> - 你可以自由进出自己的房间（对自己的文件有全部权限）
> - 你可以进入走廊但不能修改（对系统文件只读）
> - 管理员可以进入任何房间维修（root 用户无所不能）

#### 3.3 rwx 权限模型

每个文件/目录有三组权限，每组由 r、w、x 组成：

```
权限字符串：-rwxr-xr--
          │└┬┘└┬┘└┬┘
          │  │  │  │
          │  │  │  └── 其他用户（Others）权限：r-- = 只读
          │  │  └───── 所属组（Group）权限：r-x = 读+执行
          │  └──────── 所有者（Owner）权限：rwx = 读+写+执行
          └─────────── 文件类型（- 普通文件，d 目录，l 链接）
```

| 权限字符 | 对文件的作用 | 对目录的作用 |
|----------|-------------|-------------|
| r (read, 4) | 读取文件内容 | 列出目录中的文件列表（ls） |
| w (write, 2) | 修改文件内容 | 在目录中创建/删除文件 |
| x (execute, 1) | 执行文件（如脚本） | 进入目录（cd） |

**数字表示法**：

| 数字 | 权限 | 说明 |
|------|------|------|
| 7 | rwx | 读 + 写 + 执行 |
| 6 | rw- | 读 + 写 |
| 5 | r-x | 读 + 执行 |
| 4 | r-- | 只读 |
| 0 | --- | 无权限 |

```bash
# 查看文件权限
ls -l script.sh
# 输出：-rwxr-xr-x 1 root root 1234 May 1 10:00 script.sh
#      └┬┘└┬┘└┬┘
#       7  5  5  → 权限数字是 755

# chmod = Change Mode，修改权限
chmod 755 script.sh          # 所有者：rwx，组：r-x，其他：r-x
chmod 644 config.txt         # 所有者：rw-，组：r--，其他：r--（常见配置文件权限）
chmod 600 secret.key         # 只有所有者可读写（敏感文件如私钥）
chmod +x deploy.sh           # 给所有用户添加执行权限（常用）
chmod -x deploy.sh           # 移除执行权限
chmod u+w file.txt           # u = user（所有者），给所有者添加写权限
chmod g-r file.txt           # g = group，移除组的读权限
chmod o=rx dir/              # o = other，设置其他用户权限为 r-x

# chown = Change Owner，修改所有者和所属组
chown alice:developers file.txt    # 将文件所有者改为 alice，所属组改为 developers
chown -R alice:alice mydir/        # -R = recursive，递归修改目录及内部所有文件

# 实际场景：部署 Java 应用
# 1. 上传 jar 包后，确保它有执行权限
chmod +x app.jar
# 2. 确保配置文件只有应用用户能读
chmod 600 application-prod.yml
# 3. 确保日志目录应用用户可写
chown -R appuser:appuser /var/log/myapp/
```

---

### 4. 进程管理

#### 4.1 是什么

进程（Process）是正在运行的程序的实例。你的 Java 应用、Nginx、MySQL 在 Linux 中都是以进程的形式运行的。

#### 4.2 为什么需要它

**真实场景类比**：

> 进程就像厨房里的厨师。ps 命令是查看"现在有哪些厨师在干活"，top 命令是实时监控"每个厨师忙不忙、用了多少煤气"，kill 命令是"让某个厨师下班"。

```bash
# ps = Process Status，查看进程快照
ps                           # 显示当前终端的进程
ps -ef                       # -e = 所有进程，-f = 全格式显示
# 输出列说明：
# UID        PID  PPID  C STIME TTY          TIME CMD
# root         1     0  0 09:00 ?        00:00:01 /sbin/init
# UID: 用户ID  PID: 进程ID  PPID: 父进程ID  CMD: 启动命令

ps -ef | grep java           # 管道 + grep，筛选出 Java 相关进程
# | 是管道符号，将前一个命令的输出作为后一个命令的输入
# grep 是文本过滤工具，只显示包含 "java" 的行

# top = 实时进程监控（按 CPU 占用排序）
top
# 界面说明：
# 第一行：系统运行时间、用户数、负载平均值
# 第二行：进程总数、运行中、睡眠中、停止、僵尸进程
# 第三行：CPU 使用率（us 用户态、sy 系统态、id 空闲）
# 第四行：内存使用情况
# 操作：
#   q     退出
#   k     输入 PID 后回车，发送信号终止进程
#   M     按内存占用排序
#   P     按 CPU 占用排序（默认）

# free = 查看内存使用情况
free -h                      # -h = human-readable，以 K/M/G 显示
# 输出：
#               total        used        free      shared  buff/cache   available
# Mem:           7.7G        2.1G        1.2G        256M        4.4G        5.0G
# Swap:          2.0G          0B        2.0G

# df = Disk Free，查看磁盘空间
df -h                        # -h = human-readable
# 输出：
# Filesystem      Size  Used Avail Use% Mounted on
# /dev/sda1        50G   20G   28G  42% /
# /dev/sdb1       100G   10G   90G  10% /data

# kill = 终止进程
kill 1234                    # 向 PID 为 1234 的进程发送默认终止信号（SIGTERM，优雅退出）
kill -9 1234                 # -9 = SIGKILL，强制终止（进程无法忽略，慎用）
killall java                 # 终止所有名为 java 的进程
pkill -f "myapp.jar"         # 按完整命令行匹配终止进程
```

---

### 5. 网络命令

#### 5.1 是什么

网络命令用于诊断网络连通性、查看端口占用、下载资源等。

#### 5.2 为什么需要它

**真实场景类比**：

> 你的应用部署在服务器上，用户说"访问不了"。网络命令就是你的诊断工具箱：
> - ping 检查"路通不通"
> - curl 检查"服务有没有正确响应"
> - netstat 检查"端口有没有被占用"

```bash
# ping = 测试网络连通性
ping www.baidu.com           # 向百度发送 ICMP 包，测试外网连通性
ping 192.168.1.100           # 测试内网某台机器是否可达
# 按 Ctrl + C 停止，查看统计结果：
# 3 packets transmitted, 3 received, 0% packet loss  ← 0% 丢包表示网络正常

# curl = 发送 HTTP 请求，测试接口
curl http://localhost:8080/api/health    # GET 请求健康检查接口
curl -I http://localhost:8080            # -I = 只显示响应头（Headers）
curl -X POST http://localhost:8080/api/login \
     -H "Content-Type: application/json" \
     -d '{"username":"admin","password":"123456"}'
# -X 指定请求方法，-H 添加请求头，-d 发送请求体

# netstat = Network Statistics，查看网络连接和端口
netstat -tlnp                # -t = TCP, -l = 监听中, -n = 数字显示, -p = 显示进程
# 输出示例：
# Proto Recv-Q Send-Q Local Address    Foreign Address  State   PID/Program name
# tcp        0      0 0.0.0.0:22       0.0.0.0:*        LISTEN  1234/sshd
# tcp        0      0 0.0.0.0:8080     0.0.0.0:*        LISTEN  5678/java
# 0.0.0.0:8080 表示所有网卡都在监听 8080 端口，5678/java 表示 PID 5678 的 Java 进程

netstat -tlnp | grep 8080    # 筛选 8080 端口的占用情况

# ss = Socket Statistics，netstat 的现代替代品，速度更快
ss -tlnp | grep 3306         # 查看 MySQL 3306 端口是否在监听

# wget = 下载文件
wget https://example.com/file.zip        # 下载文件到当前目录
wget -O myfile.zip https://example.com/file.zip   # -O 指定保存文件名
```

---

### 6. 压缩与打包

#### 6.1 是什么

压缩是将多个文件或目录打包成一个文件，并减小体积，便于传输和备份。

#### 6.2 为什么需要它

**真实场景类比**：

> 你要搬家，把散落一屋的东西（项目文件）装进箱子（打包），再用真空压缩袋压缩（压缩），这样一辆车就能拉走，到了新家再拆开即可。

```bash
# tar = Tape Archive，打包工具（本身不压缩，配合 gzip 实现压缩）

# 打包并压缩（最常用）
tar -czvf backup.tar.gz /home/student/myproject/
# -c = create 创建新包
# -z = gzip 压缩
# -v = verbose 显示过程
# -f = file 指定文件名
# 含义：将 myproject 目录打包并 gzip 压缩为 backup.tar.gz

# 解压
tar -xzvf backup.tar.gz
# -x = extract 解压
# -z = gzip 解压
# -v = verbose
# -f = file

# 解压到指定目录
tar -xzvf backup.tar.gz -C /opt/restore/
# -C = 切换到指定目录后解压

# 只打包不压缩（速度快，体积大）
tar -cvf backup.tar myproject/

# 查看压缩包内容（不解压）
tar -tzvf backup.tar.gz
# -t = list 列出内容

# zip / unzip = 另一种压缩格式（Windows 常用）
zip -r backup.zip myproject/          # -r = recursive，递归压缩目录
unzip backup.zip                      # 解压到当前目录
unzip backup.zip -d /opt/restore/     # -d = 指定解压目录
```

---

### 7. 文本处理三剑客（入门）

#### 7.1 grep = 全局正则搜索

```bash
# grep = Global Regular Expression Print，在文本中搜索匹配行

grep "ERROR" app.log                  # 在 app.log 中搜索包含 ERROR 的行
grep -i "error" app.log               # -i = ignore case，忽略大小写
grep -n "Exception" app.log           # -n = 显示行号
grep -v "INFO" app.log                # -v = invert，反向匹配，显示不包含 INFO 的行
grep -C 3 "NullPointerException" app.log   # -C 3 = Context，显示匹配行及上下各 3 行

# 组合使用：查找 ERROR 但不包含 "connection"
grep "ERROR" app.log | grep -v "connection"

# 递归搜索目录中所有文件
grep -r "TODO" /home/student/project/
```

#### 7.2 find = 文件查找

```bash
# find = 在目录树中查找文件

find /home/student -name "*.java"          # 查找所有 .java 文件
find /var/log -name "*.log" -mtime +7      # 查找 7 天前修改过的日志文件
find . -type f -size +10M                  # 查找当前目录下大于 10MB 的文件
# -type f = 普通文件，-type d = 目录

# 找到后执行操作：删除 30 天前的日志
find /var/log -name "*.log" -mtime +30 -delete

# 找到后执行命令
find . -name "*.class" -exec rm {} \;     # 删除所有 .class 文件
# {} 代表找到的文件，\; 表示 -exec 命令结束
```

#### 7.3 sed = 流编辑器（入门）

```bash
# sed = Stream Editor，对文本进行过滤和转换

sed 's/old/new/' file.txt             # 将每行第一个 old 替换为 new
sed 's/old/new/g' file.txt            # g = global，全局替换
sed -i 's/localhost/127.0.0.1/g' config.yml   # -i = in-place，直接修改文件

# 删除空行
sed '/^$/d' file.txt

# 打印第 5 到 10 行
sed -n '5,10p' file.txt
```

---

### 8. Vim 基础操作

#### 8.1 是什么

Vim 是 Linux 上最经典的文本编辑器，所有服务器都预装。没有图形界面时，它是你唯一编辑配置文件的工具。

#### 8.2 为什么需要它

**真实场景类比**：

> 你远程登录到一台服务器，需要修改 Nginx 的配置文件。服务器上没有 Word、没有 VS Code，只有 Vim。学会 Vim 就像学会在荒野中生火——基础但救命。

#### 8.3 Vim 三种模式

```
+-----------+       i,a,o,I,A,O       +-----------+
|  命令模式  |  -------------------->  |  插入模式  |
| (Normal)  |                         | (Insert)  |
|           |  <--------------------  |           |
+-----------+        按 Esc            +-----------+
     |
     | 按 : 或 / 或 ?
     v
+-----------+
|  底行模式  |
| (Command) |
+-----------+
```

```bash
vim nginx.conf               # 打开文件（如果不存在则创建）
```

**命令模式常用操作**：

| 按键 | 作用 |
|------|------|
| `i` | 在光标前插入（进入插入模式） |
| `a` | 在光标后插入 |
| `o` | 在当前行下方新开一行插入 |
| `Esc` | 返回命令模式 |
| `dd` | 删除当前行 |
| `yy` | 复制当前行 |
| `p` | 在光标后粘贴 |
| `u` | 撤销 |
| `Ctrl + r` | 重做 |
| `x` | 删除光标处字符 |
| `G` | 跳到文件末尾 |
| `gg` | 跳到文件开头 |
| `:行号` | 跳到指定行，如 `:20` |
| `/keyword` | 向下搜索 keyword |
| `n` | 跳到下一个匹配 |
| `N` | 跳到上一个匹配 |

**底行模式常用命令**：

| 命令 | 作用 |
|------|------|
| `:w` | 保存（write） |
| `:q` | 退出（quit） |
| `:wq` | 保存并退出 |
| `:q!` | 强制退出不保存 |
| `:set nu` | 显示行号 |
| `:%s/old/new/g` | 全局替换 |

**Vim 新手急救指南**：

```
1. 打开文件：vim filename
2. 按 i 进入插入模式，开始编辑
3. 编辑完成后，按 Esc 回到命令模式
4. 输入 :wq 回车，保存退出
5. 如果改错了不想保存，输入 :q! 回车，强制退出
```

---

### 9. SSH 远程连接与文件传输

```bash
# ssh = Secure Shell，安全远程登录
ssh root@192.168.1.100       # 以 root 用户登录到 192.168.1.100
ssh -p 2222 user@server.com  # -p 指定端口（默认 22）

# 使用密钥登录（更安全，无需密码）
ssh -i ~/.ssh/id_rsa root@server.com
# -i = identity file，指定私钥文件

# scp = Secure Copy，安全复制文件
scp localfile.txt root@192.168.1.100:/tmp/           # 上传文件到服务器
scp -r ./myproject root@192.168.1.100:/opt/          # -r = recursive，上传目录
scp root@192.168.1.100:/var/log/app.log ./logs/      # 从服务器下载文件
scp -P 2222 file.txt user@host:/path/                # -P（大写）指定 SSH 端口
```

---

## 动手练习

### 练习 1：文件系统探索之旅

1. 使用 `pwd` 查看当前位置，使用 `cd /var/log` 进入日志目录
2. 使用 `ls -la` 查看该目录下所有文件（包括隐藏文件）
3. 使用 `tail -f syslog` 实时监控系统日志，开另一个终端观察变化
4. 按 `Ctrl + C` 退出，回到上级目录 `cd ..`
5. 在家目录创建练习文件夹：`cd ~ && mkdir linux-practice && cd linux-practice`
6. 创建几个测试文件：`touch file1.txt file2.txt file3.txt`
7. 复制 file1.txt 为 backup.txt：`cp file1.txt backup.txt`
8. 删除 file3.txt：`rm file3.txt`

### 练习 2：权限管理实战

1. 创建脚本文件：`echo '#!/bin/bash' > deploy.sh && echo 'echo "Deploying..."' >> deploy.sh`
2. 查看当前权限：`ls -l deploy.sh`
3. 添加执行权限：`chmod +x deploy.sh`
4. 再次查看权限，对比变化
5. 运行脚本：`./deploy.sh`
6. 修改权限为只有所有者可读写：`chmod 600 deploy.sh`
7. 尝试再次运行，观察权限不足的错误
8. 恢复执行权限：`chmod 700 deploy.sh`

### 练习 3：日志排查实战

1. 假设有一个应用日志文件 `/var/log/myapp/app.log`
2. 查看最后 50 行：`tail -n 50 /var/log/myapp/app.log`
3. 实时追踪日志：`tail -f /var/log/myapp/app.log`
4. 在另一个终端，搜索所有 ERROR：`grep -n "ERROR" /var/log/myapp/app.log`
5. 搜索包含 "NullPointerException" 的上下文：`grep -C 5 "NullPointerException" /var/log/myapp/app.log`
6. 统计 ERROR 出现次数：`grep -c "ERROR" /var/log/myapp/app.log`
7. 查找所有今天修改过的日志文件：`find /var/log/myapp -name "*.log" -mtime 0`

---

## 常见错误排查

### 安装环境问题

| 阶段 | 错误现象 | 原因 | 解决方案 |
|------|----------|------|----------|
| 安装环境问题 | `bash: command not found` | 命令未安装或不在 PATH 环境变量中 | 使用 `which 命令名` 检查；若未安装，用包管理器安装（Ubuntu: `apt install`，CentOS: `yum install`） |
| 安装环境问题 | `E: Unable to locate package` | 包名错误或软件源未更新 | 先执行 `sudo apt update` 更新软件源，再安装 |
| 安装环境问题 | `Permission denied` | 当前用户没有执行该命令的权限 | 在命令前加 `sudo` 以管理员身份运行，或切换到 root 用户 |

### 命令执行问题

| 阶段 | 错误现象 | 原因 | 解决方案 |
|------|----------|------|----------|
| 命令执行问题 | `rm: cannot remove 'xxx': Is a directory` | 删除目录时未加 `-r` 参数 | 使用 `rm -r 目录名` 递归删除，或 `rm -rf` 强制删除（⚠️ 谨慎） |
| 命令执行问题 | `cp: omitting directory 'xxx'` | 复制目录时未加 `-r` 参数 | 使用 `cp -r 源目录 目标目录` |
| 命令执行问题 | `No such file or directory` | 文件/目录不存在，或路径拼写错误 | 使用 `ls` 确认路径；注意区分大小写；检查是否使用了相对路径但当前位置不对 |
| 命令执行问题 | `./script.sh: Permission denied` | 脚本没有执行权限 | 执行 `chmod +x script.sh` 添加执行权限 |

### 配置问题

| 阶段 | 错误现象 | 原因 | 解决方案 |
|------|----------|------|----------|
| 配置问题 | 修改配置文件后服务无法启动 | 配置文件语法错误 | 用 `cat -n` 查看修改处；检查是否有非法字符；用 `systemctl status 服务名` 查看详细错误 |
| 配置问题 | Vim 中无法输入中文或显示乱码 | 终端编码设置问题 | 在 Vim 底行模式输入 `:set encoding=utf-8` 和 `:set fileencoding=utf-8` |
| 配置问题 | `sudo: command not found` | 当前用户不在 sudoers 列表中 | 切换到 root 用户执行 `usermod -aG sudo 用户名`（Ubuntu）或 `usermod -aG wheel 用户名`（CentOS） |

### 网络问题

| 阶段 | 错误现象 | 原因 | 解决方案 |
|------|----------|------|----------|
| 网络问题 | `ping: unknown host www.baidu.com` | DNS 解析失败或网络不通 | 检查 `/etc/resolv.conf` 中的 DNS 配置；尝试 ping IP 地址如 `ping 223.5.5.5` |
| 网络问题 | `ssh: connect to host port 22: Connection refused` | SSH 服务未启动或防火墙阻挡 | 在服务器上执行 `systemctl status sshd` 检查服务；检查防火墙 `ufw status` 或 `firewall-cmd --list-ports` |
| 网络问题 | `scp: No such file or directory` | 远程路径错误或文件不存在 | 先用 `ssh` 登录到远程服务器，确认文件存在且路径正确 |
| 网络问题 | `curl: (7) Failed to connect` | 目标服务未启动或端口错误 | 在服务器本地用 `curl localhost:端口` 测试；用 `netstat -tlnp` 确认服务是否监听正确端口 |

---

## 本节小结

```
+----------------------------------------------------------+
|                    Linux 基础命令思维导图                  |
+----------------------------------------------------------+
|                                                          |
|   +----------------+    +----------------+               |
|   |   文件操作     |    |   权限管理      |               |
|   |  ls cd pwd     |    |  rwx 模型      |               |
|   |  mkdir rm cp   |    |  chmod chown   |               |
|   |  mv touch cat  |    |  755 644 700   |               |
|   +----------------+    +----------------+               |
|            |                      |                      |
|   +----------------+    +----------------+               |
|   |   进程管理     |    |   网络诊断      |               |
|   |  ps top kill   |    |  ping curl     |               |
|   |  free df       |    |  netstat ss    |               |
|   +----------------+    +----------------+               |
|            |                      |                      |
|   +----------------+    +----------------+               |
|   |   文本处理     |    |   远程操作      |               |
|   |  grep find sed |    |  ssh scp       |               |
|   |  head tail     |    |  密钥登录       |               |
|   +----------------+    +----------------+               |
|            |                      |                      |
|   +----------------+    +----------------+               |
|   |   压缩打包     |    |   编辑器        |               |
|   |  tar zip       |    |  Vim 三种模式   |               |
|   |  gzip unzip    |    |  i :wq :q!     |               |
|   +----------------+    +----------------+               |
|                                                          |
|   核心原则：先 pwd 确认位置 → 再操作 → 操作前备份        |
|                                                          |
+----------------------------------------------------------+
```

---

## 参考文档

- [Linux 命令大全 - man7.org](https://man7.org/linux/man-pages/)
- [Linux 命令行与 Shell 脚本编程大全](https://www.linuxcommand.org/)
- [Vim 官方文档](https://www.vim.org/docs.php)
- [鸟哥的 Linux 私房菜](https://linux.vbird.org/)
- [ExplainShell - 可视化解释命令](https://explainshell.com/)
