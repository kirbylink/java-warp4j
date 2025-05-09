package de.dddns.kirbylink.warp4j.model;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class JavaVersionTest {

  @Nested
  @DisplayName("Tests for compare method")
  class TestsForCompareToMethod {

    @Test
    void compareTo_ShouldReturnNegative_WhenMajorIsLess() {
      // Given
      var v1 = new JavaVersion(8, 0, 0, 0);
      var v2 = new JavaVersion(11, 0, 0, 0);

      // When // Then
      assertThat(v1).isLessThan(v2);
    }

    @Test
    void compareTo_ShouldReturnPositive_WhenMajorIsGreater() {
      // Given
      var v1 = new JavaVersion(17, 0, 0, 0);
      var v2 = new JavaVersion(11, 0, 0, 0);

      // When // Then
      assertThat(v1).isGreaterThan(v2);
    }

    @Test
    void compareTo_ShouldReturnNegative_WhenMinorIsLess() {
      // Given
      var v1 = new JavaVersion(17, 0, 0, 0);
      var v2 = new JavaVersion(17, 1, 0, 0);

      // When // Then
      assertThat(v1).isLessThan(v2);
    }

    @Test
    void compareTo_ShouldReturnPositive_WhenPatchIsGreater() {
      // Given
      var v1 = new JavaVersion(17, 0, 2, 0);
      var v2 = new JavaVersion(17, 0, 1, 0);

      // When // Then
      assertThat(v1).isGreaterThan(v2);
    }

    @Test
    void compareTo_ShouldReturnNegative_WhenBuildIsLess() {
      // Given
      var v1 = new JavaVersion(17, 0, 1, 6);
      var v2 = new JavaVersion(17, 0, 1, 7);

      // When // Then
      assertThat(v1).isLessThan(v2);
    }

    @Test
    void compareTo_ShouldReturnZero_WhenAllFieldsAreEqual() {
      // Given
      var v1 = new JavaVersion(17, 0, 1, 7);
      var v2 = new JavaVersion(17, 0, 1, 7);

      // When // Then
      assertThat(v1).isEqualByComparingTo(v2);
    }
  }

  @Test
  void shouldParseJava8Version() {
    // Given
    var versionString = "1.8.0_345";

    // When
    var version = JavaVersion.parse(versionString);

    // Then
    assertThat(version.getMajor()).isEqualTo(8);
    assertThat(version.getMinor()).isZero();
    assertThat(version.getPatch()).isZero();
    assertThat(version.getBuild()).isEqualTo(345);
  }

  @Test
  void shouldParseJava9PlusVersionWithBuild() {
    // Given
    var versionString = "17.0.13+11";

    // When
    var version = JavaVersion.parse(versionString);

    // Then
    assertThat(version.getMajor()).isEqualTo(17);
    assertThat(version.getMinor()).isZero();
    assertThat(version.getPatch()).isEqualTo(13);
    assertThat(version.getBuild()).isEqualTo(11);
  }

  @Test
  void shouldParseJava9PlusVersionWithoutBuild() {
    // Given
    var versionString = "17.0.13";

    // When
    var version = JavaVersion.parse(versionString);

    // Then
    assertThat(version.getMajor()).isEqualTo(17);
    assertThat(version.getMinor()).isZero();
    assertThat(version.getPatch()).isEqualTo(13);
    assertThat(version.getBuild()).isEqualTo(-1);
  }

  @Test
  void shouldReturnCorrectStringForPreJava9() {
    // Given

    // When
    var version = new JavaVersion(8, 0, 0, 345);

    // Then
    assertThat(version).hasToString("8.0.0_345");
  }

  @Test
  void shouldReturnCorrectStringForJava9PlusWithBuild() {
    // Given

    // When
    var version = new JavaVersion(17, 0, 13, 11);

    // Then
    assertThat(version).hasToString("17.0.13+11");
  }

  @Test
  void shouldReturnCorrectStringForJava9PlusWithoutBuild() {
    // Given

    // When
    var version = new JavaVersion(17, 0, 13, -1);

    // Then
    assertThat(version).hasToString("17.0.13");
  }

  @Test
  void mapToVersionData_WithFullVersion_ReturnsFullVersionData() {
    // Given
    var versionString = "17.0.14+7";
    var javaVersion = new JavaVersion(17, 0, 14, 7);

    // When
    var actualVersionData = javaVersion.mapToVersionData();

    // Then
    assertThat(actualVersionData.getMajor()).isEqualTo(17);
    assertThat(actualVersionData.getMinor()).isZero();
    assertThat(actualVersionData.getPatch()).isEqualTo(14);
    assertThat(actualVersionData.getBuild()).isEqualTo(7);
    assertThat(actualVersionData.getSemver()).isEqualTo(versionString);
  }

  @Test
  void mapToVersionData_WithOnlyMajorVersion_ReturnsVersionDataWithOnlyMajorVersionSet() {
    // Given
    var versionString = "17.0.0+0";
    var javaVersion = new JavaVersion(17, 0, 0, 0);

    // When
    var actualVersionData = javaVersion.mapToVersionData();

    // Then
    assertThat(actualVersionData.getMajor()).isEqualTo(17);
    assertThat(actualVersionData.getMinor()).isZero();
    assertThat(actualVersionData.getPatch()).isZero();
    assertThat(actualVersionData.getBuild()).isZero();
    assertThat(actualVersionData.getSemver()).isEqualTo(versionString);
  }
}
