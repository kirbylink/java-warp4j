package de.dddns.kirbylink.warp4j.model;

import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import jakarta.annotation.Nonnull;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@JsonPropertyOrder({
  AvailableReleaseVersion.AVAILABLE_LTS_RELEASES,
  AvailableReleaseVersion.AVAILABLE_RELEASES,
  AvailableReleaseVersion.MOST_RECENT_FEATURE_RELEASE,
  AvailableReleaseVersion.MOST_RECENT_FEATURE_VERSION,
  AvailableReleaseVersion.MOST_RECENT_LTS,
  AvailableReleaseVersion.TIP_VERSION
})
public class AvailableReleaseVersion {
  public static final String AVAILABLE_LTS_RELEASES = "available_lts_releases";
  public static final String AVAILABLE_RELEASES = "available_releases";
  public static final String MOST_RECENT_FEATURE_RELEASE = "most_recent_feature_release";
  public static final String MOST_RECENT_FEATURE_VERSION = "most_recent_feature_version";
  public static final String MOST_RECENT_LTS = "most_recent_lts";
  public static final String TIP_VERSION = "tip_version";

  @Nonnull
  @JsonProperty(AVAILABLE_LTS_RELEASES)
  @JsonInclude(value = JsonInclude.Include.ALWAYS)
  private List<Integer> availableLtsReleases;

  @Nonnull
  @JsonProperty(AVAILABLE_RELEASES)
  @JsonInclude(value = JsonInclude.Include.ALWAYS)
  private List<Integer> availableReleases;

  @Nonnull
  @JsonProperty(MOST_RECENT_FEATURE_RELEASE)
  @JsonInclude(value = JsonInclude.Include.ALWAYS)
  private int mostRecentFeatureRelease;

  @Nonnull
  @JsonProperty(MOST_RECENT_FEATURE_VERSION)
  @JsonInclude(value = JsonInclude.Include.ALWAYS)
  private int mostRecentFeatureVersion;

  @Nonnull
  @JsonProperty(MOST_RECENT_LTS)
  @JsonInclude(value = JsonInclude.Include.ALWAYS)
  private int mostRecentLts;

  @Nonnull
  @JsonProperty(TIP_VERSION)
  @JsonInclude(value = JsonInclude.Include.ALWAYS)
  private int tipVersion;

  public List<Integer> getAllAvailableVersions() {
    return Stream.of(availableLtsReleases, availableReleases, List.of(mostRecentFeatureRelease, mostRecentFeatureVersion, mostRecentLts, tipVersion))
        .flatMap(List::stream)
        .filter(version -> version != 0)
        .collect(Collectors.toCollection(TreeSet::new))
        .stream()
        .toList();
  }
}

