package com.clinic.dto;

import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Data
public class InsuranceSettlementRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long orderId;
    private Long patientId;
    private Long insurancePatientId;
    private BigDecimal totalAmount;

    private List<SettlementItem> items;

    @Data
    public static class SettlementItem implements Serializable {
        private static final long serialVersionUID = 1L;
        private Long catalogItemId;
        private String itemCode;
        private String itemName;
        private String specification;
        private String unit;
        private BigDecimal quantity;
        private BigDecimal unitPrice;
        private BigDecimal totalPrice;
    }
}
