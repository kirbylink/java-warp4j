package de.dddns.kirbylink.warp4j.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ArchitectureTest {

  @ParameterizedTest
  @CsvSource({"x64, X64", "x32, X32", "aarch64, AARCH64", "arm, ARM", "amd64, X64", "arm64, AARCH64"})
  void testFromValue_WhenValueIsValid_ThenArchitectureEnumIsReturned(String value, Architecture expectedArchitecture) {
    // Given

    // When
    var actualArchitecture = Architecture.fromValue(value);

    // Then
    assertThat(actualArchitecture).isEqualTo(expectedArchitecture);
  }

  @Test
  void testFromValue_WhenValueIsUnsupported_ThenIllegalArgumentExceptionIsThrown() {
    // Given
    var value = "invalidArchitecture";

    var throwAbleMethod = catchThrowable(() -> {
      Architecture.fromValue(value);
    });

    // Then
    assertThat(throwAbleMethod).isInstanceOf(IllegalArgumentException.class);
  }
}
