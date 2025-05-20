package de.dddns.kirbylink.warp4j.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.naming.NoPermissionException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import de.dddns.kirbylink.warp4j.config.Warp4JCommand.Warp4JCommandConfiguration;
import de.dddns.kirbylink.warp4j.config.Warp4JConfiguration;
import de.dddns.kirbylink.warp4j.model.Architecture;
import de.dddns.kirbylink.warp4j.model.JdkProcessingState;
import de.dddns.kirbylink.warp4j.model.Platform;
import de.dddns.kirbylink.warp4j.model.Target;
import de.dddns.kirbylink.warp4j.model.adoptium.v3.VersionData;
import de.dddns.kirbylink.warp4j.utilities.FileUtilities;

class Warp4JServiceTest {

  private static MockedStatic<Warp4JConfiguration> mockedWarp4JConfiguration;
  private static MockedStatic<Files> mockedFiles;
  private static MockedStatic<FileUtilities> mockedFileUtilities;

  private DownloadService mockedDownloadService;
  private CachedJdkCollectorService mockedCachedJdkCollectorService;
  private FileService mockedFileService;
  private OptimizerService mockedOptimizerService;
  private WarpService mockedWarpService;

  private Warp4JCommandConfiguration mockedWarp4jCommandConfiguration;
  private Path mockedApplicationDataDirectoryPath;
  private Path mockedWarpPackerPath;
  private VersionData mockedVersionData;
  private Path mockedJarFilePath;
  private Path mockedBundleDirectoryPath;
  private Path mockedBundleScriptPath;
  private Path mockedOutputDirectoryPath;

  private List<JdkProcessingState> jdkProcessingStates;

  private Warp4JService warp4JService;

  @BeforeEach
  void setUp() {
    mockedWarp4JConfiguration = mockStatic(Warp4JConfiguration.class);
    mockedFiles = mockStatic(Files.class);
    mockedFileUtilities = mockStatic(FileUtilities.class);

    mockedDownloadService = mock(DownloadService.class);
    mockedCachedJdkCollectorService = mock(CachedJdkCollectorService.class);
    mockedFileService = mock(FileService.class);
    mockedOptimizerService = mock(OptimizerService.class);
    mockedWarpService = mock(WarpService.class);

    mockedWarp4jCommandConfiguration = mock(Warp4JCommandConfiguration.class);
    mockedApplicationDataDirectoryPath = mock(Path.class);
    mockedWarpPackerPath = mock(Path.class);
    mockedVersionData = mock(VersionData.class);
    mockedJarFilePath = mock(Path.class);
    when(mockedWarp4jCommandConfiguration.getJarFilePath()).thenReturn(mockedJarFilePath);
    var mockedJarFileNamePath = mock(Path.class);
    when(mockedJarFilePath.getFileName()).thenReturn(mockedJarFileNamePath);
    when(mockedJarFileNamePath.toString()).thenReturn("jarFile.jar");
    mockedBundleDirectoryPath = mock(Path.class);
    mockedBundleScriptPath = mock(Path.class);
    mockedOutputDirectoryPath = mock(Path.class);
    when(mockedWarp4jCommandConfiguration.getOutputDirectoryPath()).thenReturn(mockedOutputDirectoryPath);
    when(mockedOutputDirectoryPath.resolve(anyString())).thenReturn(mockedOutputDirectoryPath);
    mockedFileUtilities.when(() -> FileUtilities.resolveOptionalWildCardAndFindFirstMatch(mockedJarFilePath)).thenReturn(mockedJarFilePath);

    warp4JService = new Warp4JService(mockedDownloadService, mockedCachedJdkCollectorService, mockedFileService, mockedOptimizerService, mockedWarpService);
  }

  @AfterEach
  void tearDown() {
    mockedWarp4JConfiguration.close();
    mockedFiles.close();
    mockedFileUtilities.close();
  }

  @Nested
  @DisplayName("Tests for setup")
  class Warp4JSetupTests {

    @Test
    void testCreateExecutableJarFile_WhenJarFileNotExists_ThenFileNotFoundExceptionIsThrown() {
      // Given
      when(mockedJarFilePath.toString()).thenReturn("path/to/jarFile.jar");
      mockedFiles.when(() -> Files.notExists(mockedJarFilePath)).thenReturn(true);

      // When
      var throwAbleMethod = catchThrowable(() -> {
        warp4JService.createExecutableJarFile(mockedWarp4jCommandConfiguration);
      });

      // Then
      assertThat(throwAbleMethod).isInstanceOf(FileNotFoundException.class).hasMessage("No Jar file found at path/to/jarFile.jar");
    }

    @Test
    void testCreateExecutableJarFile_WhenPlatformOrArchitectureIsNotSupportedByWarp_ThenUnsupportedOperationExceptionIsThrown() {
      // Given
      var target = new Target(Platform.WINDOWS, Architecture.X64);
      mockedWarp4JConfiguration.when(Warp4JConfiguration::getArchitecture).thenReturn("x64");
      mockedWarp4JConfiguration.when(Warp4JConfiguration::getOsName).thenReturn("Windows 10");
      mockedWarp4JConfiguration.when(() -> Warp4JConfiguration.supportedPlatformAndArchitectureByWarp(target)).thenReturn(false);

      // When
      var throwAbleMethod = catchThrowable(() -> {
        warp4JService.createExecutableJarFile(mockedWarp4jCommandConfiguration);
      });

      // Then
      assertThat(throwAbleMethod).isInstanceOf(UnsupportedOperationException.class).hasMessage("Warp-Packer does not support current architecture X64 and platform WINDOWS");
    }

    @Test
    void testCreateExecutableJarFile_WhenWarpPackerIsNotAvailable_ThenFileNotFoundExceptionIsThrown() throws NoPermissionException, IOException {
      // Given
      var target = new Target(Platform.WINDOWS, Architecture.X64);
      mockedWarp4JConfiguration.when(Warp4JConfiguration::getArchitecture).thenReturn("x64");
      mockedWarp4JConfiguration.when(Warp4JConfiguration::getOsName).thenReturn("Windows 10");
      mockedWarp4JConfiguration.when(() -> Warp4JConfiguration.supportedPlatformAndArchitectureByWarp(target)).thenReturn(true);
      mockedWarp4JConfiguration.when(Warp4JConfiguration::initializeApplicationDataDirectory).thenReturn(mockedApplicationDataDirectoryPath);

      when(mockedWarpPackerPath.toString()).thenReturn("/path/to/application/data/warp/warp-packer.exe");
      mockedWarp4JConfiguration.when(() -> Warp4JConfiguration.getWarpPackerPath(mockedApplicationDataDirectoryPath)).thenReturn(mockedWarpPackerPath);
      doThrow(new IOException("Can not connect to github.com")).when(mockedDownloadService).downloadWarpPackerIfNeeded(mockedWarpPackerPath, target);

      // When
      var throwAbleMethod = catchThrowable(() -> {
        warp4JService.createExecutableJarFile(mockedWarp4jCommandConfiguration);
      });

      // Then
      assertThat(throwAbleMethod).isInstanceOf(FileNotFoundException.class).hasMessage("Warp-Packer does not exist and can not be downloaded. Internet connection needs to be checked or Warp-Packer must be available at /path/to/application/data/warp/warp-packer.exe");
    }

    @Test
    void testCreateExecutableJarFile_WhenTargetArchitectureAndNoPlatformNotEqualToCurrentSystem_ThenCurrentSystemWillBeAddedAsJdkProcessingState() throws IOException, NoPermissionException, InterruptedException {
      // Given
      var platform = Platform.WINDOWS;
      var architecture = Architecture.X64;
      var target = new Target(platform, architecture);
      mockInitialization("x64", "Windows 10", target);

      jdkProcessingStates = new ArrayList<>();
      var anotherTarget = new Target(Platform.LINUX, Architecture.X64);
      var jdkProcessingState = JdkProcessingState.builder().target(anotherTarget).build();
      jdkProcessingStates.add(jdkProcessingState);
      when(mockedCachedJdkCollectorService.collectCachedJdkStates(any(), any(), any())).thenReturn(jdkProcessingStates);
      when(mockedWarp4jCommandConfiguration.isOptimize()).thenReturn(true);
      mockedWarp4JConfiguration.when(() -> Warp4JConfiguration.isSupportedTarget(target)).thenReturn(true);
      var expectedJdkProcessingState = JdkProcessingState.builder().target(target).build();
      when(mockedCachedJdkCollectorService.collectCachedJdkState(mockedApplicationDataDirectoryPath, target, mockedVersionData)).thenReturn(expectedJdkProcessingState );

      when(mockedDownloadService.downloadJdk(any(), any(), any())).thenReturn(false);

      // When
      var actualReturnValue = warp4JService.createExecutableJarFile(mockedWarp4jCommandConfiguration);

      // Then
      assertThat(actualReturnValue).isZero();
      assertThat(jdkProcessingStates)
        .hasSize(2)
        .contains(expectedJdkProcessingState);
    }
  }

  @Nested
  @DisplayName("Tests for JDK preparation")
  class TestsForJdkPreparation {

    @Test
    void testCreateExecutableJarFile_WhenJdkIsNotAvailable_ThenJdkWillBeDownloaded() throws IOException, NoPermissionException, InterruptedException {
      // Given
      var platform = Platform.WINDOWS;
      var architecture = Architecture.X64;
      var target = new Target(platform, architecture);
      mockInitialization("x64", "Windows 10", target);
      var jdkProcessingState = JdkProcessingState.builder().target(target).build();
      mockCollectionOfCachedFiles(jdkProcessingState);

      when(mockedDownloadService.downloadJdk(target, mockedVersionData, mockedApplicationDataDirectoryPath)).thenReturn(true);

      // When
      var actualReturnValue = warp4JService.createExecutableJarFile(mockedWarp4jCommandConfiguration);

      // Then
      assertThat(actualReturnValue).isZero();
      verify(mockedDownloadService).downloadJdk(target, mockedVersionData, mockedApplicationDataDirectoryPath);
    }

    @Test
    void testCreateExecutableJarFile_WhenJdkIsAvailable_ThenJdkWillNotBeDownloaded() throws IOException, NoPermissionException, InterruptedException {
      // Given
      var platform = Platform.WINDOWS;
      var architecture = Architecture.X64;
      var target = new Target(platform, architecture);
      mockInitialization("x64", "Windows 10", target);
      var jdkProcessingState = JdkProcessingState.builder().target(target).downloaded(true).build();
      mockCollectionOfCachedFiles(jdkProcessingState);

      // When
      var actualReturnValue = warp4JService.createExecutableJarFile(mockedWarp4jCommandConfiguration);

      // Then
      assertThat(actualReturnValue).isZero();
      verify(mockedDownloadService, never()).downloadJdk(any(), any(), any());
    }

    @Test
    void testCreateExecutableJarFile_WhenJdkIsAvailableButWithPullOption_ThenJdkWillBeDownloaded() throws IOException, NoPermissionException, InterruptedException {
      // Given
      var platform = Platform.WINDOWS;
      var architecture = Architecture.X64;
      var target = new Target(platform, architecture);
      mockInitialization("x64", "Windows 10", target);
      var jdkProcessingState = JdkProcessingState.builder().target(target).downloaded(true).build();
      mockCollectionOfCachedFiles(jdkProcessingState);

      when(mockedWarp4jCommandConfiguration.isPull()).thenReturn(true);

      // When
      var actualReturnValue = warp4JService.createExecutableJarFile(mockedWarp4jCommandConfiguration);

      // Then
      assertThat(actualReturnValue).isZero();
      verify(mockedDownloadService).downloadJdk(target, mockedVersionData, mockedApplicationDataDirectoryPath);
    }

    @Test
    void testCreateExecutableJarFile_WhenJdkIsAvailable_ThenJdkWillBeExtracted() throws IOException, NoPermissionException, InterruptedException {
      // Given
      var platform = Platform.WINDOWS;
      var architecture = Architecture.X64;
      var target = new Target(platform, architecture);
      mockInitialization("x64", "Windows 10", target);
      var jdkProcessingState = JdkProcessingState.builder().target(target).downloaded(true).build();
      mockCollectionOfCachedFiles(jdkProcessingState);

      var mockedExtractedPath = mock(Path.class);
      when(mockedFileService.extractJdkAndDeleteCompressedFile(target, mockedVersionData, mockedApplicationDataDirectoryPath)).thenReturn(mockedExtractedPath);

      // When
      var actualReturnValue = warp4JService.createExecutableJarFile(mockedWarp4jCommandConfiguration);

      // Then
      assertThat(actualReturnValue).isZero();
      verify(mockedFileService).extractJdkAndDeleteCompressedFile(target, mockedVersionData, mockedApplicationDataDirectoryPath);
    }

    @Test
    void testCreateExecutableJarFile_WhenJdkIsExtracted_ThenJdkWillNotBeExtracted() throws IOException, NoPermissionException, InterruptedException {
      // Given
      var platform = Platform.WINDOWS;
      var architecture = Architecture.X64;
      var target = new Target(platform, architecture);
      mockInitialization("x64", "Windows 10", target);
      var mockedExtractedPath = mock(Path.class);
      var jdkProcessingState = JdkProcessingState.builder().target(target).extracted(true).extractedJdkPath(mockedExtractedPath).build();
      mockCollectionOfCachedFiles(jdkProcessingState);


      // When
      var actualReturnValue = warp4JService.createExecutableJarFile(mockedWarp4jCommandConfiguration);

      // Then
      assertThat(actualReturnValue).isZero();
      verify(mockedFileService, never()).extractJdkAndDeleteCompressedFile(any(), any(), any());
    }
  }

  @Nested
  @DisplayName("Tests for optimization")
  class TestsForOptimization {

    @Test
    void testCreateExecutableJarFile_WhenOptimizationButWithJavaVersion8_ThenOptimizationWillBeSkipped() throws IOException, NoPermissionException, InterruptedException {
      // Given
      var platform = Platform.WINDOWS;
      var architecture = Architecture.X64;
      var target = new Target(platform, architecture);
      mockUntilJdkPreparation("x64", "Windows 10", target);

      when(mockedVersionData.getMajor()).thenReturn(8);

      // When
      var actualReturnValue = warp4JService.createExecutableJarFile(mockedWarp4jCommandConfiguration);

      // Then
      assertThat(actualReturnValue).isZero();
      verifyNoInteractions(mockedOptimizerService);
    }

    @Test
    void testCreateExecutableJarFile_WhenOptimizationButWithoutJdkForCurrentSystem_ThenOptimizationWillBeSkipped() throws IOException, NoPermissionException, InterruptedException {
      // Given
      var platform = Platform.WINDOWS;
      var architecture = Architecture.X64;
      var target = new Target(platform, architecture);
      mockUntilJdkPreparation("x64", "Windows 10", target);
      var mockedExtractedPath = mock(Path.class);
      var differentTarget = new Target(Platform.LINUX, architecture);
      var jdkProcessingStateTarget = JdkProcessingState.builder().target(differentTarget).isTarget(true).extracted(true).extractedJdkPath(mockedExtractedPath).build();
      mockCollectionOfCachedFiles(jdkProcessingStateTarget);

      when(mockedWarp4jCommandConfiguration.isLinux()).thenReturn(true);
      when(mockedWarp4jCommandConfiguration.isOptimize()).thenReturn(true);
      when(mockedVersionData.getMajor()).thenReturn(17);

      mockedWarp4JConfiguration.when(() -> Warp4JConfiguration.isSupportedTarget(target)).thenReturn(true);
      var jdkProcessingStateCurrentSystem = JdkProcessingState.builder().target(target).isTarget(false).build();
      when(mockedCachedJdkCollectorService.collectCachedJdkState(mockedApplicationDataDirectoryPath, target, mockedVersionData)).thenReturn(jdkProcessingStateCurrentSystem);

      when(mockedDownloadService.downloadJdk(target, mockedVersionData, mockedApplicationDataDirectoryPath)).thenReturn(false);

      // When
      var actualReturnValue = warp4JService.createExecutableJarFile(mockedWarp4jCommandConfiguration);

      // Then
      assertThat(actualReturnValue).isZero();
      mockedWarp4JConfiguration.verify(() -> Warp4JConfiguration.isSupportedTarget(target));
      verifyNoInteractions(mockedOptimizerService);
    }

    @Test
    void testCreateExecutableJarFile_WhenOptimizationWithAnalyzedModules_ThenOptimizationWillBeExecuted() throws IOException, NoPermissionException, InterruptedException {
      // Given
      var platform = Platform.WINDOWS;
      var architecture = Architecture.X64;
      var target = new Target(platform, architecture);
      mockUntilJdkPreparation("x64", "Windows 10", target);
      var mockedExtractedPath = mock(Path.class);
      var jdkProcessingStateTarget = JdkProcessingState.builder().target(target).isTarget(true).extractedJdkPath(mockedExtractedPath).build();
      mockCollectionOfCachedFiles(jdkProcessingStateTarget);
      when(mockedWarp4jCommandConfiguration.isOptimize()).thenReturn(true);
      when(mockedVersionData.getMajor()).thenReturn(17);

      var mockedExtractedBinPath = mock(Path.class);
      when(mockedExtractedPath.resolve("bin")).thenReturn(mockedExtractedBinPath);
      when(mockedExtractedBinPath.resolve(anyString())).thenReturn(mockedExtractedBinPath);
      when(mockedOptimizerService.analyzeModules(any(), any(), any(), any())).thenReturn("java.module1,java.module2");

      // When
      var actualReturnValue = warp4JService.createExecutableJarFile(mockedWarp4jCommandConfiguration);

      // Then
      assertThat(actualReturnValue).isZero();
      verify(mockedOptimizerService).analyzeModules(eq(mockedExtractedBinPath), any(), any(), eq(mockedVersionData));
      verify(mockedOptimizerService).createOptimizedRuntime(eq(target), any(), eq(mockedVersionData), eq(mockedApplicationDataDirectoryPath), eq(mockedExtractedBinPath), eq("java.module1,java.module2"));
    }

    @Test
    void testCreateExecutableJarFile_WhenOptimizationWithEmptyAnalyzedModulesAndModuleInfoClassInJar_ThenOptimizationWithFallbackWillBeExecuted() throws IOException, NoPermissionException, InterruptedException {
      // Given
      var platform = Platform.WINDOWS;
      var architecture = Architecture.X64;
      var target = new Target(platform, architecture);
      mockUntilJdkPreparation("x64", "Windows 10", target);
      var mockedExtractedPath = mock(Path.class);
      var jdkProcessingStateTarget = JdkProcessingState.builder().target(target).isTarget(true).extractedJdkPath(mockedExtractedPath).build();
      mockCollectionOfCachedFiles(jdkProcessingStateTarget);
      when(mockedWarp4jCommandConfiguration.isOptimize()).thenReturn(true);
      when(mockedVersionData.getMajor()).thenReturn(17);

      var mockedExtractedBinPath = mock(Path.class);
      when(mockedExtractedPath.resolve("bin")).thenReturn(mockedExtractedBinPath);
      when(mockedExtractedBinPath.resolve(anyString())).thenReturn(mockedExtractedBinPath);
      when(mockedOptimizerService.analyzeModules(any(), any(), any(), any())).thenReturn("").thenReturn("java.module1,java.module2");

      mockedFileUtilities.when(() -> FileUtilities.jarContainsModuleInfoClass(mockedJarFilePath)).thenReturn(true);
      var mockedModifiedJarFilePath = mock(Path.class);
      mockedFileUtilities.when(() -> FileUtilities.createJarFileWithoutModuleInfoClass(mockedJarFilePath, mockedApplicationDataDirectoryPath)).thenReturn(mockedModifiedJarFilePath);

      // When
      var actualReturnValue = warp4JService.createExecutableJarFile(mockedWarp4jCommandConfiguration);

      // Then
      assertThat(actualReturnValue).isZero();
      verify(mockedOptimizerService, times(2)).analyzeModules(eq(mockedExtractedBinPath), any(), any(), eq(mockedVersionData));
      verify(mockedOptimizerService).createOptimizedRuntime(eq(target), any(), eq(mockedVersionData), eq(mockedApplicationDataDirectoryPath), eq(mockedExtractedBinPath), eq("java.module1,java.module2"));
    }

    @Test
    void testCreateExecutableJarFile_WhenOptimizationWithEmptyAnalyzedModulesAndNoModuleInfoClassInJar_ThenOptimizationWithFallbackAllModulePathWillBeExecuted() throws IOException, NoPermissionException, InterruptedException {
      // Given
      var platform = Platform.WINDOWS;
      var architecture = Architecture.X64;
      var target = new Target(platform, architecture);
      mockUntilJdkPreparation("x64", "Windows 10", target);
      var mockedExtractedPath = mock(Path.class);
      var jdkProcessingStateTarget = JdkProcessingState.builder().target(target).isTarget(true).extractedJdkPath(mockedExtractedPath).build();
      mockCollectionOfCachedFiles(jdkProcessingStateTarget);
      when(mockedWarp4jCommandConfiguration.isOptimize()).thenReturn(true);
      when(mockedVersionData.getMajor()).thenReturn(17);

      var mockedExtractedBinPath = mock(Path.class);
      when(mockedExtractedPath.resolve("bin")).thenReturn(mockedExtractedBinPath);
      when(mockedExtractedBinPath.resolve(anyString())).thenReturn(mockedExtractedBinPath);
      when(mockedOptimizerService.analyzeModules(any(), any(), any(), any())).thenReturn("").thenReturn("java.module1,java.module2");

      // When
      var actualReturnValue = warp4JService.createExecutableJarFile(mockedWarp4jCommandConfiguration);

      // Then
      assertThat(actualReturnValue).isZero();
      verify(mockedOptimizerService).analyzeModules(eq(mockedExtractedBinPath), any(), any(), eq(mockedVersionData));
      verify(mockedOptimizerService).createOptimizedRuntime(eq(target), any(), eq(mockedVersionData), eq(mockedApplicationDataDirectoryPath), eq(mockedExtractedBinPath), eq("ALL-MODULE-PATH"));
    }
  }

  @Nested
  @DisplayName("Tests for Copy optional JDK to bundle directory")
  class TestsForCopyJdkToBundleDirectory {

    @Test
    void testCreateExecutableJarFile_WhenOptimization_ThenJdkWillNotCopiedToBundleDirectory() throws IOException, NoPermissionException, InterruptedException {
      // GIven
      var platform = Platform.WINDOWS;
      var architecture = Architecture.X64;
      var target = new Target(platform, architecture);
      mockUntilJdkPreparation("x64", "Windows 10", target);
      var mockedExtractedPath = mock(Path.class);
      var jdkProcessingStateTarget = JdkProcessingState.builder().target(target).isTarget(true).extractedJdkPath(mockedExtractedPath).optimized(true).build();
      mockCollectionOfCachedFiles(jdkProcessingStateTarget);

      // When
      var actualReturnValue = warp4JService.createExecutableJarFile(mockedWarp4jCommandConfiguration);

      // Then
      assertThat(actualReturnValue).isZero();
      verify(mockedFileService, never()).copyJdkToBundleDirectory(any(), any(), any());
    }

    @Test
    void testCreateExecutableJarFile_WhenNoOptimization_ThenJdkWillBeCopiedToBundleDirectory() throws IOException, NoPermissionException, InterruptedException {
      // GIven
      var platform = Platform.WINDOWS;
      var architecture = Architecture.X64;
      var target = new Target(platform, architecture);
      mockUntilJdkPreparation("x64", "Windows 10", target);
      var mockedExtractedPath = mock(Path.class);
      var jdkProcessingStateTarget = JdkProcessingState.builder().target(target).isTarget(true).extractedJdkPath(mockedExtractedPath).build();
      mockCollectionOfCachedFiles(jdkProcessingStateTarget);
      mockCopyJdkToBundleDirectory(target);

      // When
      var actualReturnValue = warp4JService.createExecutableJarFile(mockedWarp4jCommandConfiguration);

      // Then
      assertThat(actualReturnValue).isZero();
      verify(mockedFileService).copyJdkToBundleDirectory(target, mockedApplicationDataDirectoryPath, mockedVersionData);
    }
  }

  @Nested
  @DisplayName("Tests for Copy Jar file and launcher script to bundle directory")
  class TestsForCopyJarFileAndLauncherScriptToBundleDirectory {

    @Test
    void testCreateExecutableJarFile_WhenProcessed_ThenJarFileAndCreatedScriptLauncherWillBeCopiedToBundleDirectory() throws IOException, NoPermissionException, InterruptedException {
      // GIven
      var platform = Platform.WINDOWS;
      var architecture = Architecture.X64;
      var target = new Target(platform, architecture);
      mockUntilJdkPreparation("x64", "Windows 10", target);
      var mockedExtractedPath = mock(Path.class);
      var jdkProcessingStateTarget = JdkProcessingState.builder().target(target).isTarget(true).extractedJdkPath(mockedExtractedPath).build();
      mockCollectionOfCachedFiles(jdkProcessingStateTarget);
      mockCopyJdkToBundleDirectory(target);

      mockCopyJarFileAndCreateLauncherScriptToBundleDirectory(target);

      // When
      var actualReturnValue = warp4JService.createExecutableJarFile(mockedWarp4jCommandConfiguration);

      // Then
      assertThat(actualReturnValue).isZero();
      verify(mockedFileService).copyJarFileAndCreateLauncherScriptToBundleDirectory(target, mockedBundleDirectoryPath, mockedJarFilePath, mockedWarp4jCommandConfiguration);
    }
  }

  @Nested
  @DisplayName("Tests for warp bundle with warp-packer")
  class TestsForWarpBundleWithWarpPacker {

    @Test
    void testCreateExecutableJarFile_WhenProcessed_ThenBundleDirectoryWillBeWarped() throws IOException, NoPermissionException, InterruptedException {
      // GIven
      var platform = Platform.WINDOWS;
      var architecture = Architecture.X64;
      var target = new Target(platform, architecture);
      mockUntilJdkPreparation("x64", "Windows 10", target);
      var mockedExtractedPath = mock(Path.class);
      var jdkProcessingStateTarget = JdkProcessingState.builder().target(target).isTarget(true).extractedJdkPath(mockedExtractedPath).build();
      mockCollectionOfCachedFiles(jdkProcessingStateTarget);
      mockCopyJdkToBundleDirectory(target);
      mockCopyJarFileAndCreateLauncherScriptToBundleDirectory(target);

      mockWarpBundle(target);

      // When
      var actualReturnValue = warp4JService.createExecutableJarFile(mockedWarp4jCommandConfiguration);

      // Then
      assertThat(actualReturnValue).isZero();
      verify(mockedWarpService).warpBundle(target, mockedBundleDirectoryPath, mockedBundleScriptPath, mockedOutputDirectoryPath, mockedWarpPackerPath, "application");
    }
  }

  @Nested
  @DisplayName("Tests for compress warped bundle")
  class TestsForCompressWarpedBundle {

    @Test
    void testCreateExecutableJarFile_WhenProcessed_ThenWarpedBundleWillBeCompressed() throws IOException, NoPermissionException, InterruptedException {
      // GIven
      var platform = Platform.WINDOWS;
      var architecture = Architecture.X64;
      var target = new Target(platform, architecture);
      mockUntilJdkPreparation("x64", "Windows 10", target);
      var mockedExtractedPath = mock(Path.class);
      var jdkProcessingStateTarget = JdkProcessingState.builder().target(target).isTarget(true).extractedJdkPath(mockedExtractedPath).build();
      mockCollectionOfCachedFiles(jdkProcessingStateTarget);
      mockCopyJdkToBundleDirectory(target);
      mockCopyJarFileAndCreateLauncherScriptToBundleDirectory(target);
      mockWarpBundle(target);

      when(mockedFileService.compressBundle(target, mockedOutputDirectoryPath)).thenReturn(true);

      // When
      var actualReturnValue = warp4JService.createExecutableJarFile(mockedWarp4jCommandConfiguration);

      // Then
      assertThat(actualReturnValue).isZero();
      verify(mockedFileService).compressBundle(target, mockedOutputDirectoryPath);
    }
  }

  void mockUntilJdkPreparation(String currentPlatformString, String currentOsNameString, Target currentTarget) throws IOException {
    mockInitialization(currentPlatformString, currentOsNameString, currentTarget);
    var mockedExtractedPath = mock(Path.class);
    var jdkProcessingState = JdkProcessingState.builder().target(currentTarget).extracted(true).extractedJdkPath(mockedExtractedPath).build();
    mockCollectionOfCachedFiles(jdkProcessingState);
  }

  void mockInitialization(String currentPlatformString, String currentOsNameString, Target currentTarget) throws IOException {
    mockedWarp4JConfiguration.when(Warp4JConfiguration::getArchitecture).thenReturn(currentPlatformString);
    mockedWarp4JConfiguration.when(Warp4JConfiguration::getOsName).thenReturn(currentOsNameString);
    mockedWarp4JConfiguration.when(() -> Warp4JConfiguration.supportedPlatformAndArchitectureByWarp(currentTarget)).thenReturn(true);
    mockedWarp4JConfiguration.when(Warp4JConfiguration::initializeApplicationDataDirectory).thenReturn(mockedApplicationDataDirectoryPath);

    mockedWarp4JConfiguration.when(() -> Warp4JConfiguration.getWarpPackerPath(mockedApplicationDataDirectoryPath)).thenReturn(mockedWarpPackerPath);

    when(mockedWarp4jCommandConfiguration.getJavaVersion()).thenReturn("17");
    when(mockedDownloadService.getJavaVersionToUse("17")).thenReturn(mockedVersionData);
  }

  void mockCollectionOfCachedFiles(JdkProcessingState jdkProcessingState) {
    jdkProcessingStates = new ArrayList<>();
    jdkProcessingStates.add(jdkProcessingState);
    when(mockedCachedJdkCollectorService.collectCachedJdkStates(any(), any(), any())).thenReturn(jdkProcessingStates);
  }

  void mockCopyJdkToBundleDirectory(Target target) {
    when(mockedFileService.copyJdkToBundleDirectory(target, mockedApplicationDataDirectoryPath, mockedVersionData)).thenReturn(mockedBundleDirectoryPath);
  }

  void mockCopyJarFileAndCreateLauncherScriptToBundleDirectory(Target target) {
    mockedBundleScriptPath = mock(Path.class);
    when(mockedFileService.copyJarFileAndCreateLauncherScriptToBundleDirectory(target, mockedBundleDirectoryPath, mockedJarFilePath, mockedWarp4jCommandConfiguration)).thenReturn(mockedBundleScriptPath);
  }

  void mockWarpBundle(Target target) {
    var prefix = "application";
    when(mockedWarp4jCommandConfiguration.getPrefix()).thenReturn(prefix);
    when(mockedWarpService.warpBundle(target, mockedBundleDirectoryPath, mockedBundleScriptPath, mockedOutputDirectoryPath, mockedWarpPackerPath, prefix)).thenReturn(true);
  }
}

