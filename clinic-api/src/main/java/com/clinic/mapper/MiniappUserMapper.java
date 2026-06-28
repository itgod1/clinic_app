package com.clinic.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.clinic.entity.MiniappUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface MiniappUserMapper extends BaseMapper<MiniappUser> {

    /**
     * 根据openid查询用户
     */
    MiniappUser selectByOpenid(@Param("openid") String openid);

    /**
     * 根据手机号查询用户
     */
    MiniappUser selectByPhone(@Param("phone") String phone);

    /**
     * 更新登录信息
     */
    @Update("UPDATE miniapp_user SET last_login_time = NOW(), login_count = login_count + 1 WHERE id = #{userId}")
    int updateLoginInfo(@Param("userId") Long userId);
}
