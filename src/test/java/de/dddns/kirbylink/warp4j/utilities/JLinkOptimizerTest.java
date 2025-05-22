package de.dddns.kirbylink.warp4j.utilities;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import de.dddns.kirbylink.warp4j.model.Architecture;
import de.dddns.kirbylink.warp4j.model.Platform;
import de.dddns.kirbylink.warp4j.model.Target;
import de.dddns.kirbylink.warp4j.model.adoptium.v3.VersionData;

class JLinkOptimizerTest {

  private ProcessExecutor processExecutor;

  private JLinkOptimizer jLinkOptimizer;

  @BeforeEach
  void setUp() {
    processExecutor = mock(ProcessExecutor.class);
    jLinkOptimizer = new JLinkOptimizer(processExecutor);
  }

  @Test
  void shouldCreateOptimizedRuntime() throws IOException, InterruptedException {
    // GIven
    var platform = Platform.MACOS;
    var architecture = Architecture.X64;
    var target = new Target(platform, architecture);
    var applicationDataDirectory = Path.of("/tmp/test-app");
    var versionData = new VersionData();
    versionData.setMajor(17);
    versionData.setSecurity(2);
    var jlinkPath = mock(Path.class);
    when(jlinkPath.toString()).thenReturn("/usr/bin/jlink");
    var modules = "java.base,java.logging";

    var jmodsPath = applicationDataDirectory.resolve("jdk")
        .resolve(platform.getValue())
        .resolve(architecture.getValue())
        .resolve("jdk-" + versionData.getSemver());

    var stripDebug = (versionData.getMajor() >= 13) ? "strip-java-debug-attributes" : "strip-debug";

    var outputPath = applicationDataDirectory.resolve("bundle")
        .resolve(platform.getValue())
        .resolve(architecture.getValue())
        .resolve("java");

    List<String> expectedCommand = List.of(
        "/usr/bin/jlink", "--no-header-files", "--no-man-pages", "--" + stripDebug,
        "--module-path", jmodsPath.toString() + "/Contents/Home/jmods", "--add-modules", modules,
        "--output", outputPath.toString()
    );

    var successResult = new ProcessExecutor.ExecutionResult(0, List.of("JLink completed successfully"), Collections.emptyList());

    when(processExecutor.execute(expectedCommand)).thenReturn(successResult);

    // When
    jLinkOptimizer.createOptimizedRuntime(target, applicationDataDirectory, jmodsPath, versionData, jlinkPath, modules);

    // Then
    verify(processExecutor).execute(expectedCommand);
  }

  @Test
  void shouldThrowExceptionOnFailure() throws IOException, InterruptedException {
    // Given
    var platform = Platform.WINDOWS;
    var architecture = Architecture.AARCH64;
    var target = new Target(platform, architecture);
    var applicationDataDirectory = Path.of("/tmp/test-app");
    var versionData = new VersionData();
    versionData.setMajor(11);
    versionData.setSecurity(15);
    var jlinkPath = mock(Path.class);
    when(jlinkPath.toString()).thenReturn("C:\\jdk\\bin\\jlink.exe");
    var modules = "java.base,java.xml";

    var jmodsPath = applicationDataDirectory.resolve("jdk")
        .resolve(platform.getValue())
        .resolve(architecture.getValue())
        .resolve("jdk-" + versionData.getSemver());

    var stripDebug = (versionData.getMajor() >= 13) ? "strip-java-debug-attributes" : "strip-debug";

    var outputPath = applicationDataDirectory.resolve("bundle")
        .resolve(platform.getValue())
        .resolve(architecture.getValue())
        .resolve("java");

    List<String> expectedCommand = List.of(
        "C:\\jdk\\bin\\jlink.exe", "--no-header-files", "--no-man-pages", "--" + stripDebug,
        "--module-path", jmodsPath.toString() + "/jmods", "--add-modules", modules,
        "--output", outputPath.toString()
    );

    var failureResult = new ProcessExecutor.ExecutionResult(1, List.of("Error: JLink failed"), Collections.emptyList());
    when(processExecutor.execute(expectedCommand)).thenReturn(failureResult);

    // When
    var throwAbleMethod = catchThrowable(() -> {
      jLinkOptimizer.createOptimizedRuntime(target, applicationDataDirectory, jmodsPath, versionData, jlinkPath, modules);
    });

    // Then
    assertThat(throwAbleMethod.getMessage()).isEqualTo("Failed to optimize runtime");
    verify(processExecutor).execute(expectedCommand);
  }
}

