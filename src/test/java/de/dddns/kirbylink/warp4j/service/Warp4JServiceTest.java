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
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import javax.naming.NoPermissionException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import de.dddns.kirbylink.warp4j.config.Warp4JCommand.Warp4JCommandConfiguration;
import de.dddns.kirbylink.warp4j.config.Warp4JConfiguration;
import de.dddns.kirbylink.warp4j.model.Architecture;
import de.dddns.kirbylink.warp4j.model.JdkProcessingState;
import de.dddns.kirbylink.warp4j.model.Platform;
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
      mockedWarp4JConfiguration.when(Warp4JConfiguration::getArchitecture).thenReturn("x64");
      mockedWarp4JConfiguration.when(Warp4JConfiguration::getOsName).thenReturn("Windows 10");
      mockedWarp4JConfiguration.when(() -> Warp4JConfiguration.supportedPlatformAndArchitectureByWarp(Architecture.X64, Platform.WINDOWS)).thenReturn(false);

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
      mockedWarp4JConfiguration.when(Warp4JConfiguration::getArchitecture).thenReturn("x64");
      mockedWarp4JConfiguration.when(Warp4JConfiguration::getOsName).thenReturn("Windows 10");
      mockedWarp4JConfiguration.when(() -> Warp4JConfiguration.supportedPlatformAndArchitectureByWarp(Architecture.X64, Platform.WINDOWS)).thenReturn(true);
      mockedWarp4JConfiguration.when(Warp4JConfiguration::initializeApplicationDataDirectory).thenReturn(mockedApplicationDataDirectoryPath);

      when(mockedWarpPackerPath.toString()).thenReturn("/path/to/application/data/warp/warp-packer.exe");
      mockedWarp4JConfiguration.when(() -> Warp4JConfiguration.getWarpPackerPath(mockedApplicationDataDirectoryPath)).thenReturn(mockedWarpPackerPath);
      doThrow(new IOException("Can not connect to github.com")).when(mockedDownloadService).downloadWarpPackerIfNeeded(mockedWarpPackerPath);

      // When
      var throwAbleMethod = catchThrowable(() -> {
        warp4JService.createExecutableJarFile(mockedWarp4jCommandConfiguration);
      });

      // Then
      assertThat(throwAbleMethod).isInstanceOf(FileNotFoundException.class).hasMessage("Warp-Packer does not exist and can not be downloaded. Internet connection needs to be checked or Warp-Packer must be available at /path/to/application/data/warp/warp-packer.exe");
    }

    @Test
    @SuppressWarnings("unchecked")
    void testCreateExecutableJarFile_WhenNoArchitectureAndNoPlatformIsSet_ThenAllSupportedCombinationWillBeUsed() throws IOException, NoPermissionException, InterruptedException {
      // Given
      mockInitialization("x64", "Windows 10", Platform.WINDOWS, Architecture.X64);

      ArgumentCaptor<List<Architecture>> listOfArchitectureCaptor = ArgumentCaptor.forClass(List.class);
      ArgumentCaptor<List<Platform>> listOfPlatformsCaptor = ArgumentCaptor.forClass(List.class);

      when(mockedCachedJdkCollectorService.collectCachedJdkStates(any(), any(), listOfArchitectureCaptor.capture(), listOfPlatformsCaptor.capture())).thenReturn(Collections.emptyList());

      // When
      var actualReturnValue = warp4JService.createExecutableJarFile(mockedWarp4jCommandConfiguration);

      // Then
      assertThat(actualReturnValue).isZero();
      assertThat(listOfArchitectureCaptor.getValue()).containsExactlyInAnyOrder(Architecture.values());
      assertThat(listOfPlatformsCaptor.getValue()).containsExactlyInAnyOrder(Platform.values());
    }

    @ParameterizedTest
    @SuppressWarnings("unchecked")
    @MethodSource("provideArchitectureAndPlatforms")
    void testCreateExecutableJarFile_WhenArchitectureAndPlatformIsSet_ThenAllCombinationWillBeUsed(boolean isLinux, boolean isMacos, boolean isWindows, String expectedArchitecture, List<Platform> expectedlistOfPlatforms, List<Architecture> expectedListOfArchitecture) throws IOException, NoPermissionException, InterruptedException {
      // Given
      mockInitialization("x64", "Windows 10", Platform.WINDOWS, Architecture.X64);

      ArgumentCaptor<List<Architecture>> listOfArchitectureCaptor = ArgumentCaptor.forClass(List.class);
      ArgumentCaptor<List<Platform>> listOfPlatformsCaptor = ArgumentCaptor.forClass(List.class);

      when(mockedWarp4jCommandConfiguration.isLinux()).thenReturn(isLinux);
      when(mockedWarp4jCommandConfiguration.isMacos()).thenReturn(isMacos);
      when(mockedWarp4jCommandConfiguration.isWindows()).thenReturn(isWindows);
      when(mockedWarp4jCommandConfiguration.getArchitecture()).thenReturn(expectedArchitecture);

      when(mockedCachedJdkCollectorService.collectCachedJdkStates(any(), any(), listOfArchitectureCaptor.capture(), listOfPlatformsCaptor.capture())).thenReturn(Collections.emptyList());

      // When
      var actualReturnValue = warp4JService.createExecutableJarFile(mockedWarp4jCommandConfiguration);

      // Then
      assertThat(actualReturnValue).isZero();
      assertThat(listOfArchitectureCaptor.getValue()).containsExactlyInAnyOrderElementsOf(expectedListOfArchitecture);
      assertThat(listOfPlatformsCaptor.getValue()).containsExactlyInAnyOrderElementsOf(expectedlistOfPlatforms);
    }

    @Test
    void testCreateExecutableJarFile_WhenTargetArchitectureAndNoPlatformNotEqualToCurrentSystem_ThenCurrentSystemWillBeAddedAsJdkProcessingState() throws IOException, NoPermissionException, InterruptedException {
      // Given
      mockInitialization("x64", "Windows 10", Platform.WINDOWS, Architecture.X64);

      jdkProcessingStates = new ArrayList<>();
      var jdkProcessingState = JdkProcessingState.builder().architecture(Architecture.X64).platform(Platform.LINUX).build();
      jdkProcessingStates.add(jdkProcessingState);
      when(mockedCachedJdkCollectorService.collectCachedJdkStates(any(), any(), any(), any())).thenReturn(jdkProcessingStates);
      when(mockedWarp4jCommandConfiguration.isOptimize()).thenReturn(true);
      mockedWarp4JConfiguration.when(() -> Warp4JConfiguration.isSupportedTarget(Architecture.X64, Platform.WINDOWS)).thenReturn(true);
      var expectedJdkProcessingState = JdkProcessingState.builder().architecture(Architecture.X64).platform(Platform.WINDOWS).build();
      when(mockedCachedJdkCollectorService.collectCachedJdkState(mockedApplicationDataDirectoryPath, Platform.WINDOWS, Architecture.X64, mockedVersionData)).thenReturn(expectedJdkProcessingState );

      when(mockedDownloadService.downloadJdk(any(), any(), any(), any())).thenReturn(false);

      // When
      var actualReturnValue = warp4JService.createExecutableJarFile(mockedWarp4jCommandConfiguration);

      // Then
      assertThat(actualReturnValue).isZero();
      assertThat(jdkProcessingStates)
        .hasSize(2)
        .contains(expectedJdkProcessingState);
    }

    static Stream<Arguments> provideArchitectureAndPlatforms() {
      return Stream.of(
          Arguments.of(true, false, false, "x64", List.of(Platform.LINUX), List.of(Architecture.X64)),
          Arguments.of(true, true, false, "aarch64", List.of(Platform.LINUX, Platform.MACOS), List.of(Architecture.AARCH64)),
          Arguments.of(true, true, true, "x64", List.of(Platform.LINUX, Platform.MACOS, Platform.WINDOWS), List.of(Architecture.X64)),
          Arguments.of(false, true, true, "x64", List.of(Platform.MACOS, Platform.WINDOWS), List.of(Architecture.X64)),
          Arguments.of(false, false, false, null, List.of(Platform.LINUX, Platform.MACOS, Platform.WINDOWS), List.of(Architecture.values())),
          Arguments.of(false, false, false, "", List.of(Platform.LINUX, Platform.MACOS, Platform.WINDOWS), List.of(Architecture.values()))
      );
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
      mockInitialization("x64", "Windows 10", platform, architecture);
      var jdkProcessingState = JdkProcessingState.builder().architecture(architecture).platform(platform).build();
      mockCollectionOfCachedFiles(jdkProcessingState);

      when(mockedDownloadService.downloadJdk(Platform.WINDOWS, Architecture.X64, mockedVersionData, mockedApplicationDataDirectoryPath)).thenReturn(true);

      // When
      var actualReturnValue = warp4JService.createExecutableJarFile(mockedWarp4jCommandConfiguration);

      // Then
      assertThat(actualReturnValue).isZero();
      verify(mockedDownloadService).downloadJdk(Platform.WINDOWS, Architecture.X64, mockedVersionData, mockedApplicationDataDirectoryPath);
    }

    @Test
    void testCreateExecutableJarFile_WhenJdkIsAvailable_ThenJdkWillNotBeDownloaded() throws IOException, NoPermissionException, InterruptedException {
      // Given
      var platform = Platform.WINDOWS;
      var architecture = Architecture.X64;
      mockInitialization("x64", "Windows 10", platform, architecture);
      var jdkProcessingState = JdkProcessingState.builder().architecture(architecture).platform(platform).downloaded(true).build();
      mockCollectionOfCachedFiles(jdkProcessingState);

      // When
      var actualReturnValue = warp4JService.createExecutableJarFile(mockedWarp4jCommandConfiguration);

      // Then
      assertThat(actualReturnValue).isZero();
      verify(mockedDownloadService, never()).downloadJdk(any(), any(), any(), any());
    }

    @Test
    void testCreateExecutableJarFile_WhenJdkIsAvailableButWithPullOption_ThenJdkWillBeDownloaded() throws IOException, NoPermissionException, InterruptedException {
      // Given
      var platform = Platform.WINDOWS;
      var architecture = Architecture.X64;
      mockInitialization("x64", "Windows 10", platform, architecture);
      var jdkProcessingState = JdkProcessingState.builder().architecture(architecture).platform(platform).downloaded(true).build();
      mockCollectionOfCachedFiles(jdkProcessingState);

      when(mockedWarp4jCommandConfiguration.isPull()).thenReturn(true);

      // When
      var actualReturnValue = warp4JService.createExecutableJarFile(mockedWarp4jCommandConfiguration);

      // Then
      assertThat(actualReturnValue).isZero();
      verify(mockedDownloadService).downloadJdk(Platform.WINDOWS, Architecture.X64, mockedVersionData, mockedApplicationDataDirectoryPath);
    }

    @Test
    void testCreateExecutableJarFile_WhenJdkIsAvailable_ThenJdkWillBeExtracted() throws IOException, NoPermissionException, InterruptedException {
      // Given
      var platform = Platform.WINDOWS;
      var architecture = Architecture.X64;
      mockInitialization("x64", "Windows 10", platform, architecture);
      var jdkProcessingState = JdkProcessingState.builder().architecture(architecture).platform(platform).downloaded(true).build();
      mockCollectionOfCachedFiles(jdkProcessingState);

      var mockedExtractedPath = mock(Path.class);
      when(mockedFileService.extractJdkAndDeleteCompressedFile(platform, architecture, mockedVersionData, mockedApplicationDataDirectoryPath)).thenReturn(mockedExtractedPath);

      // When
      var actualReturnValue = warp4JService.createExecutableJarFile(mockedWarp4jCommandConfiguration);

      // Then
      assertThat(actualReturnValue).isZero();
      verify(mockedFileService).extractJdkAndDeleteCompressedFile(platform, architecture, mockedVersionData, mockedApplicationDataDirectoryPath);
    }

    @Test
    void testCreateExecutableJarFile_WhenJdkIsExtracted_ThenJdkWillNotBeExtracted() throws IOException, NoPermissionException, InterruptedException {
      // Given
      var platform = Platform.WINDOWS;
      var architecture = Architecture.X64;
      mockInitialization("x64", "Windows 10", platform, architecture);
      var mockedExtractedPath = mock(Path.class);
      var jdkProcessingState = JdkProcessingState.builder().architecture(architecture).platform(platform).extracted(true).extractedJdkPath(mockedExtractedPath).build();
      mockCollectionOfCachedFiles(jdkProcessingState);


      // When
      var actualReturnValue = warp4JService.createExecutableJarFile(mockedWarp4jCommandConfiguration);

      // Then
      assertThat(actualReturnValue).isZero();
      verify(mockedFileService, never()).extractJdkAndDeleteCompressedFile(any(), any(), any(), any());
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
      mockUntilJdkPreparation("x64", "Windows 10", platform, architecture);

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
      mockUntilJdkPreparation("x64", "Windows 10", platform, architecture);
      var mockedExtractedPath = mock(Path.class);
      var jdkProcessingStateTarget = JdkProcessingState.builder().architecture(architecture).platform(Platform.LINUX).isTarget(true).extracted(true).extractedJdkPath(mockedExtractedPath).build();
      mockCollectionOfCachedFiles(jdkProcessingStateTarget);

      when(mockedWarp4jCommandConfiguration.isLinux()).thenReturn(true);
      when(mockedWarp4jCommandConfiguration.isOptimize()).thenReturn(true);
      when(mockedVersionData.getMajor()).thenReturn(17);

      mockedWarp4JConfiguration.when(() -> Warp4JConfiguration.isSupportedTarget(Architecture.X64, Platform.WINDOWS)).thenReturn(true);
      var jdkProcessingStateCurrentSystem = JdkProcessingState.builder().architecture(architecture).platform(platform).isTarget(false).build();
      when(mockedCachedJdkCollectorService.collectCachedJdkState(mockedApplicationDataDirectoryPath, platform, architecture, mockedVersionData)).thenReturn(jdkProcessingStateCurrentSystem);

      when(mockedDownloadService.downloadJdk(platform, architecture, mockedVersionData, mockedApplicationDataDirectoryPath)).thenReturn(false);

      // When
      var actualReturnValue = warp4JService.createExecutableJarFile(mockedWarp4jCommandConfiguration);

      // Then
      assertThat(actualReturnValue).isZero();
      mockedWarp4JConfiguration.verify(() -> Warp4JConfiguration.isSupportedTarget(Architecture.X64, Platform.WINDOWS));
      verifyNoInteractions(mockedOptimizerService);
    }

    @Test
    void testCreateExecutableJarFile_WhenOptimizationWithAnalyzedModules_ThenOptimizationWillBeExecuted() throws IOException, NoPermissionException, InterruptedException {
      // Given
      var platform = Platform.WINDOWS;
      var architecture = Architecture.X64;
      mockUntilJdkPreparation("x64", "Windows 10", platform, architecture);
      var mockedExtractedPath = mock(Path.class);
      var jdkProcessingStateTarget = JdkProcessingState.builder().architecture(architecture).platform(platform).isTarget(true).extractedJdkPath(mockedExtractedPath).build();
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
      verify(mockedOptimizerService).createOptimizedRuntime(eq(platform), eq(architecture), any(), eq(mockedVersionData), eq(mockedApplicationDataDirectoryPath), eq(mockedExtractedBinPath), eq("java.module1,java.module2"));
    }

    @Test
    void testCreateExecutableJarFile_WhenOptimizationWithEmptyAnalyzedModulesAndModuleInfoClassInJar_ThenOptimizationWithFallbackWillBeExecuted() throws IOException, NoPermissionException, InterruptedException {
      // Given
      var platform = Platform.WINDOWS;
      var architecture = Architecture.X64;
      mockUntilJdkPreparation("x64", "Windows 10", platform, architecture);
      var mockedExtractedPath = mock(Path.class);
      var jdkProcessingStateTarget = JdkProcessingState.builder().architecture(architecture).platform(platform).isTarget(true).extractedJdkPath(mockedExtractedPath).build();
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
      verify(mockedOptimizerService).createOptimizedRuntime(eq(platform), eq(architecture), any(), eq(mockedVersionData), eq(mockedApplicationDataDirectoryPath), eq(mockedExtractedBinPath), eq("java.module1,java.module2"));
    }

    @Test
    void testCreateExecutableJarFile_WhenOptimizationWithEmptyAnalyzedModulesAndNoModuleInfoClassInJar_ThenOptimizationWithFallbackAllModulePathWillBeExecuted() throws IOException, NoPermissionException, InterruptedException {
      // Given
      var platform = Platform.WINDOWS;
      var architecture = Architecture.X64;
      mockUntilJdkPreparation("x64", "Windows 10", platform, architecture);
      var mockedExtractedPath = mock(Path.class);
      var jdkProcessingStateTarget = JdkProcessingState.builder().architecture(architecture).platform(platform).isTarget(true).extractedJdkPath(mockedExtractedPath).build();
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
      verify(mockedOptimizerService).createOptimizedRuntime(eq(platform), eq(architecture), any(), eq(mockedVersionData), eq(mockedApplicationDataDirectoryPath), eq(mockedExtractedBinPath), eq("ALL-MODULE-PATH"));
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
      mockUntilJdkPreparation("x64", "Windows 10", platform, architecture);
      var mockedExtractedPath = mock(Path.class);
      var jdkProcessingStateTarget = JdkProcessingState.builder().architecture(architecture).platform(platform).isTarget(true).extractedJdkPath(mockedExtractedPath).optimized(true).build();
      mockCollectionOfCachedFiles(jdkProcessingStateTarget);

      // When
      var actualReturnValue = warp4JService.createExecutableJarFile(mockedWarp4jCommandConfiguration);

      // Then
      assertThat(actualReturnValue).isZero();
      verify(mockedFileService, never()).copyJdkToBundleDirectory(any(), any(), any(), any());
    }

    @Test
    void testCreateExecutableJarFile_WhenNoOptimization_ThenJdkWillBeCopiedToBundleDirectory() throws IOException, NoPermissionException, InterruptedException {
      // GIven
      var platform = Platform.WINDOWS;
      var architecture = Architecture.X64;
      mockUntilJdkPreparation("x64", "Windows 10", platform, architecture);
      var mockedExtractedPath = mock(Path.class);
      var jdkProcessingStateTarget = JdkProcessingState.builder().architecture(architecture).platform(platform).isTarget(true).extractedJdkPath(mockedExtractedPath).build();
      mockCollectionOfCachedFiles(jdkProcessingStateTarget);
      mockCopyJdkToBundleDirectory(platform, architecture);

      // When
      var actualReturnValue = warp4JService.createExecutableJarFile(mockedWarp4jCommandConfiguration);

      // Then
      assertThat(actualReturnValue).isZero();
      verify(mockedFileService).copyJdkToBundleDirectory(platform, architecture, mockedApplicationDataDirectoryPath, mockedVersionData);
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
      mockUntilJdkPreparation("x64", "Windows 10", platform, architecture);
      var mockedExtractedPath = mock(Path.class);
      var jdkProcessingStateTarget = JdkProcessingState.builder().architecture(architecture).platform(platform).isTarget(true).extractedJdkPath(mockedExtractedPath).build();
      mockCollectionOfCachedFiles(jdkProcessingStateTarget);
      mockCopyJdkToBundleDirectory(platform, architecture);

      mockCopyJarFileAndCreateLauncherScriptToBundleDirectory(platform, architecture);

      // When
      var actualReturnValue = warp4JService.createExecutableJarFile(mockedWarp4jCommandConfiguration);

      // Then
      assertThat(actualReturnValue).isZero();
      verify(mockedFileService).copyJarFileAndCreateLauncherScriptToBundleDirectory(platform, architecture, mockedBundleDirectoryPath, mockedJarFilePath, mockedWarp4jCommandConfiguration);
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
      mockUntilJdkPreparation("x64", "Windows 10", platform, architecture);
      var mockedExtractedPath = mock(Path.class);
      var jdkProcessingStateTarget = JdkProcessingState.builder().architecture(architecture).platform(platform).isTarget(true).extractedJdkPath(mockedExtractedPath).build();
      mockCollectionOfCachedFiles(jdkProcessingStateTarget);
      mockCopyJdkToBundleDirectory(platform, architecture);
      mockCopyJarFileAndCreateLauncherScriptToBundleDirectory(platform, architecture);

      mockWarpBundle(platform, architecture);

      // When
      var actualReturnValue = warp4JService.createExecutableJarFile(mockedWarp4jCommandConfiguration);

      // Then
      assertThat(actualReturnValue).isZero();
      verify(mockedWarpService).warpBundle(platform, architecture, mockedBundleDirectoryPath, mockedBundleScriptPath, mockedOutputDirectoryPath, mockedWarpPackerPath, "application");
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
      mockUntilJdkPreparation("x64", "Windows 10", platform, architecture);
      var mockedExtractedPath = mock(Path.class);
      var jdkProcessingStateTarget = JdkProcessingState.builder().architecture(architecture).platform(platform).isTarget(true).extractedJdkPath(mockedExtractedPath).build();
      mockCollectionOfCachedFiles(jdkProcessingStateTarget);
      mockCopyJdkToBundleDirectory(platform, architecture);
      mockCopyJarFileAndCreateLauncherScriptToBundleDirectory(platform, architecture);
      mockWarpBundle(platform, architecture);

      when(mockedFileService.compressBundle(platform, architecture, mockedOutputDirectoryPath)).thenReturn(true);

      // When
      var actualReturnValue = warp4JService.createExecutableJarFile(mockedWarp4jCommandConfiguration);

      // Then
      assertThat(actualReturnValue).isZero();
      verify(mockedFileService).compressBundle(platform, architecture, mockedOutputDirectoryPath);
    }
  }

  void mockUntilJdkPreparation(String currentPlatformString, String currentOsNameString, Platform currentPlatform, Architecture currentArchitecture) throws IOException {
    mockInitialization(currentPlatformString, currentOsNameString, currentPlatform, currentArchitecture);
    var mockedExtractedPath = mock(Path.class);
    var jdkProcessingState = JdkProcessingState.builder().architecture(currentArchitecture).platform(currentPlatform).extracted(true).extractedJdkPath(mockedExtractedPath).build();
    mockCollectionOfCachedFiles(jdkProcessingState);
  }

  void mockInitialization(String currentPlatformString, String currentOsNameString, Platform currentPlatform, Architecture currentArchitecture) throws IOException {
    mockedWarp4JConfiguration.when(Warp4JConfiguration::getArchitecture).thenReturn(currentPlatformString);
    mockedWarp4JConfiguration.when(Warp4JConfiguration::getOsName).thenReturn(currentOsNameString);
    mockedWarp4JConfiguration.when(() -> Warp4JConfiguration.supportedPlatformAndArchitectureByWarp(currentArchitecture, currentPlatform)).thenReturn(true);
    mockedWarp4JConfiguration.when(Warp4JConfiguration::initializeApplicationDataDirectory).thenReturn(mockedApplicationDataDirectoryPath);

    mockedWarp4JConfiguration.when(() -> Warp4JConfiguration.getWarpPackerPath(mockedApplicationDataDirectoryPath)).thenReturn(mockedWarpPackerPath);

    when(mockedWarp4jCommandConfiguration.getJavaVersion()).thenReturn("17");
    when(mockedDownloadService.getJavaVersionToUse("17")).thenReturn(mockedVersionData);
  }

  void mockCollectionOfCachedFiles(JdkProcessingState jdkProcessingState) {
    jdkProcessingStates = new ArrayList<>();
    jdkProcessingStates.add(jdkProcessingState);
    when(mockedCachedJdkCollectorService.collectCachedJdkStates(any(), any(), any(), any())).thenReturn(jdkProcessingStates);
  }

  void mockCopyJdkToBundleDirectory(Platform platform, Architecture architecture) {
    when(mockedFileService.copyJdkToBundleDirectory(platform, architecture, mockedApplicationDataDirectoryPath, mockedVersionData)).thenReturn(mockedBundleDirectoryPath);
  }

  void mockCopyJarFileAndCreateLauncherScriptToBundleDirectory(Platform platform, Architecture architecture) {
    mockedBundleScriptPath = mock(Path.class);
    when(mockedFileService.copyJarFileAndCreateLauncherScriptToBundleDirectory(platform, architecture, mockedBundleDirectoryPath, mockedJarFilePath, mockedWarp4jCommandConfiguration)).thenReturn(mockedBundleScriptPath);
  }

  void mockWarpBundle(Platform platform, Architecture architecture) {
    var prefix = "application";
    when(mockedWarp4jCommandConfiguration.getPrefix()).thenReturn(prefix);
    when(mockedWarpService.warpBundle(platform, architecture, mockedBundleDirectoryPath, mockedBundleScriptPath, mockedOutputDirectoryPath, mockedWarpPackerPath, prefix)).thenReturn(true);
  }
}

