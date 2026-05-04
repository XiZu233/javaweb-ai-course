package com.tlias.mapper;

import com.tlias.pojo.Emp;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface EmpMapper {

    List<Emp> list(@Param("name") String name,
                   @Param("gender") Integer gender,
                   @Param("begin") String begin,
                   @Param("end") String end);

    void deleteByIds(@Param("ids") List<Integer> ids);

    @Insert("insert into emp(username, password, name, gender, image, job, entrydate, dept_id, create_time, update_time) " +
            "values(#{username}, #{password}, #{name}, #{gender}, #{image}, #{job}, #{entrydate}, #{deptId}, now(), now())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(Emp emp);

    Emp getById(Integer id);

    void update(Emp emp);

    @Select("select count(*) from emp where dept_id = #{deptId}")
    Long countByDeptId(Integer deptId);
}
