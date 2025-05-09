package de.dddns.kirbylink.warp4j.model;

import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum Platform {
  LINUX("linux"),
  MACOS("mac"),
  WINDOWS("windows");

  @Getter
  private final String value;

  public static Platform fromValue(String value) {
    return Arrays.stream(Platform.values())
      .filter(platform -> platform.value.equalsIgnoreCase(value))
      .findFirst()
      .orElseThrow(() -> new IllegalArgumentException("Unsupported platform value: " + value));
  }

  public static Platform fromSystemPropertyOsName(String osName) {
    if (osName.contains("unix")) {
      return LINUX;
    }
    return Arrays.stream(Platform.values())
      .filter(platform -> osName.toLowerCase().contains(platform.value))
      .findFirst()
      .orElseThrow(() -> new IllegalArgumentException("Unsupported os name value: " + osName));
  }
}
