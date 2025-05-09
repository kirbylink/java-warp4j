package de.dddns.kirbylink.warp4j.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import de.dddns.kirbylink.warp4j.config.Warp4JCommand.Warp4JCommandConfiguration;
import de.dddns.kirbylink.warp4j.config.Warp4JConfiguration;
import de.dddns.kirbylink.warp4j.model.Architecture;
import de.dddns.kirbylink.warp4j.model.Platform;
import de.dddns.kirbylink.warp4j.model.adoptium.v3.VersionData;
import de.dddns.kirbylink.warp4j.utilities.FileUtilities;

class FileServiceTest {

  private FileService fileService;
  private static MockedStatic<Files> mockedFiles;
  private static MockedStatic<FileUtilities> mockedFileUtilities;
  private static MockedStatic<Warp4JConfiguration> mockedWarp4JConfiguration;

  @BeforeEach
  void setUp() {
    fileService = new FileService();
    mockedFiles = mockStatic(Files.class);
    mockedFileUtilities = mockStatic(FileUtilities.class);
    mockedWarp4JConfiguration = mockStatic(Warp4JConfiguration.class);
  }

  @AfterEach
  void tearDown() {
    mockedFiles.close();
    mockedFileUtilities.close();
    mockedWarp4JConfiguration.close();
  }

  @Nested
  @DisplayName("Tests for Windows")
  class TestsForWindows {

    private final Platform platform = Platform.WINDOWS;
    private final Architecture architecture = Architecture.X64;

    @Test
    void testExtractJdkAndDeleteCompressedFile_WhenWindows_ThenZipExtractedAndDeleted() {
      // Given
      var versionData = new VersionData();
      versionData.setMajor(17);

      var mockedApplicationDataDirectory = mock(Path.class);
      var mockedZipPath = mock(Path.class);
      var mockedExtractedPath = mock(Path.class);

      when(mockedApplicationDataDirectory.resolve(anyString())).thenReturn(mockedApplicationDataDirectory);
      mockedFiles.when(() -> Files.deleteIfExists(mockedZipPath)).thenReturn(true);
      mockedFiles.when(() -> Files.list(mockedApplicationDataDirectory)).thenReturn(Stream.of(mockedExtractedPath));
      mockedFiles.when(() -> Files.isDirectory(mockedExtractedPath)).thenReturn(true);
      when(mockedExtractedPath.getFileName()).thenReturn(Path.of("jdk-17"));

      mockedWarp4JConfiguration.when(() -> Warp4JConfiguration.isOnlyFeatureVersion(versionData)).thenReturn(true);
      mockedFileUtilities.when(() -> FileUtilities.extractZip(any(), any())).then(inv -> null);
      mockedFileUtilities.when(() -> FileUtilities.optionalExtractedJdkPath(mockedApplicationDataDirectory, "17")).thenReturn(Optional.of(mockedApplicationDataDirectory));

      // When
      var result = fileService.extractJdkAndDeleteCompressedFile(platform, architecture, versionData, mockedApplicationDataDirectory);

      // Then
      assertThat(result).isNotNull();
    }

    @Test
    void testCompressBundle_WhenWindows_ThenZipCreated() {
      // Given
      var mockedBundlePath = mock(Path.class);
      var mockedBundleDirectory = mock(Path.class);
      var mockedBundleFileName = mock(Path.class);
      var mockedCompressedTarget = mock(Path.class);

      when(mockedBundlePath.getParent()).thenReturn(mockedBundleDirectory);
      when(mockedBundlePath.getFileName()).thenReturn(mockedBundleFileName);
      when(mockedBundleFileName.toString()).thenReturn("warp4j.exe");
      when(mockedBundleDirectory.resolve("warp4j.zip")).thenReturn(mockedCompressedTarget);

      mockedFileUtilities.when(() -> FileUtilities.createZip(mockedBundlePath, mockedCompressedTarget)).then(inv -> null);

      // When
      var result = fileService.compressBundle(platform, architecture, mockedBundlePath);

      // Then
      assertThat(result).isTrue();
    }

    @Test
    void testCopyJarFileAndCreateLauncherScriptToBundleDirectory_WhenWindows_ThenCreatesBatScript() {
      // Given
      var mockedConfig = mock(Warp4JCommandConfiguration.class);
      var mockedBundlePath = mock(Path.class);
      var mockedJarPath = mock(Path.class);

      when(mockedJarPath.getFileName()).thenReturn(Path.of("myapp.jar"));
      when(mockedBundlePath.resolve(anyString())).thenReturn(mockedBundlePath);
      when(mockedConfig.isSilent()).thenReturn(true);

      mockedFiles.when(() -> Files.copy(mockedJarPath, mockedBundlePath)).thenReturn(mockedBundlePath);
      mockedFiles.when(() -> Files.write(eq(mockedBundlePath), any(byte[].class), eq(StandardOpenOption.CREATE), eq(StandardOpenOption.TRUNCATE_EXISTING))).thenReturn(mockedBundlePath);

      mockedWarp4JConfiguration.when(() -> Warp4JConfiguration.getLauncherWindows(any(), any(), any(), eq(true))).thenReturn("echo Windows!");

      // When
      var result = fileService.copyJarFileAndCreateLauncherScriptToBundleDirectory(platform, architecture, mockedBundlePath, mockedJarPath, mockedConfig);

      // Then
      assertThat(result).isNotNull();
    }
  }

  @Nested
  @DisplayName("Tests for Linux")
  class TestsForLinux {

    private final Platform platform = Platform.LINUX;
    private final Architecture architecture = Architecture.X64;

    @Test
    void testExtractJdkAndDeleteCompressedFile_WhenLinux_ThenReturnsExtractedPath() {
      // Given
      var versionData = new VersionData();
      versionData.setMajor(17);
      versionData.setSemver("17.0.14+7");

      var mockedAppDataDir = mock(Path.class);
      var mockedJdkRoot = mock(Path.class);
      var mockedJdkDirectory = mock(Path.class);
      var mockedCompressed = mock(Path.class);
      var mockedExtracted = mock(Path.class);

      when(mockedAppDataDir.resolve(anyString())).thenReturn(mockedAppDataDir);
      when(mockedAppDataDir.resolve("jdk")).thenReturn(mockedJdkRoot);
      when(mockedJdkRoot.resolve(platform.getValue())).thenReturn(mockedJdkDirectory);
      when(mockedJdkDirectory.resolve(architecture.getValue())).thenReturn(mockedJdkDirectory);
      when(mockedJdkDirectory.resolve("jdk.tar.gz")).thenReturn(mockedCompressed);
      when(mockedExtracted.getFileName()).thenReturn(Path.of("jdk-17.0.14+7"));
      
      mockedWarp4JConfiguration.when(() -> Warp4JConfiguration.isOnlyFeatureVersion(versionData)).thenReturn(false);
      mockedFileUtilities.when(() -> FileUtilities.optionalExtractedJdkPath(mockedJdkDirectory, "17.0.14+7")).thenReturn(Optional.of(mockedExtracted));

      mockedFiles.when(() -> Files.deleteIfExists(mockedCompressed)).thenReturn(true);
      mockedFiles.when(() -> Files.list(mockedJdkDirectory)).thenReturn(Stream.of(mockedExtracted));
      mockedFiles.when(() -> Files.isDirectory(mockedExtracted)).thenReturn(true);

      // When
      var result = fileService.extractJdkAndDeleteCompressedFile(platform, architecture, versionData, mockedAppDataDir);

      // Then
      assertThat(result).isEqualTo(mockedExtracted);
    }

    @Test
    void testCompressBundle_WhenLinux_ThenTarGzCreated() {
      // Given
      var mockedBundlePath = mock(Path.class);
      var mockedBundleDir = mock(Path.class);
      var mockedBundleFileName = mock(Path.class);
      var mockedCompressedTarget = mock(Path.class);

      when(mockedBundlePath.getParent()).thenReturn(mockedBundleDir);
      when(mockedBundlePath.getFileName()).thenReturn(mockedBundleFileName);
      when(mockedBundleFileName.toString()).thenReturn("warp4j");
      when(mockedBundleDir.resolve("warp4j.tar.gz")).thenReturn(mockedCompressedTarget);

      mockedFileUtilities.when(() -> FileUtilities.createTarGz(mockedBundlePath, mockedCompressedTarget)).then(inv -> null);

      // When
      var result = fileService.compressBundle(platform, architecture, mockedBundlePath);

      // Then
      assertThat(result).isTrue();
    }

    @Test
    void testCopyJarFileAndCreateLauncherScriptToBundleDirectory_WhenLinux_ThenCreatesShellScript() {
      // Given
      var mockedConfig = mock(Warp4JCommandConfiguration.class);
      var mockedBundlePath = mock(Path.class);
      var mockedJarPath = mock(Path.class);
      var mockedScriptPath = mock(Path.class);

      when(mockedJarPath.getFileName()).thenReturn(Path.of("myapp.jar"));
      when(mockedConfig.isSilent()).thenReturn(false);
      when(mockedBundlePath.resolve(anyString())).thenReturn(mockedBundlePath);

      mockedFiles.when(() -> Files.copy(mockedJarPath, mockedBundlePath)).thenReturn(mockedBundlePath);
      mockedFiles.when(() -> Files.write(eq(mockedBundlePath), any(byte[].class), eq(StandardOpenOption.CREATE), eq(StandardOpenOption.TRUNCATE_EXISTING))).thenReturn(mockedScriptPath);
      mockedFiles.when(() -> Files.setPosixFilePermissions(eq(mockedScriptPath), any())).thenReturn(mockedScriptPath);

      mockedWarp4JConfiguration.when(() -> Warp4JConfiguration.getLauncherBash(any(), any(), any(), any())).thenReturn("#!/bin/bash\necho Linux");

      // When
      var result = fileService.copyJarFileAndCreateLauncherScriptToBundleDirectory(platform, architecture, mockedBundlePath, mockedJarPath, mockedConfig);

      // Then
      assertThat(result).isEqualTo(mockedScriptPath);
    }
  }

  @Nested
  @DisplayName("Tests for MacOs")
  class TestsForMacOs {

    private final Platform platform = Platform.MACOS;
    private final Architecture architecture = Architecture.X64;

    @Test
    void testCompressBundle_WhenMac_ThenAppStructureCreated() {
      // Given
      var mockedBundlePath = mock(Path.class);
      var mockedBundleDir = mock(Path.class);
      var mockedBundleFileName = mock(Path.class);
      var mockedAppDir = mock(Path.class);
      var mockedAppTarget = mock(Path.class);
      var mockedCompressed = mock(Path.class);

      var mockedAppDirFile = mock(File.class);

      when(mockedBundlePath.getParent()).thenReturn(mockedBundleDir);
      when(mockedBundlePath.getFileName()).thenReturn(mockedBundleFileName);
      when(mockedBundleFileName.toString()).thenReturn("myapp");
      when(mockedBundleDir.resolve("myapp.app")).thenReturn(mockedAppDir);
      when(mockedAppDir.toFile()).thenReturn(mockedAppDirFile);
      when(mockedAppDirFile.exists()).thenReturn(false);

      when(mockedBundleDir.resolve("myapp.tar.gz")).thenReturn(mockedCompressed);
      when(mockedAppDir.resolve("myapp")).thenReturn(mockedAppTarget);

      mockedFiles.when(() -> Files.createDirectory(mockedAppDir)).thenReturn(mockedAppDir);
      mockedFiles.when(() -> Files.copy(mockedBundlePath, mockedAppTarget, StandardCopyOption.REPLACE_EXISTING)).thenReturn(mockedAppTarget);
      mockedFileUtilities.when(() -> FileUtilities.createTarGz(mockedAppDir, mockedCompressed)).then(inv -> null);

      // When
      var result = fileService.compressBundle(platform, architecture, mockedBundlePath);

      // Then
      assertThat(result).isTrue();
    }
  }
}
