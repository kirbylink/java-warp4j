package de.dddns.kirbylink.warp4j.utilities;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DownloadUtilitiesTest {

  DownloadUtilities downloadUtilities;

  @BeforeEach
  void setUp() {
    downloadUtilities = new DownloadUtilities();
  }

  @Test
  void testDownloadFile() throws IOException {
    // Given
    var temporaryValidJdkDirectory = Files.createTempDirectory("valid-jdk-directory");
    var mockedUrl = mock(URL.class);
    InputStream inputStream = new ByteArrayInputStream("warp-packer-binary".getBytes());
    when(mockedUrl.openStream()).thenReturn(inputStream);

    try {
      // When
      downloadUtilities.downloadFile(mockedUrl, temporaryValidJdkDirectory.resolve("warp-packer"));

      // Then
      assertThat(temporaryValidJdkDirectory.resolve("warp-packer")).exists().hasSize("warp-packer-binary".length());
    } finally {
      deleteDirectoryRecursively(temporaryValidJdkDirectory);
    }
  }

  private void deleteDirectoryRecursively(Path path) throws IOException {
    if (Files.isDirectory(path)) {
      try (var entries = Files.newDirectoryStream(path)) {
        for (Path entry : entries) {
          deleteDirectoryRecursively(entry);
        }
      }
    }
    Files.delete(path);
  }
}
