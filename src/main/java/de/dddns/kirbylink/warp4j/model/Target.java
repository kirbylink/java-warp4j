package de.dddns.kirbylink.warp4j.model;

import java.util.Set;
import lombok.Value;

@Value
public class Target {
  Platform platform;
  Architecture architecture;

  public static Set<Target> getAllValidTargets() {
    return Set.of(
        new Target(Platform.LINUX, Architecture.X64),
        new Target(Platform.LINUX, Architecture.AARCH64),
        new Target(Platform.MACOS, Architecture.X64),
        new Target(Platform.MACOS, Architecture.AARCH64),
        new Target(Platform.WINDOWS, Architecture.X64),
        new Target(Platform.WINDOWS, Architecture.AARCH64)
    );
  }
}
