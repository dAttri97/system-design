package com.attri.systemdesign.base;

import lombok.Getter;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BaseResponse<T> {

    private final String statusCode;
    private final String statusMessage;
    private final T data;
    private final Boolean first;
    private final Boolean last;
    private final Integer number;
    private final Integer size;
    private final Integer numberOfElements;
    private final Sort sort;
    private final Long totalElements;
    private final Integer totalPages;

    /**
     * Captured from the MDC (set by TraceIdFilter) when the response object is
     * built on the request thread; null for responses built outside a request.
     */
    private final String traceId = MDC.get(TraceIdFilter.TRACE_ID_MDC_KEY);

    public BaseResponse(String statusCode, String statusMessage, T data) {
        this.statusCode = statusCode;
        this.statusMessage = statusMessage;
        this.data = data;
        this.first = null;
        this.last = null;
        this.number = null;
        this.size = null;
        this.numberOfElements = null;
        this.sort = null;
        this.totalElements = null;
        this.totalPages = null;

    }

    public BaseResponse(T data) {
        this.statusCode = null;
        this.statusMessage = null;
        this.data = data;
        this.first = null;
        this.last = null;
        this.number = null;
        this.size = null;
        this.numberOfElements = null;
        this.sort = null;
        this.totalElements = null;
        this.totalPages = null;
    }

    public BaseResponse(Builder<T> builder) {
        this.statusCode = builder.statusCode;
        this.statusMessage = builder.statusMessage;
        this.data = builder.data;
        if (builder.pageInfo != null) {
            this.first = builder.pageInfo.isFirst();
            this.last = builder.pageInfo.isLast();
            this.number = builder.pageInfo.getNumber();
            this.size = builder.pageInfo.getSize();
            this.numberOfElements = builder.pageInfo.getNumberOfElements();
            this.sort = builder.pageInfo.getSort();
            this.totalElements = builder.pageInfo.getTotalElements();
            this.totalPages = builder.pageInfo.getTotalPages();
        } else {
            this.first = null;
            this.last = null;
            this.number = null;
            this.size = null;
            this.numberOfElements = null;
            this.sort = null;
            this.totalElements = null;
            this.totalPages = null;
        }

    }


    public static <B> Builder<B> builder(B data) {
        return new Builder<B>(data);
    }

    public static <B> BaseResponse<B> build() {
        B obj = null;
        return BaseResponse.builder(obj).build();
    }

    public static class Builder<B> {

        private String statusCode;
        private String statusMessage;
        private B data;
        private Page<?> pageInfo = null;


        private Builder(B data) {
            this.data = data;
            this.statusCode = String.valueOf(HttpStatus.OK.value());
            this.statusMessage = HttpStatus.OK.getReasonPhrase();
        }

        public BaseResponse<B> build() {
            return new BaseResponse<B>(this);
        }

        public Builder<B> addStatusCode(String statusCode) {
            this.statusCode = statusCode;
            return this;
        }

        public Builder<B> addStatusMessage(String statusMessage) {
            this.statusMessage = statusMessage;
            return this;
        }

        @SuppressWarnings("unchecked")
        public Builder<B> page(Pageable pageable, long totalRecords) {
            if (data instanceof Map) {
                Map<Object, Page<?>> paginatedMap = new HashMap<>();
                ((Map<?, ?>) data).forEach((key, value) -> {
                    if (value instanceof Collection) {
                        Page<?> page = new PageImpl<>((List<?>) value, pageable, totalRecords);
                        paginatedMap.put(key, page);
                    }
                });
                this.data = (B) paginatedMap;
                return this;
            } else if (data instanceof Collection) {
                this.pageInfo = new PageImpl<>((List<?>) data, pageable, totalRecords);
                return this;
            }
            return this;
        }

        public Builder<B> page(Page<?> page) {
            this.pageInfo = new PageImpl<>(page.getContent(), page.getPageable(), page.getTotalElements());
            return this;
        }
    }
}
