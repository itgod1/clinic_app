package com.clinic.service;

import com.clinic.common.exception.BusinessException;
import com.clinic.entity.Patient;
import com.clinic.mapper.PatientMapper;
import com.clinic.service.impl.miniapp.MiniappPatientServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 小程序建档 bind-first 单测：命中后台未绑定档案时认领而非新建，
 * 避免同一人双档案把挂号/药单历史拆散。
 */
@ExtendWith(MockitoExtension.class)
class MiniappPatientBindFirstTest {

    @Mock
    PatientMapper patientMapper;

    @InjectMocks
    MiniappPatientServiceImpl service;

    private Patient requestOf(String name, String phone) {
        Patient p = new Patient();
        p.setPatientName(name);
        p.setPhone(phone);
        p.setClinicId(1L);
        return p;
    }

    @Test
    void unboundArchiveIsClaimedInsteadOfInserted() {
        // 后台建过同诊所同手机号的张三档案（未绑 user_id）
        Patient unbound = requestOf("张三", "13800000001");
        unbound.setId(1L);
        when(patientMapper.selectCount(any())).thenReturn(0L);
        when(patientMapper.selectOne(any())).thenReturn(unbound);

        service.createMyPatient(35L, requestOf("张三", "13800000001"));

        assertEquals(35L, unbound.getUserId()); // 认领：补 user_id
        verify(patientMapper).updateById(unbound);
        verify(patientMapper, never()).insert(any(Patient.class));
    }

    @Test
    void noUnboundArchiveFallsBackToInsert() {
        when(patientMapper.selectCount(any())).thenReturn(0L);
        when(patientMapper.selectOne(any())).thenReturn(null);

        service.createMyPatient(35L, requestOf("李四", "13800000002"));

        verify(patientMapper).insert(any(Patient.class));
        verify(patientMapper, never()).updateById(any(Patient.class));
    }

    @Test
    void duplicateBoundPhoneStillRejected() {
        when(patientMapper.selectCount(any())).thenReturn(1L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.createMyPatient(35L, requestOf("张三", "13800000001")));
        assertTrue(ex.getMessage().contains("已添加"));
        verify(patientMapper, never()).insert(any(Patient.class));
        verify(patientMapper, never()).updateById(any(Patient.class));
    }
}
