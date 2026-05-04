package com.tlias.service;

import com.github.pagehelper.PageInfo;
import com.tlias.pojo.Emp;

import java.util.List;

public interface EmpService {
    PageInfo<Emp> list(String name, Integer gender, String begin, String end, Integer page, Integer pageSize);

    void delete(List<Integer> ids);

    void save(Emp emp);

    Emp getById(Integer id);

    void update(Emp emp);
}
