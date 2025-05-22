package de.dddns.kirbylink.warp4j.utilities;

import static java.lang.String.format;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import org.openapitools.jackson.nullable.JsonNullableModule;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import de.dddns.kirbylink.warp4j.config.Warp4JConfiguration;
import de.dddns.kirbylink.warp4j.model.AvailableReleaseVersion;
import de.dddns.kirbylink.warp4j.model.ReleaseVersionsResponse;
import de.dddns.kirbylink.warp4j.model.Target;
import de.dddns.kirbylink.warp4j.model.adoptium.v3.Release;
import de.dddns.kirbylink.warp4j.model.adoptium.v3.VersionData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class AdoptiumClient {

  private final HttpClient httpClient;
  private final ObjectMapper objectMapper;

  public AdoptiumClient() {
    this(HttpClient.newHttpClient(), JsonMapper.builder().addModule(new JavaTimeModule()).addModule(new JsonNullableModule()).build());
  }

  public String getDownloadUrlForSpecificJavaVersionDataAndSystem(VersionData versionData, Target target) {
    String url;
    if(Warp4JConfiguration.isOnlyFeatureVersion(versionData)) {
      url = Warp4JConfiguration.getAdoptiumApiAssetsFeatureReleasesUrl(versionData.getMajor().toString(), target.getArchitecture().getValue(), target.getPlatform().getValue());
      log.debug("Adoptium api assets feature releases url: {}", url);
    } else {
      url = Warp4JConfiguration.getAdoptiumApiAssetsVersionUrl(versionData.getOpenjdkVersion(), target.getArchitecture().getValue(), target.getPlatform().getValue());
      log.debug("Adoptium api assets version url: {}", url);
    }
    try {
      var response = fetchReleases(url);
      var downloadUrl = response.get(0).getBinaries().get(0).getPackage().getLink();
      log.debug(downloadUrl);
      return downloadUrl;
    } catch (IOException e) {
      log.warn("No download found for target {} and platform {}", target.getArchitecture(), target.getPlatform().getValue());
    }
    return null;
  }

  public ReleaseVersionsResponse fetchLatestVersionsDataFromMajorVersion(String url) throws IOException {
    return fetchData(url, new TypeReference<ReleaseVersionsResponse>() {});
  }

  public AvailableReleaseVersion fetchAvailableReleases() throws IOException {
    return fetchData(Warp4JConfiguration.getAdoptiumApiAvailableReleasesUrl(), new TypeReference<AvailableReleaseVersion>() {});
  }

  public Release fetchRelease(String url) throws IOException {
    return fetchData(url, new TypeReference<Release>() {});
  }

  public List<Release> fetchReleases(String url) throws IOException {
    return fetchData(url, new TypeReference<List<Release>>() {});
  }

  public VersionData fetchVersionData(String javaVersion) throws IOException {
    return fetchData(Warp4JConfiguration.getAdoptiumApiVersionUrl(javaVersion), new TypeReference<VersionData>() {});
  }

  protected <T> T fetchData(String url, TypeReference<T> typeReference) throws IOException {
    var request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();

    HttpResponse<String> response;
    try {
      response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IOException(String.format("Failed to fetch data from %s. No response received.", url));
    } catch (Exception e) {
      throw new IOException(String.format("Failed to fetch data from %s. No response received.", url));
    }

    if (response.statusCode() != 200) {
      throw new IOException(String.format("Failed to fetch data from %s. Status code: %d", url, response.statusCode()));
    }

    log.debug("Response from {}: {}", url, response.body());

    try {
      return objectMapper.readValue(response.body(), typeReference);
    } catch (Exception e) {
      throw new IOException(format("Failed to parse data from %s. Malformed response.", url), e);
    }
  }
}

