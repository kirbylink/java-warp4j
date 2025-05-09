package de.dddns.kirbylink.warp4j.model;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class AvailableReleaseVersionTest {

  @Test
  void shouldReturnSortedAndUniqueVersions() {
    // Given
    var availableReleaseVersion = new AvailableReleaseVersion();
    availableReleaseVersion.setAvailableLtsReleases(List.of(8, 11, 17));
    availableReleaseVersion.setAvailableReleases(List.of(9, 10, 11, 12, 13, 14, 15, 16, 17));
    availableReleaseVersion.setMostRecentFeatureRelease(21);
    availableReleaseVersion.setMostRecentFeatureVersion(22);
    availableReleaseVersion.setMostRecentLts(17);
    availableReleaseVersion.setTipVersion(23);

    // When
    var result = availableReleaseVersion.getAllAvailableVersions();

    // Then
    assertThat(result).containsExactly(8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 21, 22, 23);
  }

  @Test
  void shouldReturnEmptyListWhenNoDataIsAvailable() {
    // Given
    var availableReleaseVersion = new AvailableReleaseVersion();
    availableReleaseVersion.setAvailableLtsReleases(Collections.emptyList());
    availableReleaseVersion.setAvailableReleases(Collections.emptyList());
    availableReleaseVersion.setMostRecentFeatureRelease(0);
    availableReleaseVersion.setMostRecentFeatureVersion(0);
    availableReleaseVersion.setMostRecentLts(0);
    availableReleaseVersion.setTipVersion(0);

    // When
    var result = availableReleaseVersion.getAllAvailableVersions();

    // Then
    assertThat(result).isEmpty();
  }

}
