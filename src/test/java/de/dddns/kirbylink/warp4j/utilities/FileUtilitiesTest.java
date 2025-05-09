package de.dddns.kirbylink.warp4j.utilities;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystemException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import de.dddns.kirbylink.warp4j.model.JavaVersion;
import de.dddns.kirbylink.warp4j.model.Platform;
import lombok.extern.slf4j.Slf4j;

@Slf4j
class FileUtilitiesTest {

  @TempDir
  static Path temporaryDirectory;

  private static Path zipFile;
  private static Path tarGzFile;

  @BeforeAll
  static void setup() throws IOException {
    zipFile = createTestZip(temporaryDirectory);
    tarGzFile = createTestTarGz(temporaryDirectory);
  }

  @Test
  void testCopyRecursively() throws IOException {
    // Given
    var sourceDir = temporaryDirectory.resolve("source");
    var nestedFile = sourceDir.resolve("subdir/file.txt");
    Files.createDirectories(nestedFile.getParent());
    Files.writeString(nestedFile, "Hello World");
    var targetDir = temporaryDirectory.resolve("target");

    // When
    FileUtilities.copyRecursively(sourceDir, targetDir);

    // Then
    var copiedFile = targetDir.resolve("subdir/file.txt");
    assertThat(copiedFile).exists();
    assertThat(Files.readString(copiedFile)).isEqualTo("Hello World");
  }

  @Test
  void testDeleteRecursively_deletesFilesAndSymlinks() throws IOException {
    // Given
    var directoryRecursivelyToDelete = Files.createDirectory(temporaryDirectory.resolve("recursive-folder-to-be-deleted."));
    var subDir = Files.createDirectory(directoryRecursivelyToDelete.resolve("subdir"));
    Files.createFile(subDir.resolve("file.txt"));
    var realTarget = Files.createFile(directoryRecursivelyToDelete.resolve("real.txt"));

    try {
      Files.createSymbolicLink(directoryRecursivelyToDelete.resolve("link.txt"), realTarget);
    } catch (UnsupportedOperationException | FileSystemException e) {
      log.warn("No symlinks supported. Skip.");
    }

    // When
    FileUtilities.deleteRecursively(directoryRecursivelyToDelete);

    // Then
    assertThat(directoryRecursivelyToDelete).doesNotExist();
  }

  @Test
  void testDeleteRecursively_WhenPathNotExist_NoErrorOccurs() {
    // Given

    // When
    var throwAbleMethod = catchThrowable(() -> {
      FileUtilities.deleteRecursively(Path.of("/not/existing/path"));
    });

    // Then
    assertThat(throwAbleMethod).isNull();
  }

  @Test
  void testCreateZipFromFile() throws IOException {
    var file = Files.writeString(temporaryDirectory.resolve("hello.txt"), "Just a file!");
    zipFile = Files.createTempFile("test", ".zip");

    FileUtilities.createZip(file, zipFile);
    assertThat(zipFile).exists();
  }

  @Test
  void testCreateZipFromDirectory() throws IOException {
    var folder = temporaryDirectory.resolve("subdirectory-zip");
    Files.createDirectory(folder);
    Files.writeString(folder.resolve("hello.txt"), "Just a file!");

    FileUtilities.createZip(folder, zipFile);
    assertThat(zipFile).exists();
  }

  @Test
  void testCreateTarGzFromFile() throws IOException {
    var file = Files.writeString(temporaryDirectory.resolve("hello.txt"), "Only me inside!");

    FileUtilities.createTarGz(file, tarGzFile);
    assertThat(tarGzFile).exists();
  }

  @Test
  void testCreateTarGzFromFolder() throws IOException {
    var folder = temporaryDirectory.resolve("subdirectory-targz");
    Files.createDirectory(folder);
    Files.writeString(folder.resolve("hello.txt"), "Only me inside!");

    FileUtilities.createTarGz(folder, tarGzFile);
    assertThat(tarGzFile).exists();
  }

  @Test
  void testExtractZip() throws IOException {
    // Given
    var outputDir = temporaryDirectory.resolve("output");

    // When
    FileUtilities.extractZip(zipFile, outputDir);

    // Then
    assertThat(outputDir.resolve("testfile.txt")).exists();
    assertThat(outputDir.resolve("subdir/nestedfile.txt")).exists();
  }

  @Test
  void testExtractTarGz() throws IOException {
    // Given
    var outputDir = temporaryDirectory.resolve("output");

    // When
    FileUtilities.extractTarGz(tarGzFile, outputDir, Platform.LINUX);

    // Then
    assertThat(outputDir.resolve("testfile.txt")).exists();
    assertThat(outputDir.resolve("subdir/nestedfile.txt")).exists();
  }

  @Test
  void testExtractTarGz_macOS() throws IOException {
    // Given
    var tarGzMac = createTestTarGzForMac(temporaryDirectory);
    var outputDir = temporaryDirectory.resolve("mac-output");

    // When
    FileUtilities.extractTarGz(tarGzMac, outputDir, Platform.MACOS);

    // Then
    var jdkRoot = outputDir.resolve("jdk-21.jdk");
    assertThat(jdkRoot.resolve("testfile.txt")).exists();
    assertThat(jdkRoot.resolve("subdir/nestedfile.txt")).exists();
    assertThat(Files.readString(jdkRoot.resolve("testfile.txt"))).isEqualTo("Mac Test Content");
    assertThat(Files.readString(jdkRoot.resolve("subdir/nestedfile.txt"))).isEqualTo("Nested Mac content");
  }

  @Test
  void testMapModeToPermissions() {
    assertThat(FileUtilities.convertModeToPosix(0755)).containsExactlyInAnyOrder(
        PosixFilePermission.OWNER_READ,
        PosixFilePermission.OWNER_WRITE,
        PosixFilePermission.OWNER_EXECUTE,
        PosixFilePermission.GROUP_READ,
        PosixFilePermission.GROUP_EXECUTE,
        PosixFilePermission.OTHERS_READ,
        PosixFilePermission.OTHERS_EXECUTE);

    assertThat(FileUtilities.convertModeToPosix(0644)).containsExactlyInAnyOrder(
        PosixFilePermission.OWNER_READ,
        PosixFilePermission.OWNER_WRITE,
        PosixFilePermission.GROUP_READ,
        PosixFilePermission.OTHERS_READ);

    assertThat(FileUtilities.convertModeToPosix(0700)).containsExactlyInAnyOrder(
        PosixFilePermission.OWNER_READ,
        PosixFilePermission.OWNER_WRITE,
        PosixFilePermission.OWNER_EXECUTE);

    assertThat(FileUtilities.convertModeToPosix(0000)).isEmpty();
  }
  
  @Test
  void testExtractVersionFromJdkFolder() {
    // Given
    var mockedJdkDirectoryPath = mock(Path.class);
    when(mockedJdkDirectoryPath.getFileName()).thenReturn(mockedJdkDirectoryPath);
    when(mockedJdkDirectoryPath.toString()).thenReturn("jdk-17.0.15+6");
    var expectedJavaVersionResult = new JavaVersion(17, 0, 15, 6);
    
    // When
    var actualJavaVersionResult = FileUtilities.extractVersionFromJdkFolder(mockedJdkDirectoryPath);
    
    // Then
    assertThat(actualJavaVersionResult).isEqualByComparingTo(expectedJavaVersionResult);
  }

  @Test
  void testJarContainsModuleInfoClass() throws IOException {
    // Given
    var jarFileWithModuleInfoClass = Files.createFile(temporaryDirectory.resolve("test-with-modul-info-classes.jar"));
    createJarFile(jarFileWithModuleInfoClass);

    // When
    var actualContainsModuleInfoClasses = FileUtilities.jarContainsModuleInfoClass(jarFileWithModuleInfoClass);

    // Then
    assertThat(actualContainsModuleInfoClasses).isTrue();
  }

  @Test
  void testJarContainsModuleInfoClass_WhenExceptionOccursThenFalseIsReturned() {
    // Given

    // When
    var actualContainsModuleInfoClasses = FileUtilities.jarContainsModuleInfoClass(null);

    // Then
    assertThat(actualContainsModuleInfoClasses).isFalse();
  }

  @Test
  void testCreateJarFileWithoutModuleInfoClass() throws IOException {
    // Given
    var jarFileWithModuleInfoClass = Files.createFile(temporaryDirectory.resolve("test-with-module-info.jar"));
    var outputDir = temporaryDirectory.resolve("output");
    Files.createDirectories(outputDir);
    createJarFile(jarFileWithModuleInfoClass);

    // When
    var cleanedJar = FileUtilities.createJarFileWithoutModuleInfoClass(jarFileWithModuleInfoClass, outputDir);

    // Then
    try (var jarFile = new JarFile(cleanedJar.toFile())) {
      var entries = jarFile.stream().map(JarEntry::getName).toList();
      assertThat(entries).doesNotContain("module-info.class");
      assertThat(entries).contains("com/example/MyClass.class");
    }
  }

  private void createJarFile(Path jarFileWithModuleInfoClass) throws IOException {
    try (var jarOutputStream = new JarOutputStream(Files.newOutputStream(jarFileWithModuleInfoClass))) {
      jarOutputStream.putNextEntry(new JarEntry("module-info.class"));
      jarOutputStream.write("dummy module-info".getBytes());
      jarOutputStream.closeEntry();

      jarOutputStream.putNextEntry(new JarEntry("com/example/MyClass.class"));
      jarOutputStream.write("dummy class content".getBytes());
      jarOutputStream.closeEntry();
    }
  }


  private static Path createTestZip(Path directory) throws IOException {
    var zipFile = directory.resolve("test.zip");
    try (var fs = FileSystems.newFileSystem(URI.create("jar:" + zipFile.toUri()), Map.of("create", "true"))) {
      var fileInsideZip = fs.getPath("testfile.txt");
      Files.writeString(fileInsideZip, "Test content");

      var subdir = fs.getPath("subdir");
      Files.createDirectory(subdir);
      var nestedFile = subdir.resolve("nestedfile.txt");
      Files.writeString(nestedFile, "Nested content");
    }
    return zipFile;
  }

  private static Path createTestTarGz(Path directory) throws IOException {
    var tarGzFile = directory.resolve("test.tar.gz");
    try (var tarArchiveOutputStream = new TarArchiveOutputStream(new GzipCompressorOutputStream(Files.newOutputStream(tarGzFile)))) {
      var temporaryFile = directory.resolve("testfile.txt");
      Files.writeString(temporaryFile, "Test content");
      var entry = new TarArchiveEntry(temporaryFile.toFile(), "testfile.txt");
      entry.setSize(Files.size(temporaryFile));
      entry.setMode(0644);
      tarArchiveOutputStream.putArchiveEntry(entry);
      Files.copy(temporaryFile, tarArchiveOutputStream);
      tarArchiveOutputStream.closeArchiveEntry();

      var subdir = directory.resolve("subdir");
      Files.createDirectory(subdir);
      var dirEntry = new TarArchiveEntry(subdir.toFile(), "subdir/");
      tarArchiveOutputStream.putArchiveEntry(dirEntry);
      tarArchiveOutputStream.closeArchiveEntry();

      var nestedFile = subdir.resolve("nestedfile.txt");
      Files.writeString(nestedFile, "Nested content");
      var nestedEntry = new TarArchiveEntry(nestedFile.toFile(), "subdir/nestedfile.txt");
      nestedEntry.setSize(Files.size(nestedFile));
      nestedEntry.setMode(0755);
      tarArchiveOutputStream.putArchiveEntry(nestedEntry);
      Files.copy(nestedFile, tarArchiveOutputStream);
      tarArchiveOutputStream.closeArchiveEntry();
    }
    return tarGzFile;
  }

  private static Path createTestTarGzForMac(Path directory) throws IOException {
    var tarGzFile = directory.resolve("mac-test.tar.gz");
    var basePath = "jdk-21.jdk/Contents/Home/";

    try (var out = new TarArchiveOutputStream(new GzipCompressorOutputStream(Files.newOutputStream(tarGzFile)))) {
      var testFile = Files.writeString(directory.resolve("testfile.txt"), "Mac Test Content");
      var fileEntry = new TarArchiveEntry(testFile.toFile(), basePath + "testfile.txt");
      fileEntry.setSize(Files.size(testFile));
      fileEntry.setMode(0644);
      out.putArchiveEntry(fileEntry);
      Files.copy(testFile, out);
      out.closeArchiveEntry();

      // Subdirectory + Datei
      var subDir = Files.createDirectory(directory.resolve("mac-subdir"));
      var nestedFile = Files.writeString(subDir.resolve("nestedfile.txt"), "Nested Mac content");
      var dirEntry = new TarArchiveEntry(basePath + "subdir/");
      out.putArchiveEntry(dirEntry);
      out.closeArchiveEntry();

      var nestedEntry = new TarArchiveEntry(nestedFile.toFile(), basePath + "subdir/nestedfile.txt");
      nestedEntry.setSize(Files.size(nestedFile));
      nestedEntry.setMode(0755);
      out.putArchiveEntry(nestedEntry);
      Files.copy(nestedFile, out);
      out.closeArchiveEntry();
    }

    return tarGzFile;
  }
}
