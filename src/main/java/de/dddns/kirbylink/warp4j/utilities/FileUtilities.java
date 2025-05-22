package de.dddns.kirbylink.warp4j.utilities;

import static de.dddns.kirbylink.warp4j.config.Warp4JConfiguration.APPLICATION_DATA_BUNDLE_DIRECTORY;
import static de.dddns.kirbylink.warp4j.config.Warp4JConfiguration.APPLICATION_DATA_JAVA_DIRECTORY;
import static de.dddns.kirbylink.warp4j.config.Warp4JConfiguration.CLEANED_JAR_FILE_JAR;
import static java.lang.String.format;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigInteger;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.Optional;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import de.dddns.kirbylink.warp4j.model.JavaVersion;
import de.dddns.kirbylink.warp4j.model.Platform;
import de.dddns.kirbylink.warp4j.model.Target;
import de.dddns.kirbylink.warp4j.model.adoptium.v3.VersionData;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@UtilityClass
public class FileUtilities {

  public static void copyRecursively(Path source, Path target) throws IOException {
    Files.walkFileTree(source, new SimpleFileVisitor<>() {
      @Override
      public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
        var targetDir = target.resolve(source.relativize(dir));
        Files.createDirectories(targetDir);
        return FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        var targetFile = target.resolve(source.relativize(file));
        Files.copy(file, targetFile, StandardCopyOption.REPLACE_EXISTING);
        return FileVisitResult.CONTINUE;
      }
    });
  }

  public static void deleteRecursively(Path path) throws IOException {
    if (Files.notExists(path)) {
      return;
    }

    Files.walkFileTree(path, new SimpleFileVisitor<>() {
      @Override
      public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        log.trace("Delete file: {}", file);
        Files.delete(file);
        return FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
        log.trace("Delete directory: {}", dir);
        Files.delete(dir);
        return FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
        log.warn("Failed to access: {}", file, exc);
        Files.deleteIfExists(file);
        return FileVisitResult.CONTINUE;
      }
    });
  }

  public static Path resolveOptionalWildCardAndFindFirstMatch(Path pathWithoptionalWildcardInFileName) {
    var pathString = pathWithoptionalWildcardInFileName.toString();
    if (!pathString.contains("*") && !pathString.contains("?")) {
      return pathWithoptionalWildcardInFileName;
    }

    var parent = Optional.ofNullable(pathWithoptionalWildcardInFileName.getParent()).orElse(Paths.get("."));
    var pattern = pathWithoptionalWildcardInFileName.getFileName().toString();

    try (var matches = Files.newDirectoryStream(parent, pattern)) {
      var iterator = matches.iterator();
      if (iterator.hasNext()) {
        var match = iterator.next();
        log.info("Resolved wildcard '{}' to '{}'", matches, match);
        return match;
      }
      throw new IllegalArgumentException("No files matched wildcard pattern: " + pathWithoptionalWildcardInFileName);
    } catch (IOException e) {
      throw new UncheckedIOException("Error while resolving wildcard path: " + pathWithoptionalWildcardInFileName, e);
    }
  }

  public static void createZip(Path sourcePath, Path zipFilePath) throws IOException {
    try (var fileOutputStream = Files.newOutputStream(zipFilePath); var zipOutputStream = new ZipOutputStream(fileOutputStream)) {

      if (Files.isDirectory(sourcePath)) {
        Files.walkFileTree(sourcePath, new SimpleFileVisitor<>() {
          @Override
          public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            var entryName = sourcePath.relativize(file).toString().replace("\\", "/");
            zipOutputStream.putNextEntry(new ZipEntry(entryName));
            Files.copy(file, zipOutputStream);
            zipOutputStream.closeEntry();
            return FileVisitResult.CONTINUE;
          }

          @Override
          public FileVisitResult preVisitDirectory(Path directory, BasicFileAttributes attrs) throws IOException {
            if (!sourcePath.equals(directory)) {
              var entryName = sourcePath.relativize(directory).toString().replace("\\", "/") + "/";
              zipOutputStream.putNextEntry(new ZipEntry(entryName));
              zipOutputStream.closeEntry();
            }
            return FileVisitResult.CONTINUE;
          }
        });
      } else {
        var entryName = sourcePath.getFileName().toString();
        zipOutputStream.putNextEntry(new ZipEntry(entryName));
        Files.copy(sourcePath, zipOutputStream);
        zipOutputStream.closeEntry();
      }
    }
  }

  public static void createTarGz(Path sourcePath, Path tarGzFilePath) throws IOException {
    try (var fileOutputSteam = Files.newOutputStream(tarGzFilePath);
        var bufferedOutputStream = new BufferedOutputStream(fileOutputSteam);
        var gzipCompressorOutputStream = new GzipCompressorOutputStream(bufferedOutputStream);
        var tarArchiveOutputStream = new TarArchiveOutputStream(gzipCompressorOutputStream)) {
      tarArchiveOutputStream.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);

      if (Files.isDirectory(sourcePath)) {
        try (var paths = Files.walk(sourcePath)) {
          var rootPath = sourcePath.getFileName();

          paths.forEach(path -> {
            try {
              var relativePath = rootPath.resolve(sourcePath.relativize(path));
              var entry = new TarArchiveEntry(path.toFile(), relativePath.toString());

              if (Files.isExecutable(path)) {
                entry.setMode(0755);
              }

              tarArchiveOutputStream.putArchiveEntry(entry);

              if (Files.isRegularFile(path)) {
                Files.copy(path, tarArchiveOutputStream);
              }

              tarArchiveOutputStream.closeArchiveEntry();
            } catch (IOException e) {
              throw new UncheckedIOException(e);
            }
          });
        }
      } else {
        var entry = new TarArchiveEntry(sourcePath.toFile(), sourcePath.getFileName().toString());
        if (Files.isExecutable(sourcePath)) {
          entry.setMode(0755);
        }
        tarArchiveOutputStream.putArchiveEntry(entry);
        Files.copy(sourcePath, tarArchiveOutputStream);
        tarArchiveOutputStream.closeArchiveEntry();
      }
    }
  }

  public static void extractZip(Path zipFile, Path targetDir) throws IOException {
    try (var zip = ZipFile.builder().setFile(zipFile.toFile()).get()) {
      zip.getEntries().asIterator().forEachRemaining(entry -> {
        try {
          var entryPath = targetDir.resolve(entry.getName());
          if (entry.isDirectory()) {
            Files.createDirectories(entryPath);
          } else {
            Files.createDirectories(entryPath.getParent());
            try (var is = zip.getInputStream(entry); var os = Files.newOutputStream(entryPath)) {
              is.transferTo(os);
            }
          }
        } catch (IOException e) {
          throw new UncheckedIOException(e);
        }
      });
    }
  }

  public static void extractTarGz(Path tarGzFile, Path targetDir, Platform platform) throws IOException {
    String prefixToRemove = null;
    var outputBaseDir = targetDir;

    if (platform == Platform.MACOS) {
      prefixToRemove = getMacExtractionPrefix(tarGzFile);

      var jdkRootDirName = prefixToRemove.split("/")[0];
      outputBaseDir = targetDir.resolve(jdkRootDirName);
      Files.createDirectories(outputBaseDir);
    }

    try (var inputStream = Files.newInputStream(tarGzFile);
        var bufferedInputStream = new BufferedInputStream(inputStream);
        var gzipIn = new GzipCompressorInputStream(bufferedInputStream);
        var tarIn = new TarArchiveInputStream(gzipIn)) {

      ArchiveEntry entry;
      while ((entry = tarIn.getNextEntry()) != null) {
        var maybeRelativeEntry = getRelativeEntryName(entry.getName(), prefixToRemove);
        if (maybeRelativeEntry.isEmpty()) {
          continue;
        }

        var outputPath = outputBaseDir.resolve(maybeRelativeEntry.get()).normalize();
        validatePath(outputPath, targetDir);

        writeEntryToDisk(entry, tarIn, outputPath);
      }
    }
  }

  public static Optional<Path> optionalExtractedJdkPath(Path applicationDataJdkDirectoryPath, String versionPrefix) throws IOException {
    try (var paths = Files.list(applicationDataJdkDirectoryPath)) {
      return paths
        .filter(Files::isDirectory)
        .filter(path -> path.getFileName().toString().startsWith("jdk" + versionPrefix) || path.getFileName().toString().startsWith("jdk-" + versionPrefix))
        .max(Comparator.comparing(FileUtilities::extractVersionFromJdkFolder));
    }
  }

  public static JavaVersion extractVersionFromJdkFolder(Path folder) {
    var name = folder.getFileName().toString().replace("jdk-", "").replace("jdk", "");
    return JavaVersion.parse(name);
  }

  public static JarFile createJarFile(Path path) throws IOException {
    return new JarFile(path.toFile());
  }

  public static boolean jarContainsModuleInfoClass(Path jarFilePath) {
    try (var jarFile = new JarFile(jarFilePath.toFile())) {
      return jarFile.stream().anyMatch(entityInsideOfJarFile -> entityInsideOfJarFile.getName().equals("module-info.class") || entityInsideOfJarFile.getName().endsWith("/module-info.class"));
    } catch (Exception e) {
      log.warn("Could not check if JAR contains module-info.class files.");
      log.debug("Could not check if JAR contains module-info.class files.", e);
      return false;
    }
  }

  public static Path createJarFileWithoutModuleInfoClass(Path jarFilePath, Path applicationDataDirectory) throws IOException {
    var cleanedJar = applicationDataDirectory.resolve(CLEANED_JAR_FILE_JAR);
    log.info("Create JAR file {} without module-info.class...", cleanedJar);

    try (var jarInputStream = new JarInputStream(Files.newInputStream(jarFilePath));
        var jarOutputStream =
            (jarInputStream.getManifest() != null) ? new JarOutputStream(Files.newOutputStream(cleanedJar), jarInputStream.getManifest()) : new JarOutputStream(Files.newOutputStream(cleanedJar))) {

      JarEntry entry;
      while ((entry = jarInputStream.getNextJarEntry()) != null) {
        var name = entry.getName();
        if (name.equals("module-info.class") || name.endsWith("/module-info.class")) {
          continue;
        }

        var newEntry = new JarEntry(entry);
        jarOutputStream.putNextEntry(newEntry);

        jarInputStream.transferTo(jarOutputStream);

        jarOutputStream.closeEntry();
      }
    }

    return cleanedJar;
  }

  public static Path copyJdkToBundleDirectory(Target target, Path applicationDataDirectory, Path extractedJdkPath, VersionData versionData) {
    var bundleDirectoryPath =
        applicationDataDirectory.resolve(APPLICATION_DATA_BUNDLE_DIRECTORY).resolve(target.getPlatform().getValue()).resolve(target.getArchitecture().getValue()).resolve(APPLICATION_DATA_JAVA_DIRECTORY);
    log.info("Copy JDK to bundle for {} with architecture {} to {}", target.getPlatform(), target.getArchitecture(), bundleDirectoryPath);
    try {
      Files.createDirectories(bundleDirectoryPath.getParent());

      var jrePath = versionData.getMajor() > 8 ? extractedJdkPath : extractedJdkPath.resolve("jre");
      FileUtilities.copyRecursively(jrePath, bundleDirectoryPath);
      return bundleDirectoryPath.getParent();
    } catch (Exception e) {
      var message = format("Could not copy JDK to bundle forlder for %s with architecture %s. Skipping further procssing for this combination. Reason: %s", target.getPlatform(), target.getArchitecture(), e.getMessage());
      log.warn(message);
      log.debug(message, e);
      return null;
    }
  }

  public static String calculateSha256Hash(Path file) throws IOException {
    try {
      var data = Files.readAllBytes(file);
      var hash = MessageDigest.getInstance("SHA-256").digest(data);
      return new BigInteger(1, hash).toString(16);
    } catch (NoSuchAlgorithmException e) {
      return "";
    }
  }

  private static String getMacExtractionPrefix(Path tarGzFile) throws IOException {
    var requiredSuffix = "Contents/Home/";
    try (var inputStream = Files.newInputStream(tarGzFile);
        var bufferedInputStream = new BufferedInputStream(inputStream);
        var gzipCompressorInputStream = new GzipCompressorInputStream(bufferedInputStream);
        var tarArchiveInputStream = new TarArchiveInputStream(gzipCompressorInputStream)) {

      ArchiveEntry archiveEntry;
      while ((archiveEntry = tarArchiveInputStream.getNextEntry()) != null) {
        var name = archiveEntry.getName();
        var index = name.indexOf(requiredSuffix);
        if (index > 0) {
          return name.substring(0, index + requiredSuffix.length());
        }
      }
    }

    throw new IOException("No Contents/Home/ folder found in macOS JDK archive");
  }

  private static Optional<String> getRelativeEntryName(String entryName, String prefixToRemove) {
    if (prefixToRemove != null) {
      if (!entryName.startsWith(prefixToRemove)) {
        return Optional.empty();
      }
      entryName = entryName.substring(prefixToRemove.length());
    }

    return entryName.isBlank() ? Optional.empty() : Optional.of(entryName);
  }

  private static void validatePath(Path outputPath, Path targetDir) throws IOException {
    if (!outputPath.startsWith(targetDir)) {
      throw new IOException("Path traversal detected: " + outputPath);
    }
  }

  private static void writeEntryToDisk(ArchiveEntry entry, TarArchiveInputStream tarIn, Path outputPath) throws IOException {
    if (entry.isDirectory()) {
      Files.createDirectories(outputPath);
    } else {
      Files.createDirectories(outputPath.getParent());
      try (var out = Files.newOutputStream(outputPath)) {
        tarIn.transferTo(out);
      }
      if (!System.getProperty("os.name").toLowerCase().contains("win")) {
        setPermissions(outputPath, ((TarArchiveEntry) entry).getMode());
      }
    }
  }

  private static void setPermissions(Path file, int mode) {
    try {
      var perms = convertModeToPosix(mode);
      Files.setPosixFilePermissions(file, perms);
    } catch (UnsupportedOperationException e) {
      log.warn("POSIX permissions are not supported. {}", e.getMessage());
    } catch (IOException e) {
      log.error("Exception occured during set of file permissions for{}", file, e);
    }
  }

  protected static Set<PosixFilePermission> convertModeToPosix(int mode) {
    var permString = String.format("%03o", mode & 0777);

    return PosixFilePermissions.fromString(mapModeToPermissions(permString.charAt(0)) + mapModeToPermissions(permString.charAt(1)) + mapModeToPermissions(permString.charAt(2)));
  }

  private static String mapModeToPermissions(char modeChar) {
    return switch (modeChar) {
      case '7' -> "rwx";
      case '6' -> "rw-";
      case '5' -> "r-x";
      case '4' -> "r--";
      case '3' -> "-wx";
      case '2' -> "-w-";
      case '1' -> "--x";
      default -> "---";
    };
  }
}

