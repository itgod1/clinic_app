package com.clinic.service;

import com.clinic.entity.InsurancePatient;
import java.util.Map;

public interface InsurancePatientService {

    Map<String, Object> list(Long clinicId, String keyword, String insuranceType, String insuranceStatus, Integer pageNum, Integer pageSize);

    InsurancePatient getById(Long id);

    InsurancePatient getByPatientId(Long patientId);

    void create(InsurancePatient patient);

    void update(InsurancePatient patient);

    void delete(Long id);

    void validateUnique(InsurancePatient patient);
}
