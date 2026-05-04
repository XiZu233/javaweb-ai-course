# 部门管理 CRUD

## 学习目标

- 掌握完整的 Controller / Service / Mapper 三层开发流程
- 理解 RESTful 风格在 CRUD 中的具体实践
- 能够独立完成单表的增删改查接口

## 核心知识点

### 1. 需求分析

部门管理是最基础的单表 CRUD，功能包括：
- 查询部门列表
- 根据 ID 查询部门
- 新增部门
- 修改部门
- 删除部门（需校验部门下是否有员工）

### 2. 数据库表

```sql
CREATE TABLE dept (
    id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL COMMENT '部门名称',
    create_time DATETIME DEFAULT NOW(),
    update_time DATETIME DEFAULT NOW()
);
```

### 3. 实体类

```java
@Data
public class Dept {
    private Integer id;
    private String name;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
```

### 4. Mapper 层

```java
@Mapper
public interface DeptMapper {
    @Select("select * from dept")
    List<Dept> list();

    @Delete("delete from dept where id = #{id}")
    void deleteById(Integer id);

    @Insert("insert into dept(name, create_time, update_time) values(#{name}, now(), now())")
    void insert(Dept dept);

    @Select("select * from dept where id = #{id}")
    Dept getById(Integer id);

    @Update("update dept set name = #{name}, update_time = now() where id = #{id}")
    void update(Dept dept);
}
```

### 5. Service 层

```java
public interface DeptService {
    List<Dept> list();
    void delete(Integer id);
    void save(Dept dept);
    Dept getById(Integer id);
    void update(Dept dept);
}
```

```java
@Service
public class DeptServiceImpl implements DeptService {

    @Autowired
    private DeptMapper deptMapper;

    @Autowired
    private EmpMapper empMapper;

    @Override
    public List<Dept> list() {
        return deptMapper.list();
    }

    @Override
    public void delete(Integer id) {
        // 校验：部门下是否有员工
        Long count = empMapper.countByDeptId(id);
        if (count != null && count > 0) {
            throw new RuntimeException("该部门下存在员工，无法删除");
        }
        deptMapper.deleteById(id);
    }

    @Override
    public void save(Dept dept) {
        deptMapper.insert(dept);
    }

    @Override
    public Dept getById(Integer id) {
        return deptMapper.getById(id);
    }

    @Override
    public void update(Dept dept) {
        deptMapper.update(dept);
    }
}
```

### 6. Controller 层

```java
@Slf4j
@RestController
@RequestMapping("/depts")
public class DeptController {

    @Autowired
    private DeptService deptService;

    @GetMapping
    public Result list() {
        List<Dept> list = deptService.list();
        return Result.success(list);
    }

    @GetMapping("/{id}")
    public Result getById(@PathVariable Integer id) {
        Dept dept = deptService.getById(id);
        return Result.success(dept);
    }

    @PostMapping
    public Result save(@RequestBody Dept dept) {
        deptService.save(dept);
        return Result.success();
    }

    @PutMapping
    public Result update(@RequestBody Dept dept) {
        deptService.update(dept);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Integer id) {
        deptService.delete(id);
        return Result.success();
    }
}
```

### 7. 接口测试（Postman / curl）

```bash
# 查询列表
curl http://localhost:8080/depts

# 新增
curl -X POST http://localhost:8080/depts \
  -H "Content-Type: application/json" \
  -d '{"name":"测试部"}'

# 修改
curl -X PUT http://localhost:8080/depts \
  -H "Content-Type: application/json" \
  -d '{"id":1,"name":"教研部（改名）"}'

# 删除
curl -X DELETE http://localhost:8080/depts/1
```

## 动手练习

### 练习 1：完整实现部门 CRUD

按照上面的三层架构，在项目中实现部门管理的完整接口，并用 Postman 逐一测试。

### 练习 2：删除校验

在删除部门前，检查该部门下是否还有员工。如果有，返回错误提示 "该部门下存在员工，无法删除"。

## 常见错误排查

| 错误现象 | 原因 | 解决方案 |
|---------|------|---------|
| 新增成功但返回 id 为 null | 未配置 useGeneratedKeys | 注解加 `@Options(useGeneratedKeys = true, keyProperty = "id")` |
| 删除时报外键约束错误 | 数据库层面外键约束 | 先删除子表数据，或在数据库去掉外键约束由程序控制 |
| 修改后时间未更新 | 未在 SQL 中设置 update_time | update SQL 中加 `update_time = now()` |

## 本节小结

部门管理 CRUD 是后端开发的"Hello World"。通过三层架构，Controller 负责接收请求、Service 负责业务逻辑（如删除校验）、Mapper 负责数据库操作，职责清晰，便于维护和扩展。

