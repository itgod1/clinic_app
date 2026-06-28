package com.clinic.common.result;

import lombok.Data;
import java.io.Serializable;
import java.util.List;

@Data
public class PageResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long total;
    private List<T> list;

    public PageResult() {}

    public PageResult(Long total, List<T> list) {
        this.total = total;
        this.list = list;
    }

    public static <T> PageResult<T> ok(List<T> list, Long total) {
        return new PageResult<>(total, list);
    }

    public static <T> PageResult<T> ok(List<T> list) {
        return new PageResult<>(list.size() > 0 ? (long) list.size() : 0L, list);
    }

    public static <T> PageResult<T> empty() {
        return new PageResult<>(0L, List.of());
    }
}