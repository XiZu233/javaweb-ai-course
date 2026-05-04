package com.tlias.mapper;

import com.tlias.pojo.EmpExpr;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface EmpExprMapper {

    @Insert("<script>" +
            "insert into emp_expr(emp_id, begin, end, company, job) values " +
            "<foreach collection='exprList' item='expr' separator=','>" +
            "(#{expr.empId}, #{expr.begin}, #{expr.end}, #{expr.company}, #{expr.job})" +
            "</foreach>" +
            "</script>")
    void insertBatch(List<EmpExpr> exprList);

    @Select("select * from emp_expr where emp_id = #{empId}")
    List<EmpExpr> getByEmpId(Integer empId);

    @Delete("delete from emp_expr where emp_id = #{empId}")
    void deleteByEmpId(Integer empId);
}
