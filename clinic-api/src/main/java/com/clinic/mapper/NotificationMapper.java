package com.clinic.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.clinic.entity.Notification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface NotificationMapper extends BaseMapper<Notification> {

    @Select("SELECT * FROM notification WHERE clinic_id = #{clinicId} " +
            "AND doctor_id = #{doctorId} " +
            "AND is_read = 0 " +
            "AND deleted = 0 " +
            "ORDER BY created_at DESC")
    List<Notification> selectUnreadByDoctor(@Param("clinicId") Long clinicId,
                                             @Param("doctorId") Long doctorId);

    @Select("SELECT * FROM notification WHERE clinic_id = #{clinicId} " +
            "AND doctor_id = #{doctorId} " +
            "AND deleted = 0 " +
            "ORDER BY created_at DESC " +
            "LIMIT #{limit}")
    List<Notification> selectRecentByDoctor(@Param("clinicId") Long clinicId,
                                             @Param("doctorId") Long doctorId,
                                             @Param("limit") Integer limit);

    @Update("UPDATE notification SET is_read = 1 " +
            "WHERE id = #{id} AND doctor_id = #{doctorId}")
    int markAsRead(@Param("id") Long id, @Param("doctorId") Long doctorId);

    @Select("SELECT COUNT(*) FROM notification WHERE clinic_id = #{clinicId} " +
            "AND doctor_id = #{doctorId} " +
            "AND is_read = 0 " +
            "AND deleted = 0")
    Long countUnread(@Param("clinicId") Long clinicId,
                     @Param("doctorId") Long doctorId);
}
