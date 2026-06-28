package com.clinic.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.clinic.entity.Doctor;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface DoctorMapper extends BaseMapper<Doctor> {

    /**
     * 根据部门ID查询部门名称
     * @param deptId 部门ID
     * @return 部门名称
     */
    @Select("SELECT dept_name FROM department WHERE id = #{deptId} AND deleted = 0")
    String selectDeptNameByDeptId(@Param("deptId") Long deptId);

    /**
     * 根据诊所ID和医生姓名查询医生ID
     * @param clinicId 诊所ID
     * @param doctorName 医生姓名
     * @return 医生ID
     */
    @Select("SELECT id FROM doctor WHERE clinic_id = #{clinicId} AND doctor_name = #{doctorName} AND deleted = 0 LIMIT 1")
    Long selectIdByClinicIdAndName(@Param("clinicId") Long clinicId, @Param("doctorName") String doctorName);

    /**
     * 根据医生ID查询dept_id
     * @param doctorId 医生ID
     * @return dept_id
     */
    @Select("SELECT dept_id FROM doctor WHERE id = #{doctorId} AND deleted = 0 LIMIT 1")
    Long selectDeptIdByDoctorId(@Param("doctorId") Long doctorId);
}
