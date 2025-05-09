package de.dddns.kirbylink.warp4j.utilities;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import de.dddns.kirbylink.warp4j.model.Architecture;
import de.dddns.kirbylink.warp4j.model.Platform;
import de.dddns.kirbylink.warp4j.utilities.ProcessExecutor.ExecutionResult;

class WarpPackerTest {

  @TempDir
  Path tempDir;

  private ProcessExecutor processExecutor;
  private WarpPacker warpPacker;

  @BeforeEach
  void setUp() {
    processExecutor = mock(ProcessExecutor.class);
    warpPacker = new WarpPacker(processExecutor);
  }

  @Test
  void testWarpApplication_ShouldExecuteWarpPacker() throws IOException, InterruptedException {
    // Given
    var warpPackerPath = tempDir.resolve("warp-packer");
    var platform = Platform.LINUX;
    var architecture = Architecture.X64;
    var bundlePath = tempDir.resolve("bundle");
    var scriptName = "start.sh";
    var outputPath = tempDir.resolve("output");
    var prefix = "";
    var expectedCommand = List.of(warpPackerPath.toString(), "pack",
        "--arch", "linux-x64",
        "--input-dir", bundlePath.toString(),
        "--exec", "start.sh",
        "--unique-id",
        "--output", outputPath.toString());
    var mockResult = new ExecutionResult(0, Collections.emptyList(), Collections.emptyList());
    when(processExecutor.execute(anyList())).thenReturn(mockResult);

    // When
    warpPacker.warpApplication(warpPackerPath, platform, architecture, bundlePath, scriptName, outputPath, prefix);

    // Then
    verify(processExecutor).execute(expectedCommand);
  }

  @Test
  void testWarpApplication_WithPrefixShouldExecuteWarpPacker() throws IOException, InterruptedException {
    // Given
    var warpPackerPath = tempDir.resolve("warp-packer");
    var platform = Platform.LINUX;
    var architecture = Architecture.X64;
    var bundlePath = tempDir.resolve("bundle");
    var scriptName = "start.sh";
    var outputPath = tempDir.resolve("output");
    var prefix = "application-name";
    var expectedCommand = List.of(warpPackerPath.toString(), "pack",
        "--arch", "linux-x64",
        "--input-dir", bundlePath.toString(),
        "--exec", "start.sh",
        "--unique-id",
        "--output", outputPath.toString(),
        "--prefix", "application-name");
    var mockResult = new ExecutionResult(0, Collections.emptyList(), Collections.emptyList());
    when(processExecutor.execute(anyList())).thenReturn(mockResult);

    // When
    warpPacker.warpApplication(warpPackerPath, platform, architecture, bundlePath, scriptName, outputPath, prefix);

    // Then
    verify(processExecutor).execute(expectedCommand);
  }

  @Test
  void testWarpApplication_ShouldThrowExceptionOnFailure() throws IOException, InterruptedException {
    // Given
    var warpPackerPath = tempDir.resolve("warp-packer");
    var platform = Platform.LINUX;
    var architecture = Architecture.X64;
    var bundlePath = tempDir.resolve("bundle");
    var scriptName = "start.sh";
    var outputPath = tempDir.resolve("output");
    String prefix = null;
    var expectedCommand = List.of(warpPackerPath.toString(), "pack",
        "--arch", "linux-x64",
        "--input-dir", bundlePath.toString(),
        "--exec", "start.sh",
        "--unique-id",
        "--output", outputPath.toString());
    var mockResult = new ExecutionResult(101, List.of("Error occured"), Collections.emptyList());
    when(processExecutor.execute(anyList())).thenReturn(mockResult);

    // When
    var exception = assertThrows(RuntimeException.class, () ->
    warpPacker.warpApplication(warpPackerPath, platform, architecture, bundlePath, scriptName, outputPath, prefix));

    // Then
    assertThat(exception.getMessage()).isEqualTo("Failed to optimize runtime");
    verify(processExecutor).execute(expectedCommand);
  }
}
