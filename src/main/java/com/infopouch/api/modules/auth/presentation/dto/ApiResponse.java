package com.infopouch.api.modules.auth.presentation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ApiResponse<T> {

  private boolean success;
  private String message;
  private T data;

  /** Success Response with Data */
  public static <T> ApiResponse<T> success(String message, T data) {
    return new ApiResponse<>(true, message, data);
  }

  /** Success Response without Data */
  public static <T> ApiResponse<T> success(String message) {
    return new ApiResponse<>(true, message, null);
  }

  /** Error Response without Data */
  public static <T> ApiResponse<T> error(String message) {
    return new ApiResponse<>(false, message, null);
  }

  /** Error Response with Data */
  public static <T> ApiResponse<T> error(String message, T data) {
    return new ApiResponse<>(false, message, data);
  }
}
