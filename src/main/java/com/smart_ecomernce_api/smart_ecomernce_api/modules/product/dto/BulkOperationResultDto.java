package com.smart_ecomernce_api.smart_ecomernce_api.modules.product.dto;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulkOperationResultDto {

    private Integer totalRequested;

    private Integer successful;

    private Integer failed;

    private List<String> errors;

    private List<Long> successfulProductIds;

    private List<Long> failedProductIds;
}