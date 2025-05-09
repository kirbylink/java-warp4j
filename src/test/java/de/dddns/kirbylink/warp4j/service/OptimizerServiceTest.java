package de.dddns.kirbylink.warp4j.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import de.dddns.kirbylink.warp4j.config.Warp4JCommand.Warp4JCommandConfiguration;
import de.dddns.kirbylink.warp4j.model.Architecture;
import de.dddns.kirbylink.warp4j.model.Platform;
import de.dddns.kirbylink.warp4j.model.adoptium.v3.VersionData;
import de.dddns.kirbylink.warp4j.utilities.FileUtilities;
import de.dddns.kirbylink.warp4j.utilities.JLinkOptimizer;
import de.dddns.kirbylink.warp4j.utilities.JdepAnalyzer;

class OptimizerServiceTest {

  private OptimizerService optimizerService;

  private JdepAnalyzer mockedJdepAnalyzer;
  private JLinkOptimizer mockedJLinkOptimizer;
  private Warp4JCommandConfiguration mockedConfig;

  private static MockedStatic<Files> mockedFiles;
  private static MockedStatic<FileUtilities> mockedFileUtilities;

  @BeforeEach
  void setUp() {
    mockedJdepAnalyzer = mock(JdepAnalyzer.class);
    mockedJLinkOptimizer = mock(JLinkOptimizer.class);
    mockedConfig = mock(Warp4JCommandConfiguration.class);
    optimizerService = new OptimizerService(mockedJdepAnalyzer, mockedJLinkOptimizer, mockedConfig);

    mockedFiles = mockStatic(Files.class);
    mockedFileUtilities = mockStatic(FileUtilities.class);
  }

  @AfterEach
  void tearDown() {
    mockedFiles.close();
    mockedFileUtilities.close();
  }

  @Test
  void testGetClassPath_WhenWithSpringBootAndExtraction_ThenClassPathWillBeReturned() {
    // Given
    var mockedJarPath = mock(Path.class);
    var mockedAppDataPath = mock(Path.class);
    var mockedExtractedPath = mock(Path.class);
    var mockedLibPath = mock(Path.class);
    var mockedJar1 = mock(Path.class);
    var mockedJar2 = mock(Path.class);

    when(mockedConfig.getClassPath()).thenReturn(List.of());
    when(mockedConfig.isSpringBoot()).thenReturn(true);
    when(mockedJarPath.getFileName()).thenReturn(Path.of("mocked.jar"));
    when(mockedAppDataPath.resolve("extracted-jar")).thenReturn(mockedExtractedPath);
    when(mockedExtractedPath.resolve("BOOT-INF")).thenReturn(mockedExtractedPath);
    when(mockedExtractedPath.resolve("lib")).thenReturn(mockedLibPath);
    mockedFileUtilities.when(() -> FileUtilities.extractZip(mockedJarPath, mockedExtractedPath)).then(inv -> null);
    mockedFiles.when(() -> Files.list(mockedLibPath)).thenReturn(Stream.of(mockedJar1, mockedJar2));
    when(mockedJar1.toString()).thenReturn("lib1.jar");
    when(mockedJar2.toString()).thenReturn("lib2.jar");
    when(mockedJar1.toAbsolutePath()).thenReturn(mockedJar1);
    when(mockedJar2.toAbsolutePath()).thenReturn(mockedJar2);

    // When
    var classPath = optimizerService.getClassPath(mockedJarPath, mockedAppDataPath);

    // Then
    assertThat(classPath).containsExactly(mockedJar1, mockedJar2);
  }

  @Test
  void testGetClassPath_WhenWithManualClassPath_ThenClassPathWillBeReturned() {
    // Given
    var mockedManualPath = mock(Path.class);
    var mockedFile1 = mock(Path.class);
    when(mockedManualPath.toString()).thenReturn("my-lib.jar");
    when(mockedManualPath.getFileName()).thenReturn(Path.of("my-lib.jar"));
    when(mockedConfig.getClassPath()).thenReturn(List.of(mockedManualPath));
    when(mockedConfig.isSpringBoot()).thenReturn(false);
    mockedFiles.when(() -> Files.walk(mockedManualPath)).thenReturn(Stream.of(mockedFile1));
    mockedFiles.when(() -> Files.isRegularFile(mockedFile1)).thenReturn(true);
    when(mockedFile1.getFileName()).thenReturn(Path.of("file.class"));
    when(mockedFile1.toAbsolutePath()).thenReturn(mockedFile1);

    // When
    var classPath = optimizerService.getClassPath(Path.of("dummy.jar"), Path.of("/tmp"));

    // Then
    assertThat(classPath).containsExactly(mockedFile1);
  }

  @Test
  void testAnalyzeModules_WhenSuccessful_ThenModulesWillBeReturned() throws IOException, InterruptedException {
    // Given
    var mockedJdepsPath = Path.of("jdeps");
    var mockedJarPath = Path.of("app.jar");
    var versionData = new VersionData();
    versionData.setMajor(17);
    List<Path> mockedClassPath = List.of(Path.of("lib1.jar"));

    when(mockedJdepAnalyzer.analyzeDependencies("jdeps", mockedClassPath, mockedJarPath, "17")).thenReturn(List.of("java.base", "java.logging"));
    when(mockedConfig.getAdditionalModules()).thenReturn("java.xml");

    // When
    var result = optimizerService.analyzeModules(mockedJdepsPath, mockedClassPath, mockedJarPath, versionData);

    // Then
    assertThat(result).isEqualTo("java.base,java.logging,java.xml");
  }

  @Test
  void testAnalyzeModules_WhenIOException_ThenReturnEmpty() throws IOException, InterruptedException {
    // Given
    when(mockedJdepAnalyzer.analyzeDependencies(anyString(), any(), any(), any())).thenThrow(new IOException("Fail"));
    when(mockedConfig.getAdditionalModules()).thenReturn("");

    // When
    var result = optimizerService.analyzeModules(Path.of("jdeps"), List.of(), Path.of("app.jar"), new VersionData());

    // Then
    assertThat(result).isEmpty();
  }

  @Test
  void testAnalyzeModules_WhenInterruptedException_ThenReturnEmptyAndInterrupt() throws IOException, InterruptedException {
    // Given
    Thread.interrupted(); // Clear interrupted status
    when(mockedJdepAnalyzer.analyzeDependencies(anyString(), any(), any(), any())).thenThrow(new InterruptedException("interrupted"));

    // When
    var result = optimizerService.analyzeModules(Path.of("jdeps"), List.of(), Path.of("app.jar"), new VersionData());

    // Then
    assertThat(result).isEmpty();
    assertThat(Thread.interrupted()).isTrue(); // Should be re-set
  }

  @Test
  void testCreateOptimizedRuntime_WhenSuccess() throws IOException, InterruptedException {
    // Given
    var platform = Platform.LINUX;
    var arch = Architecture.X64;
    var mockedOutput = mock(Path.class);
    when(mockedJLinkOptimizer.createOptimizedRuntime(eq(platform), eq(arch), any(), any(), any(), any(), any())).thenReturn(mockedOutput);

    // When
    var result = optimizerService.createOptimizedRuntime(platform, arch, mock(Path.class), new VersionData(), mock(Path.class), mock(Path.class), "java.base");

    // Then
    assertThat(result).isEqualTo(mockedOutput);
  }

  @Test
  void testCreateOptimizedRuntime_WhenIOException_ThenReturnNull() throws IOException, InterruptedException {
    // Given
    when(mockedJLinkOptimizer.createOptimizedRuntime(any(), any(), any(), any(), any(), any(), any())).thenThrow(new IOException("boom"));

    // When
    var result = optimizerService.createOptimizedRuntime(Platform.MACOS, Architecture.X64, mock(Path.class), new VersionData(), mock(Path.class), mock(Path.class), "mod");

    // Then
    assertThat(result).isNull();
  }

  @Test
  void testCreateOptimizedRuntime_WhenInterruptedException_ThenReturnNullAndInterrupt() throws IOException, InterruptedException {
    // Given
    Thread.interrupted(); // clear first
    when(mockedJLinkOptimizer.createOptimizedRuntime(any(), any(), any(), any(), any(), any(), any())).thenThrow(new InterruptedException("boom"));

    // When
    var result = optimizerService.createOptimizedRuntime(Platform.WINDOWS, Architecture.X64, mock(Path.class), new VersionData(), mock(Path.class), mock(Path.class), "mod");

    // Then
    assertThat(result).isNull();
    assertThat(Thread.interrupted()).isTrue();
  }
}

