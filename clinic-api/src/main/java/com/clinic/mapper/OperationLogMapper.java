package com.clinic.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.clinic.entity.OperationLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface OperationLogMapper extends BaseMapper<OperationLog> {

    /**
     * 分页查询操作日志
     */
    @Select("<script>" +
            "SELECT * FROM operation_log WHERE clinic_id = #{clinicId} " +
            "<if test='module != null and module != \"\"'> AND module = #{module} </if>" +
            "<if test='operationType != null'> AND operation_type = #{operationType} </if>" +
            "<if test='username != null and username != \"\"'> AND username LIKE CONCAT('%', #{username}, '%') </if>" +
            "<if test='startTime != null'> AND operation_time &gt;= #{startTime} </if>" +
            "<if test='endTime != null'> AND operation_time &lt;= #{endTime} </if>" +
            "<if test='status != null'> AND status = #{status} </if>" +
            "ORDER BY created_at DESC" +
            "</script>")
    IPage<OperationLog> selectPageList(Page<OperationLog> page,
                                        @Param("clinicId") Long clinicId,
                                        @Param("module") String module,
                                        @Param("operationType") Integer operationType,
                                        @Param("username") String username,
                                        @Param("startTime") LocalDateTime startTime,
                                        @Param("endTime") LocalDateTime endTime,
                                        @Param("status") Integer status);

    /**
     * 查询用户最近的登录日志
     */
    @Select("SELECT * FROM operation_log WHERE user_id = #{userId} AND operation_type = 6 " +
            "ORDER BY created_at DESC LIMIT #{limit}")
    List<OperationLog> selectRecentLoginLogs(@Param("userId") Long userId, @Param("limit") Integer limit);

    /**
     * 统计某段时间内的操作次数
     */
    @Select("SELECT COUNT(*) FROM operation_log WHERE operation_time BETWEEN #{startTime} AND #{endTime} " +
            "<if test='clinicId != null'> AND clinic_id = #{clinicId} </if>")
    Long countByTimeRange(@Param("startTime") LocalDateTime startTime,
                          @Param("endTime") LocalDateTime endTime,
                          @Param("clinicId") Long clinicId);
}
