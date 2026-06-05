package com.infopouch.api.common.util;

import java.util.UUID;

public class IdGenerator {

  public static String generate(String prefix) {
    // Generates prefix_ and a clean, 8-character compact alphanumeric string
    String shortUuid = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    return prefix + "_" + shortUuid;
  }
}
