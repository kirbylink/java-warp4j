package de.dddns.kirbylink.warp4j.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PlatformTest {

  @ParameterizedTest
  @CsvSource({"linux, LINUX", "mac, MACOS", "windows, WINDOWS"})
  void testFromValue_WhenValueIsValid_ThenPlatformEnumIsReturned(String value, Platform expectedPlatform) {
    // Given

    // When
    var actualPlatform = Platform.fromValue(value);

    // Then
    assertThat(actualPlatform).isEqualTo(expectedPlatform);
  }

  @Test
  void testFromValue_WhenValueIsUnsupported_ThenIllegalArgumentExceptionIsThrown() {
    // Given
    var value = "invalidPlatform";

    var throwAbleMethod = catchThrowable(() -> {
      Platform.fromValue(value);
    });

    // Then
    assertThat(throwAbleMethod).isInstanceOf(IllegalArgumentException.class);
  }

  @ParameterizedTest
  @CsvSource({"linux, LINUX", "unix, LINUX", "mac, MACOS", "macos, MACOS", "windows, WINDOWS", "Windows 10, WINDOWS"})
  void testFromSystemPropertyOsName_WhenValueIsValid_ThenPlatformEnumIsReturned(String value, Platform expectedPlatform) {
    // Given

    // When
    var actualPlatform = Platform.fromSystemPropertyOsName(value);

    // Then
    assertThat(actualPlatform).isEqualTo(expectedPlatform);
  }

  @Test
  void testFromSystemPropertyOsName_WhenValueIsUnsupported_ThenIllegalArgumentExceptionIsThrown() {
    // Given
    var value = "invalidPlatform";

    var throwAbleMethod = catchThrowable(() -> {
      Platform.fromSystemPropertyOsName(value);
    });

    // Then
    assertThat(throwAbleMethod).isInstanceOf(IllegalArgumentException.class);
  }
}
