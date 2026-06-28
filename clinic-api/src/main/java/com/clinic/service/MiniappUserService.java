package com.clinic.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.clinic.entity.MiniappUser;

import java.util.List;
import java.util.Map;

public interface MiniappUserService extends IService<MiniappUser> {

    /**
     * 根据openid查询或创建用户（已废弃，请使用 findOrCreateByPhoneOrOpenid）
     */
    MiniappUser findOrCreateByOpenid(Map<String, String> wxData);

    /**
     * 优先根据手机号查找用户，找不到再根据openid查找或创建
     * @param phone 手机号
     * @param wxData 微信数据（包含openid等）
     * @return 用户对象
     */
    MiniappUser findOrCreateByPhoneOrOpenid(String phone, Map<String, String> wxData);

    /**
     * 更新登录信息
     */
    void updateLoginInfo(Long userId);

    /**
     * 绑定患者
     */
    boolean bindPatient(Long miniappUserId, Long patientId);

    /**
     * 切换诊所
     */
    boolean switchClinic(Long miniappUserId, Long clinicId);
    
    /**
     * 获取用户关联的诊所列表
     */
    List<Map<String, Object>> getUserClinics(Long miniappUserId);
    
    /**
     * 为用户添加诊所关联
     */
    boolean addUserClinicRelation(Long miniappUserId, Long clinicId, Integer roleType, boolean isDefault);
}
