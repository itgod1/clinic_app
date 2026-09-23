package com.clinic.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.clinic.common.exception.BusinessException;
import com.clinic.entity.Patient;
import com.clinic.entity.Prescription;
import com.clinic.mapper.DepartmentMapper;
import com.clinic.mapper.DoctorMapper;
import com.clinic.mapper.MedicalRecordMapper;
import com.clinic.mapper.PatientMapper;
import com.clinic.mapper.PrescriptionItemMapper;
import com.clinic.mapper.PrescriptionMapper;
import com.clinic.mapper.RegistrationMapper;
import com.clinic.mapper.StockItemMapper;
import com.clinic.service.impl.PrescriptionServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * 患者侧药单查询单测：归属过滤
 */
@ExtendWith(MockitoExtension.class)
class PrescriptionForUserTest {

    @Mock
    PrescriptionMapper prescriptionMapper;
    @Mock
    PrescriptionItemMapper prescriptionItemMapper;
    @Mock
    StockItemMapper stockItemMapper;
    @Mock
    DoctorMapper doctorMapper;
    @Mock
    DepartmentMapper departmentMapper;
    @Mock
    RegistrationMapper registrationMapper;
    @Mock
    MedicalRecordService medicalRecordService;
    @Mock
    MedicalRecordMapper medicalRecordMapper;
    @Mock
    PatientMapper patientMapper;

    @InjectMocks
    PrescriptionServiceImpl service;

    private Patient patientOf(Long id, Long userId) {
        Patient p = new Patient();
        p.setId(id);
        p.setUserId(userId);
        return p;
    }

    @Test
    void getForUserOwnPrescriptionReturnedWithItems() {
        Prescription p = new Prescription();
        p.setId(3L);
        p.setPatientId(11L);
        when(prescriptionMapper.selectById(3L)).thenReturn(p);
        when(patientMapper.selectById(11L)).thenReturn(patientOf(11L, 7L));
        when(prescriptionItemMapper.selectByPrescriptionId(3L)).thenReturn(Collections.emptyList());

        Prescription out = service.getForUser(7L, 3L);
        assertEquals(3L, out.getId());
        assertNotNull(out.getItems());
    }

    @Test
    void getForUserOthersPrescriptionRejected() {
        Prescription p = new Prescription();
        p.setId(9L);
        p.setPatientId(11L);
        when(prescriptionMapper.selectById(9L)).thenReturn(p);
        when(patientMapper.selectById(11L)).thenReturn(patientOf(11L, 8L));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.getForUser(7L, 9L));
        assertTrue(ex.getMessage().contains("无权"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void listForUserReturnsPatientsPrescriptions() {
        when(patientMapper.selectList(any())).thenReturn(List.of(patientOf(11L, 7L)));
        Prescription p = new Prescription();
        p.setId(3L);
        p.setPatientId(11L);
        when(prescriptionMapper.selectPage(any(Page.class), any())).thenAnswer(inv -> {
            Page<Prescription> page = inv.getArgument(0);
            page.setRecords(List.of(p));
            page.setTotal(1);
            return page;
        });
        when(prescriptionItemMapper.selectByPrescriptionId(3L)).thenReturn(Collections.emptyList());

        Map<String, Object> out = service.listForUser(7L, 1, 10);
        assertEquals(1, ((List<?>) out.get("list")).size());
        assertEquals(1L, out.get("total"));
    }

    @Test
    void listForUserNoPatientsReturnsEmpty() {
        when(patientMapper.selectList(any())).thenReturn(Collections.emptyList());

        Map<String, Object> out = service.listForUser(7L, 1, 10);
        assertEquals(0, ((List<?>) out.get("list")).size());
        assertEquals(0, out.get("total"));
    }
}
