package com.clinic.service;

import com.clinic.common.exception.BusinessException;
import com.clinic.entity.Patient;
import com.clinic.entity.Registration;
import com.clinic.mapper.DepartmentMapper;
import com.clinic.mapper.PatientMapper;
import com.clinic.mapper.RegistrationMapper;
import com.clinic.mapper.ScheduleMapper;
import com.clinic.service.impl.RegistrationServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 患者侧挂号写接口单测：幂等键防重 + 归属校验
 */
@ExtendWith(MockitoExtension.class)
class RegistrationForUserTest {

    @Mock
    RegistrationMapper registrationMapper;
    @Mock
    PatientMapper patientMapper;
    @Mock
    DepartmentMapper departmentMapper;
    @Mock
    ScheduleMapper scheduleMapper;
    @Mock
    DoctorService doctorService;

    @InjectMocks
    RegistrationServiceImpl service;

    private Patient patientOf(Long id, Long userId) {
        Patient p = new Patient();
        p.setId(id);
        p.setUserId(userId);
        return p;
    }

    @Test
    void duplicateClientRequestIdReturnsExistingWithoutInsert() {
        Registration existing = new Registration();
        existing.setId(99L);
        existing.setPatientId(5L);
        when(registrationMapper.selectOne(any())).thenReturn(existing);

        Registration reg = new Registration();
        reg.setPatientId(5L);
        Map<String, Object> out = service.createForUser(7L, reg, "agent:abc");

        assertEquals(true, out.get("duplicated"));
        assertEquals(99L, ((Registration) out.get("registration")).getId());
        verify(registrationMapper, never()).insert(any(Registration.class));
    }

    @Test
    void createForOtherUsersPatientRejected() {
        when(registrationMapper.selectOne(any())).thenReturn(null);
        when(patientMapper.selectById(5L)).thenReturn(patientOf(5L, 8L));

        Registration reg = new Registration();
        reg.setPatientId(5L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.createForUser(7L, reg, "agent:new"));
        assertTrue(ex.getMessage().contains("无权"));
        verify(registrationMapper, never()).insert(any(Registration.class));
    }

    @Test
    void cancelOthersRegistrationRejected() {
        Registration other = new Registration();
        other.setId(5L);
        other.setPatientId(11L);
        when(registrationMapper.selectById(5L)).thenReturn(other);
        when(patientMapper.selectById(11L)).thenReturn(patientOf(11L, 8L));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.cancelForUser(7L, 5L, "误约"));
        assertTrue(ex.getMessage().contains("无权"));
        verify(registrationMapper, never()).deleteById(any(Registration.class));
    }
}
