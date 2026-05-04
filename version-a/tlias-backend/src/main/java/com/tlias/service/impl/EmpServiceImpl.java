package com.tlias.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.tlias.mapper.EmpExprMapper;
import com.tlias.mapper.EmpMapper;
import com.tlias.pojo.Emp;
import com.tlias.pojo.EmpExpr;
import com.tlias.service.EmpService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EmpServiceImpl implements EmpService {

    @Autowired
    private EmpMapper empMapper;

    @Autowired
    private EmpExprMapper empExprMapper;

    @Override
    public PageInfo<Emp> list(String name, Integer gender, String begin, String end, Integer page, Integer pageSize) {
        PageHelper.startPage(page, pageSize);
        List<Emp> list = empMapper.list(name, gender, begin, end);
        return new PageInfo<>(list);
    }

    @Override
    public void delete(List<Integer> ids) {
        empMapper.deleteByIds(ids);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void save(Emp emp) {
        empMapper.insert(emp);
        if (emp.getExprList() != null && !emp.getExprList().isEmpty()) {
            for (EmpExpr expr : emp.getExprList()) {
                expr.setEmpId(emp.getId());
            }
            empExprMapper.insertBatch(emp.getExprList());
        }
    }

    @Override
    public Emp getById(Integer id) {
        return empMapper.getById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Emp emp) {
        empMapper.update(emp);
        empExprMapper.deleteByEmpId(emp.getId());
        if (emp.getExprList() != null && !emp.getExprList().isEmpty()) {
            for (EmpExpr expr : emp.getExprList()) {
                expr.setEmpId(emp.getId());
            }
            empExprMapper.insertBatch(emp.getExprList());
        }
    }
}
