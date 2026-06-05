package com.infopouch.api.common.exception;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErrorResponse {

  @JsonProperty("error_code")
  private String errorCode;

  @JsonProperty("message")
  private String message;

  @JsonProperty("timestamp")
  private LocalDateTime timestamp;

  @JsonProperty("path")
  private String path;

  @JsonProperty("status")
  private Integer status;

  @JsonProperty("trace_id")
  private String traceId;
}
