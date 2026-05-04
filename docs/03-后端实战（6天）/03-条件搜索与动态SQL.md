# 条件搜索与动态 SQL

## 学习目标

- 理解动态 SQL 的应用场景
- 掌握 MyBatis XML 中 `<if>`、`<where>`、`<foreach>`、`<set>` 的使用
- 能够实现多条件组合查询和批量操作

## 核心知识点

### 1. 需求场景

员工列表页面通常有多个搜索条件：
- 姓名（模糊搜索）
- 性别（精确匹配）
- 入职日期范围

这些条件可能部分为空，需要动态拼接 SQL。

### 2. 动态 SQL 标签

**`<if>` 条件判断**：

```xml
<select id="list" resultType="Emp">
    select * from emp
    where 1=1
    <if test="name != null and name != ''">
        and name like concat('%', #{name}, '%')
    </if>
    <if test="gender != null">
        and gender = #{gender}
    </if>
</select>
```

**`<where>` 智能 WHERE**：

```xml
<select id="list" resultMap="empResultMap">
    select e.*, d.name as dept_name
    from emp e
    left join dept d on e.dept_id = d.id
    <where>
        <if test="name != null and name != ''">
            and e.name like concat('%', #{name}, '%')
        </if>
        <if test="gender != null">
            and e.gender = #{gender}
        </if>
        <if test="begin != null and begin != ''">
            and e.entrydate &gt;= #{begin}
        </if>
        <if test="end != null and end != ''">
            and e.entrydate &lt;= #{end}
        </if>
    </where>
    order by e.update_time desc
</select>
```

`<where>` 的聪明之处：
- 自动添加 `WHERE` 关键字
- 自动去掉第一个多余的 `AND` 或 `OR`
- 如果没有任何条件，则不生成 WHERE

**`<foreach>` 循环遍历**：

```xml
<!-- 批量删除 -->
<delete id="deleteByIds">
    delete from emp where id in
    <foreach collection="ids" item="id" separator="," open="(" close=")">
        #{id}
    </foreach>
</delete>
```

`<foreach>` 属性说明：
- `collection`：传入的集合参数名
- `item`：集合中每个元素的变量名
- `separator`：元素之间的分隔符
- `open` / `close`：整个循环的开头和结尾

**`<set>` 智能 SET**：

```xml
<update id="update">
    update emp
    <set>
        <if test="username != null">username = #{username},</if>
        <if test="name != null">name = #{name},</if>
        <if test="gender != null">gender = #{gender},</if>
        <if test="image != null">image = #{image},</if>
        <if test="job != null">job = #{job},</if>
        <if test="entrydate != null">entrydate = #{entrydate},</if>
        <if test="deptId != null">dept_id = #{deptId},</if>
        update_time = now()
    </set>
    where id = #{id}
</update>
```

`<set>` 的聪明之处：
- 自动添加 `SET` 关键字
- 自动去掉最后一个多余的逗号

### 3. 完整 XML 示例

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper
        PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.tlias.mapper.EmpMapper">

    <resultMap id="empResultMap" type="com.tlias.pojo.Emp">
        <id property="id" column="id"/>
        <result property="deptName" column="dept_name"/>
        <collection property="exprList" ofType="com.tlias.pojo.EmpExpr"
                    select="com.tlias.mapper.EmpExprMapper.getByEmpId" column="id"/>
    </resultMap>

    <select id="list" resultMap="empResultMap">
        select e.*, d.name as dept_name
        from emp e
        left join dept d on e.dept_id = d.id
        <where>
            <if test="name != null and name != ''">
                and e.name like concat('%', #{name}, '%')
            </if>
            <if test="gender != null">
                and e.gender = #{gender}
            </if>
            <if test="begin != null and begin != ''">
                and e.entrydate &gt;= #{begin}
            </if>
            <if test="end != null and end != ''">
                and e.entrydate &lt;= #{end}
            </if>
        </where>
        order by e.update_time desc
    </select>

    <delete id="deleteByIds">
        delete from emp where id in
        <foreach collection="ids" item="id" separator="," open="(" close=")">
            #{id}
        </foreach>
    </delete>

    <update id="update">
        update emp
        <set>
            <if test="username != null">username = #{username},</if>
            <if test="name != null">name = #{name},</if>
            <if test="gender != null">gender = #{gender},</if>
            <if test="image != null">image = #{image},</if>
            <if test="job != null">job = #{job},</if>
            <if test="entrydate != null">entrydate = #{entrydate},</if>
            <if test="deptId != null">dept_id = #{deptId},</if>
            update_time = now()
        </set>
        where id = #{id}
    </update>

</mapper>
```

### 4. Mapper 接口与 XML 的对应关系

```java
@Mapper
public interface EmpMapper {
    List<Emp> list(@Param("name") String name,
                   @Param("gender") Integer gender,
                   @Param("begin") String begin,
                   @Param("end") String end);

    void deleteByIds(@Param("ids") List<Integer> ids);

    void update(Emp emp);
}
```

> 当参数超过 1 个时，建议用 `@Param` 指定参数名，否则 XML 中可能无法正确引用。

## 动手练习

### 练习 1：动态 SQL 调试

1. 调用 `/emps?name=张` 观察生成的 SQL（开启 MyBatis 日志）
2. 调用 `/emps?gender=1` 观察 WHERE 条件变化
3. 调用 `/emps` 不带任何参数，观察是否省略了 WHERE

### 练习 2：批量删除

实现批量删除员工接口：
```java
@DeleteMapping("/{ids}")
public Result delete(@PathVariable List<Integer> ids)
```

对应的 XML 动态 SQL 怎么写？（参考上面的 `<foreach>` 示例）

## 常见错误排查

| 错误现象 | 原因 | 解决方案 |
|---------|------|---------|
| Parameter 'xxx' not found | 参数名不匹配或缺少 @Param | 添加 @Param 注解 |
| 动态条件全部未生效 | if 判断条件写错 | 检查 test 表达式，注意 `and` 和 `or` 的优先级 |
| XML 中特殊字符报错 | <、>、& 等未转义 | 使用 &lt; &gt; &amp; 或 CDATA 包裹 |
| There is no getter for property | 实体类字段名与 #{} 中不一致 | 检查大小写和驼峰/下划线转换 |

## 本节小结

动态 SQL 是 MyBatis 的核心特性之一。`<where>` 自动处理 WHERE 和 AND，`<set>` 自动处理 SET 和逗号，`<foreach>` 实现批量操作，让 SQL 编写更加灵活和安全。

## 参考文档

- [MyBatis 动态 SQL 官方文档](https://mybatis.org/mybatis-3/zh/dynamic-sql.html)

