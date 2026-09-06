package com.attri.systemdesign.base;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public abstract class AbstractBaseController {

    @Value("${max.page.size:100}")
    protected int maxPageSize;

    protected <T> ResponseEntity<BaseResponse<T>> createResponse(BaseResponse<T> coreResponse) {
        if (coreResponse == null) {
            coreResponse = BaseResponse.build();
        }
        return ResponseEntity.status(HttpStatus.OK).body(coreResponse);
    }

    protected Pageable getPageableObject(Pageable pageable) {
        if (pageable.getPageSize() > maxPageSize) {
            pageable = PageRequest.of(pageable.getPageNumber(), maxPageSize, pageable.getSort());
        }
        return pageable;
    }


}
