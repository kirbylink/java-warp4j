package de.dddns.kirbylink.warp4j.model;

import de.dddns.kirbylink.warp4j.model.adoptium.v3.VersionData;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
@EqualsAndHashCode
public class JavaVersion implements Comparable<JavaVersion> {
  private final int major;
  private final int minor;
  private final int patch;
  private final int build;

  public boolean isPreJava9() {
    return major < 9;
  }

  @Override
  public String toString() {
    if (isPreJava9()) {
      return String.format("%d.%d.%d_%d", major, minor, patch, build);
    } else if (build != -1) {
      return String.format("%d.%d.%d+%d", major, minor, patch, build);
    } else {
      return String.format("%d.%d.%d", major, minor, patch);
    }
  }

  @Override
  public int compareTo(JavaVersion otherJavaVersion) {
    var compareValue = Integer.compare(this.major, otherJavaVersion.major);
    if (compareValue != 0) {
      return compareValue;
    }
    compareValue = Integer.compare(this.minor, otherJavaVersion.minor);
    if (compareValue != 0) {
      return compareValue;
    }
    compareValue = Integer.compare(this.patch, otherJavaVersion.patch);
    if (compareValue != 0) {
      return compareValue;
    }
    return Integer.compare(this.build, otherJavaVersion.build);
  }

  public static JavaVersion parse(String version) {
    if (version.contains("+")) {
      var parts = version.split("\\+");
      var numbers = parts[0].split("\\.");
      return new JavaVersion(Integer.parseInt(numbers[0]), Integer.parseInt(numbers[1]), Integer.parseInt(numbers[2]), Integer.parseInt(parts[1]));
    } else if (version.contains("_")) {
      var parts = version.split("_");
      var numbers = parts[0].split("\\.");
      return new JavaVersion(Integer.parseInt(numbers[1]), // 1.8 -> Major = 8
          Integer.parseInt(numbers[2]), 0, Integer.parseInt(parts[1]));
    } else {
      var numbers = version.split("\\.");
      return switch (numbers.length) {
        case 1 -> new JavaVersion(Integer.parseInt(numbers[0]), 0, 0, -1);
        case 2 -> new JavaVersion(Integer.parseInt(numbers[0]), Integer.parseInt(numbers[1]), 0, -1);
        default -> new JavaVersion(Integer.parseInt(numbers[0]), Integer.parseInt(numbers[1]), Integer.parseInt(numbers[2]), -1);
        };
    }
  }

  public VersionData mapToVersionData() {
    var versionData = new VersionData();

    versionData.setMajor(major);
    versionData.setBuild(build);
    versionData.setMinor(minor);
    versionData.setPatch(patch);
    versionData.setSemver(toString());

    return versionData;
  }
}