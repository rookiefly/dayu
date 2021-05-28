package com.rookiefly.open.dubbo.dayu.dao.mapper;

import com.rookiefly.open.dubbo.dayu.model.entity.InvokeDO;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InvokeDOMapper {

    int insertSelective(InvokeDO record);

    int updateByPrimaryKeySelective(InvokeDO record);

    List<InvokeDO> selectByInvokeDO(InvokeDO searchDO);

    int deleteByDate(@Param(value = "date") String minDate);

}