package de.dddns.kirbylink.warp4j.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import de.dddns.kirbylink.warp4j.model.Architecture;
import de.dddns.kirbylink.warp4j.model.Platform;
import de.dddns.kirbylink.warp4j.model.Target;
import de.dddns.kirbylink.warp4j.utilities.FileUtilities;
import de.dddns.kirbylink.warp4j.utilities.WarpPacker;

class WarpServiceTest {

  private WarpPacker mockWarpPacker;
  private WarpService warpService;
  private MockedStatic<FileUtilities> mockedFileUtilities;
  private MockedStatic<Files> mockedFiles;
  private Path bundleDir;
  private Path scriptPath;
  private Path outputPath;
  private Path warpPackerPath;

  @BeforeEach
  void setUp() {
    mockWarpPacker = mock(WarpPacker.class);
    warpService = new WarpService(mockWarpPacker);
    mockedFileUtilities = mockStatic(FileUtilities.class);
    mockedFiles = mockStatic(Files.class);
    bundleDir = mock(Path.class);
    scriptPath = mock(Path.class);
    when(scriptPath.getFileName()).thenReturn(scriptPath);
    when(scriptPath.toString()).thenReturn("start.sh");
    outputPath = mock(Path.class);
    warpPackerPath = mock(Path.class);
  }

  @AfterEach
  void tearDown() {
    mockedFileUtilities.close();
    mockedFiles.close();
  }

  @Test
  void testWarpBundle_WhenNoException_ThenReturnsTrue() throws Exception {
    // Given
    var target = new Target(Platform.LINUX, Architecture.X64);
    var prefix = "warp4j";

    // When
    var result = warpService.warpBundle(target, bundleDir, scriptPath, outputPath, warpPackerPath, prefix);

    // Then
    assertThat(result).isTrue();
    mockedFileUtilities.verify(() -> FileUtilities.deleteRecursively(outputPath));
    verify(mockWarpPacker).warpApplication(warpPackerPath, target, bundleDir, "start.sh", outputPath, prefix);
  }

  @Test
  void testWarpBundle_WhenIOException_ThenReturnsFalse() {
    // Given
    var target = new Target(Platform.LINUX, Architecture.X64);
    var prefix = "warp4j";

    mockedFileUtilities.when(() -> FileUtilities.deleteRecursively(outputPath)).thenThrow(new IOException("Delete failed"));

    // When
    var result = warpService.warpBundle(target, bundleDir, scriptPath, outputPath, warpPackerPath, prefix);

    // Then
    assertThat(result).isFalse();
    mockedFileUtilities.verify(() -> FileUtilities.deleteRecursively(outputPath));
    verifyNoInteractions(mockWarpPacker);
  }

  @Test
  void testWarpBundle_WhenInterruptedException_ThenReturnsFalseAndInterruptsThread() throws Exception {
    // Given
    var target = new Target(Platform.LINUX, Architecture.X64);
    var prefix = "warp4j";

    doThrow(new InterruptedException("Thread interrupted")).when(mockWarpPacker).warpApplication(any(), any(), any(), any(), any(), any());

    // When
    var result = warpService.warpBundle(target, bundleDir, scriptPath, outputPath, warpPackerPath, prefix);

    // Then
    assertThat(result).isFalse();
    verify(mockWarpPacker).warpApplication(any(), any(), any(), any(), any(), any());
    assertThat(Thread.currentThread().isInterrupted()).isTrue();
  }
}

