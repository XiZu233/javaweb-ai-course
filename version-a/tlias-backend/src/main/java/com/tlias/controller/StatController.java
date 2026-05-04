package com.tlias.controller;

import com.tlias.mapper.EmpMapper;
import com.tlias.pojo.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/stats")
public class StatController {

    @Autowired
    private EmpMapper empMapper;

    @GetMapping("/job")
    public Result jobStat() {
        // 职位分布统计
        List<Map<String, Object>> list = List.of(
                Map.of("name", "班主任", "value", 2),
                Map.of("name", "讲师", "value", 2),
                Map.of("name", "学工主管", "value", 1),
                Map.of("name", "教研主管", "value", 1),
                Map.of("name", "咨询师", "value", 1),
                Map.of("name", "其他", "value", 1)
        );
        return Result.success(list);
    }

    @GetMapping("/gender")
    public Result genderStat() {
        List<Map<String, Object>> list = List.of(
                Map.of("name", "男", "value", 4),
                Map.of("name", "女", "value", 4)
        );
        return Result.success(list);
    }
}
