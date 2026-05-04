# 部门管理 CRUD

## 学习目标

学完本节后，你将能够：
- 理解 CRUD 是什么：增（Create）、查（Read）、改（Update）、删（Delete）
- 独立完成一个完整单表的全部接口（5 个：列表、详情、新增、修改、删除）
- 使用 Postman 测试每一个接口
- 理解三层架构在 CRUD 场景下的具体落地

---

## 核心知识点

### 1. 什么是 CRUD——后端开发的"四件套"

#### 1.1 CRUD 的字面含义

```
C - Create  → 创建（新增数据）
R - Read    → 读取（查询数据）
U - Update  → 更新（修改数据）
D - Delete  → 删除（移除数据）
```

**为什么说 CRUD 是后端的 Hello World？**

任何业务系统，无论多么复杂，剥到最里面，都是在"操作数据"。
- 微博发帖 → 创建一条 Tweet
- 刷朋友圈 → 读取好友的动态
- 修改个人资料 → 更新用户信息
- 删除聊天记录 → 删除消息

学会一张表的 CRUD，就掌握了 80% 的 Web 开发模式。剩下的 20% 是性能优化、复杂业务、安全控制。

#### 1.2 RESTful 风格的 CRUD

按照 RESTful 规范，部门管理的 5 个接口应该这样设计：

| 操作 | HTTP 方法 | URL | 说明 |
|------|----------|-----|------|
| 查询列表 | GET | `/depts` | 获取所有部门 |
| 查询详情 | GET | `/depts/{id}` | 获取 id=1 的部门 |
| 新增 | POST | `/depts` | 创建新部门 |
| 修改 | PUT | `/depts` | 修改部门（id 在请求体里） |
| 删除 | DELETE | `/depts/{id}` | 删除 id=1 的部门 |

**记忆方法：**
- URL 永远是名词复数（`/depts` 不是 `/getDepts`）
- 动作由 HTTP 方法决定（GET 查、POST 增、PUT 改、DELETE 删）

---

### 2. 需求分析——动手前先想清楚

#### 2.1 业务需求

部门管理是 Tlias 人事系统最基础的模块：
1. 列表页：管理员能看到公司所有部门
2. 详情页：点击某个部门能看到详细信息
3. 新增：可以创建新部门（如成立"AI 实验室"）
4. 修改：可以改名（如"研发部"改成"技术中心"）
5. 删除：可以删除空部门（**关键约束：部门下还有员工时不能删！**）

#### 2.2 接口设计清单

```
┌──────────────────────────────────────────────────────────────────────┐
│  部门管理接口清单                                                     │
├──────────────────────────────────────────────────────────────────────┤
│  ① GET    /depts          → 查询所有部门                              │
│     入参：无                                                          │
│     返回：[{id, name, createTime, updateTime}, ...]                   │
│                                                                       │
│  ② GET    /depts/{id}     → 查询单个部门                              │
│     入参：id（路径参数）                                              │
│     返回：{id, name, createTime, updateTime}                          │
│                                                                       │
│  ③ POST   /depts          → 新增部门                                  │
│     入参：{name}                                                      │
│     返回：成功提示                                                    │
│                                                                       │
│  ④ PUT    /depts          → 修改部门                                  │
│     入参：{id, name}                                                  │
│     返回：成功提示                                                    │
│                                                                       │
│  ⑤ DELETE /depts/{id}     → 删除部门                                  │
│     入参：id（路径参数）                                              │
│     返回：成功提示（如果部门下有员工，返回错误）                       │
└──────────────────────────────────────────────────────────────────────┘
```

---

### 3. 数据库设计

#### 3.1 部门表 dept

```sql
CREATE TABLE dept (
    id          INT          PRIMARY KEY AUTO_INCREMENT  COMMENT '主键',
    name        VARCHAR(50)  NOT NULL                    COMMENT '部门名称',
    create_time DATETIME     DEFAULT NOW()               COMMENT '创建时间',
    update_time DATETIME     DEFAULT NOW()               COMMENT '更新时间'
) COMMENT '部门表';
```

**字段说明：**

| 字段 | 类型 | 含义 | 备注 |
|------|------|------|------|
| id | INT | 主键 | 自增（每插一条 +1） |
| name | VARCHAR(50) | 部门名称 | 不能为空，最多 50 字符 |
| create_time | DATETIME | 创建时间 | 默认当前时间 |
| update_time | DATETIME | 更新时间 | 修改时手动更新 |

#### 3.2 初始化数据

```sql
INSERT INTO dept (id, name) VALUES
(1, '学工部'),
(2, '教研部'),
(3, '咨询部'),
(4, '就业部'),
(5, '人事部');
```

---

### 4. 实体类（POJO）

```java
package com.tlias.pojo;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 部门实体类
 * 字段对应 dept 表
 */
@Data  // Lombok 注解：自动生成 getter/setter/toString
public class Dept {
    private Integer id;              // 对应 dept.id
    private String name;             // 对应 dept.name
    private LocalDateTime createTime; // 对应 dept.create_time（驼峰命名自动映射）
    private LocalDateTime updateTime; // 对应 dept.update_time
}
```

**关键点：**
- 数据库字段 `create_time`（下划线）→ Java 字段 `createTime`（驼峰）
- 默认情况下需要在 application.yml 开启自动转换：
  ```yaml
  mybatis:
      configuration:
          map-underscore-to-camel-case: true  # 下划线转驼峰
  ```

---

### 5. Mapper 层——直接和数据库对话

#### 5.1 接口定义

```java
package com.tlias.mapper;

import com.tlias.pojo.Dept;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper  // ★ 关键注解：让 Spring 知道这是 MyBatis Mapper，自动生成代理对象
public interface DeptMapper {

    // ========== 查询所有部门 ==========
    @Select("select * from dept order by update_time desc")
    List<Dept> list();

    // ========== 根据 ID 查询 ==========
    @Select("select * from dept where id = #{id}")
    Dept getById(Integer id);

    // ========== 新增 ==========
    @Options(useGeneratedKeys = true, keyProperty = "id")  // 让 MySQL 自增的 id 回填到对象
    @Insert("insert into dept(name, create_time, update_time) " +
            "values(#{name}, now(), now())")
    void insert(Dept dept);

    // ========== 修改 ==========
    @Update("update dept " +
            "set name = #{name}, update_time = now() " +
            "where id = #{id}")
    void update(Dept dept);

    // ========== 删除 ==========
    @Delete("delete from dept where id = #{id}")
    void deleteById(Integer id);
}
```

#### 5.2 关键注解详解

| 注解 | 作用 | 例子 |
|------|------|------|
| `@Mapper` | 标记接口为 MyBatis Mapper | 类上 |
| `@Select` | 查询 SQL | `@Select("select * from dept")` |
| `@Insert` | 新增 SQL | `@Insert("insert into dept...")` |
| `@Update` | 修改 SQL | `@Update("update dept set...")` |
| `@Delete` | 删除 SQL | `@Delete("delete from dept...")` |
| `@Options` | 主键回填 | `@Options(useGeneratedKeys=true, keyProperty="id")` |

**`#{}` vs `${}` 的区别：**

```java
// #{} 预编译参数（推荐！防止 SQL 注入）
@Select("select * from dept where id = #{id}")
// 实际执行：select * from dept where id = ?  然后传参 1

// ${} 字符串拼接（危险！可能 SQL 注入）
@Select("select * from dept where id = ${id}")
// 实际执行：select * from dept where id = 1
// 如果传入的 id 是 "1 or 1=1"，会查出全部数据！
```

**口诀：能用 `#{}` 绝不用 `${}`，除非动态表名或字段名。**

---

### 6. Service 层——业务逻辑的"指挥中心"

#### 6.1 为什么要分接口和实现？

```java
// 接口 DeptService.java 定义"做什么"
public interface DeptService {
    List<Dept> list();
    Dept getById(Integer id);
    void save(Dept dept);
    void update(Dept dept);
    void delete(Integer id);
}

// 实现 DeptServiceImpl.java 定义"怎么做"
@Service  // ★ 关键注解：让 Spring 自动管理这个对象
public class DeptServiceImpl implements DeptService {
    // 具体实现...
}
```

**好处：**
- Controller 依赖接口，不依赖具体实现
- 未来要换实现（比如改用缓存版的 Service）只需改一个地方
- 便于单元测试（可以 mock 接口）

#### 6.2 完整实现

```java
package com.tlias.service.impl;

import com.tlias.mapper.DeptMapper;
import com.tlias.mapper.EmpMapper;
import com.tlias.pojo.Dept;
import com.tlias.service.DeptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class DeptServiceImpl implements DeptService {

    @Autowired
    private DeptMapper deptMapper;  // 注入部门 Mapper

    @Autowired
    private EmpMapper empMapper;    // 注入员工 Mapper（删除时校验用）

    @Override
    public List<Dept> list() {
        // 直接调用 Mapper，没有业务逻辑
        return deptMapper.list();
    }

    @Override
    public Dept getById(Integer id) {
        return deptMapper.getById(id);
    }

    @Override
    public void save(Dept dept) {
        // 注意：create_time 和 update_time 在 SQL 里用 now() 设置了
        // 也可以在这里手动设置：
        // dept.setCreateTime(LocalDateTime.now());
        // dept.setUpdateTime(LocalDateTime.now());
        deptMapper.insert(dept);
    }

    @Override
    public void update(Dept dept) {
        deptMapper.update(dept);
    }

    @Override
    public void delete(Integer id) {
        // ★ 业务校验：部门下是否有员工
        Long count = empMapper.countByDeptId(id);
        if (count != null && count > 0) {
            // 抛出异常，由全局异常处理器统一返回
            throw new RuntimeException("该部门下存在员工，无法删除");
        }
        // 校验通过，执行删除
        deptMapper.deleteById(id);
    }
}
```

#### 6.3 业务校验的位置

**为什么校验放在 Service 层，而不是 Controller 或 Mapper？**

| 层级 | 职责 | 不该做的事 |
|------|------|-----------|
| Controller | 接收/响应 HTTP | 不写业务规则（如"部门有员工不能删"） |
| Service | **业务规则** | 不写 SQL（交给 Mapper） |
| Mapper | 数据库操作 | 不写业务判断 |

**反面教材（错误示范）：**

```java
// ❌ 把校验放在 Controller
@DeleteMapping("/{id}")
public Result delete(@PathVariable Integer id) {
    Long count = empService.countByDeptId(id);  // 错！Controller 不该写业务
    if (count > 0) {
        return Result.error("该部门下存在员工");
    }
    deptService.delete(id);
    return Result.success();
}
```

---

### 7. Controller 层——接收前端请求

#### 7.1 完整实现

```java
package com.tlias.controller;

import com.tlias.pojo.Dept;
import com.tlias.pojo.Result;
import com.tlias.service.DeptService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Slf4j                       // Lombok 提供日志对象 log
@RestController              // = @Controller + @ResponseBody，返回 JSON
@RequestMapping("/depts")    // 所有接口都以 /depts 开头
public class DeptController {

    @Autowired
    private DeptService deptService;

    /**
     * 查询所有部门列表
     * GET /depts
     */
    @GetMapping
    public Result list() {
        log.info("查询所有部门");
        List<Dept> list = deptService.list();
        return Result.success(list);
    }

    /**
     * 根据 ID 查询部门
     * GET /depts/1
     */
    @GetMapping("/{id}")
    public Result getById(@PathVariable Integer id) {
        log.info("根据 ID 查询部门，id={}", id);
        Dept dept = deptService.getById(id);
        return Result.success(dept);
    }

    /**
     * 新增部门
     * POST /depts  body: { "name": "新部门" }
     */
    @PostMapping
    public Result save(@RequestBody Dept dept) {
        log.info("新增部门：{}", dept);
        deptService.save(dept);
        return Result.success();
    }

    /**
     * 修改部门
     * PUT /depts  body: { "id": 1, "name": "新名字" }
     */
    @PutMapping
    public Result update(@RequestBody Dept dept) {
        log.info("修改部门：{}", dept);
        deptService.update(dept);
        return Result.success();
    }

    /**
     * 删除部门
     * DELETE /depts/1
     */
    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Integer id) {
        log.info("删除部门，id={}", id);
        deptService.delete(id);
        return Result.success();
    }
}
```

#### 7.2 注解详解

| 注解 | 作用 |
|------|------|
| `@RestController` | 标记这是 REST 控制器，返回 JSON |
| `@RequestMapping("/depts")` | 类级别路径前缀 |
| `@GetMapping` | 处理 GET 请求 |
| `@PostMapping` | 处理 POST 请求 |
| `@PutMapping` | 处理 PUT 请求 |
| `@DeleteMapping` | 处理 DELETE 请求 |
| `@PathVariable` | 从 URL 路径中取参数（`/depts/{id}`） |
| `@RequestBody` | 从请求体中取 JSON 转成对象 |

---

### 8. 接口测试——用 Postman 验证

#### 8.1 启动后端

```bash
cd version-a/tlias-backend
mvn spring-boot:run
# 看到 "Started TliasApplication" 即启动成功
```

#### 8.2 测试每一个接口

**① 查询列表**

```
GET http://localhost:8080/depts
```

期望返回：
```json
{
    "code": 1,
    "msg": "success",
    "data": [
        { "id": 1, "name": "学工部", "createTime": "2024-01-01T10:00:00", "updateTime": "2024-01-01T10:00:00" },
        { "id": 2, "name": "教研部", ... }
    ]
}
```

**② 查询详情**

```
GET http://localhost:8080/depts/1
```

**③ 新增**

```
POST http://localhost:8080/depts
Content-Type: application/json

{
    "name": "AI 实验室"
}
```

**④ 修改**

```
PUT http://localhost:8080/depts
Content-Type: application/json

{
    "id": 1,
    "name": "学工部（更名）"
}
```

**⑤ 删除**

```
DELETE http://localhost:8080/depts/5
```

如果该部门下有员工，应该返回错误：
```json
{
    "code": 0,
    "msg": "该部门下存在员工，无法删除",
    "data": null
}
```

---

## 动手练习

### 练习 1：完整实现部门 CRUD

按照本节内容，从 Controller / Service / Mapper / Dept 实体类全部写一遍，运行起来。

**完成标准：**
1. 能用 Postman 调通 5 个接口
2. 删除有员工的部门时返回错误提示
3. 新增成功后返回的 dept 对象包含自增 id

### 练习 2：增加字段校验

要求新增/修改时校验：
- 部门名称不能为空
- 部门名称不能超过 50 字符

**提示：** 用 `@NotBlank` 和 `@Size` 注解（需要 spring-boot-starter-validation 依赖）：

```java
public class Dept {
    @NotBlank(message = "部门名称不能为空")
    @Size(max = 50, message = "部门名称不能超过 50 字符")
    private String name;
}

@PostMapping
public Result save(@Validated @RequestBody Dept dept) { ... }
```

---

## 常见错误排查

### 阶段 1：启动报错

| 错误现象 | 原因分析 | 解决方案 |
|---------|---------|---------|
| `Failed to configure DataSource` | application.yml 数据库配置错误 | 检查 url、username、password |
| `Communications link failure` | MySQL 没启动 | 启动 MySQL 服务 |
| `Mapper bean not found` | 没加 `@Mapper` 或没扫描 | Mapper 接口加 `@Mapper`，或在启动类加 `@MapperScan` |

### 阶段 2：接口调通问题

| 错误现象 | 原因分析 | 解决方案 |
|---------|---------|---------|
| `404 Not Found` | URL 写错 | 检查 `@RequestMapping` 和方法上的路径 |
| `405 Method Not Allowed` | HTTP 方法不对 | GET/POST/PUT/DELETE 要匹配 |
| `415 Unsupported Media Type` | 请求体格式错误 | 设置请求头 `Content-Type: application/json` |

### 阶段 3：数据问题

| 错误现象 | 原因分析 | 解决方案 |
|---------|---------|---------|
| 新增成功但 id 为 null | 没加 `@Options(useGeneratedKeys=true, keyProperty="id")` | 加上注解 |
| 字段值都是 null | 数据库字段下划线，Java 字段驼峰 | application.yml 开启 `map-underscore-to-camel-case` |
| 时间格式乱码 | LocalDateTime 序列化问题 | 加 Jackson 配置或 `@JsonFormat` |

### 阶段 4：业务逻辑问题

| 错误现象 | 原因分析 | 解决方案 |
|---------|---------|---------|
| 删除时报外键约束 | 数据库有外键约束 | 先删子表数据，或改为程序控制（推荐） |
| 修改后 update_time 没变 | SQL 没更新 | `update dept set name=#{name}, update_time=now()` |
| 校验异常没返回友好信息 | 没有全局异常处理器 | 添加 `@RestControllerAdvice` 处理（见后续章节） |

---

## 本节小结

```
┌────────────────────────────────────────────────────────────────────┐
│                      部门管理 CRUD                                  │
├────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  CRUD = Create + Read + Update + Delete                             │
│                                                                     │
│  RESTful 接口设计：                                                  │
│    GET    /depts        → 查询列表                                   │
│    GET    /depts/{id}   → 查询详情                                   │
│    POST   /depts        → 新增                                       │
│    PUT    /depts        → 修改                                       │
│    DELETE /depts/{id}   → 删除                                       │
│                                                                     │
│  三层职责：                                                          │
│    Controller → 接收 HTTP 请求，参数校验                              │
│    Service    → 业务逻辑（如删除前校验是否有员工）                     │
│    Mapper     → 数据库操作（SQL）                                    │
│                                                                     │
│  关键注解：                                                          │
│    @RestController + @RequestMapping                                │
│    @GetMapping / @PostMapping / @PutMapping / @DeleteMapping        │
│    @PathVariable（路径参数）/ @RequestBody（请求体）                  │
│    @Service / @Mapper / @Autowired                                  │
│                                                                     │
│  避坑：                                                              │
│    • 用 #{} 不用 ${}（防 SQL 注入）                                  │
│    • 业务校验放 Service 层                                            │
│    • 数据库字段下划线，Java 字段驼峰                                  │
│    • Insert 后想要 id，必须加 @Options                                │
│                                                                     │
└────────────────────────────────────────────────────────────────────┘
```

---

## 参考文档

- [Spring Boot 官方文档](https://spring.io/projects/spring-boot)
- [MyBatis 官方文档](https://mybatis.org/mybatis-3/zh/)
- [RESTful API 设计指南](https://restfulapi.net/)
