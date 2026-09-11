package com.campuspilot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campuspilot.entity.Task;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TaskMapper extends BaseMapper<Task> {
}