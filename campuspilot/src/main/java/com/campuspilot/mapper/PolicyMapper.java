package com.campuspilot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campuspilot.entity.Policy;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PolicyMapper extends BaseMapper<Policy> {
}