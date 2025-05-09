package de.dddns.kirbylink.warp4j.model;

import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum Architecture {
  X64("x64"),
  X32("x32"),
  AARCH64("aarch64"),
  ARM("arm");

  @Getter
  private final String value;

  public static Architecture fromValue(String value) {

    var correctedValue = switch (value) {
      case "amd64" -> "x64";
      case "arm64" -> "aarch64";
      default -> value;
    };
    return Arrays.stream(Architecture.values())
        .filter(architecture -> architecture.value.equalsIgnoreCase(correctedValue))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Unsupported Architecture value: " + correctedValue));
  }
}
