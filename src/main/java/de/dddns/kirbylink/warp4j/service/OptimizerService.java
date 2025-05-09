package de.dddns.kirbylink.warp4j.service;

import static java.lang.String.format;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import de.dddns.kirbylink.warp4j.config.Warp4JCommand.Warp4JCommandConfiguration;
import de.dddns.kirbylink.warp4j.model.Architecture;
import de.dddns.kirbylink.warp4j.model.Platform;
import de.dddns.kirbylink.warp4j.model.adoptium.v3.VersionData;
import de.dddns.kirbylink.warp4j.utilities.FileUtilities;
import de.dddns.kirbylink.warp4j.utilities.JLinkOptimizer;
import de.dddns.kirbylink.warp4j.utilities.JdepAnalyzer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class OptimizerService {
  private static final String ERROR_MESSAGE = "Could not create minimal runtime. Skipping further processing for this combination. Reason: %s";
  private final JdepAnalyzer jdepAnalyzer;
  private final JLinkOptimizer jLinkOptimizer;
  private final Warp4JCommandConfiguration warp4jCommandConfiguration;

  private Optional<Path> optionalAutoExtractedJarPath = Optional.empty();

  public List<Path> getClassPath(Path jarFilePath, Path applicationDataDirectoryPath) {
    var classPath = new ArrayList<Path>();
    if (!warp4jCommandConfiguration.getClassPath().isEmpty()) {
      var providedClassPath = warp4jCommandConfiguration.getClassPath();
      providedClassPath.stream().forEach(path -> classPath.addAll(collectAllPathFromClassPath(path)));
    }
    if (warp4jCommandConfiguration.isSpringBoot()) {
      optionalAutoExtractedJarPath = extractJarToGetAdditionalClasspath(jarFilePath, applicationDataDirectoryPath);
      log.debug("Optional Classpath extracted from JAR: {}", optionalAutoExtractedJarPath.isPresent());
      if (optionalAutoExtractedJarPath.isPresent()) {
        var extractedSpringBootLibrariesPath = optionalAutoExtractedJarPath.get().resolve("BOOT-INF").resolve("lib");
        List<Path> extractedSpringBootLibrariesJars;
        try (var streamOfPath = Files.list(extractedSpringBootLibrariesPath)) {
          extractedSpringBootLibrariesJars = streamOfPath.filter(path -> path.toString().endsWith(".jar")).map(Path::toAbsolutePath).toList();
          log.debug("Additional files from the extracted Spring Boot: {}", extractedSpringBootLibrariesJars);
          classPath.addAll(extractedSpringBootLibrariesJars);
        } catch (IOException e) {
          log.warn("Could not extract Spring Boot application. Skip of using the class path from the extracted JAR file.");
        }
      }
    }
    return classPath;
  }

  public String analyzeModules(Path jdepsPath, List<Path> classPath, Path jarFilePath, VersionData versionData) {
    return analyzeDependencies(jdepsPath, classPath, jarFilePath, versionData);
  }

  public Path createOptimizedRuntime(Platform platform, Architecture architecture, Path jmodsPath, VersionData versionData, Path applicationDataDirectoryPath, Path jLinkPath,
      String modules) {
    log.info("Creating minimal runtime for {} with architecture {}", platform, architecture);
    try {
      return jLinkOptimizer.createOptimizedRuntime(platform, architecture, applicationDataDirectoryPath, jmodsPath, versionData, jLinkPath, modules);
    } catch (IOException e) {
      var message = format(ERROR_MESSAGE, e.getMessage());
      log.warn(message);
      log.debug(message, e);
      return null;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      var message = format(ERROR_MESSAGE, e.getMessage());
      log.warn(message);
      log.debug(message, e);
      return null;
    }
  }

  private List<Path> collectAllPathFromClassPath(Path rootDir) {
    try (var stream = Files.walk(rootDir)) {
      return stream.filter(Files::isRegularFile).filter(path -> {
        var name = path.getFileName().toString().toLowerCase();
        return name.endsWith(".jar") || name.endsWith(".class");
      }).map(Path::toAbsolutePath).collect(Collectors.toCollection(ArrayList::new));
    } catch (IOException e) {
      log.warn("Could not collect all path from given class path. Skip class path and try to continue without them.");
      return Collections.emptyList();
    }
  }

  private Optional<Path> extractJarToGetAdditionalClasspath(Path jarFilePath, Path applicationDataDirectory) {
    try {
      log.info("Extract JAR file {} to set additional class path", jarFilePath.getFileName().toString());
      var temporaryDirectoryForExtractedJar = applicationDataDirectory.resolve("extracted-jar");
      FileUtilities.extractZip(jarFilePath, temporaryDirectoryForExtractedJar);
      return Optional.of(temporaryDirectoryForExtractedJar);
    } catch (IOException e) {
      log.warn("Could not extract JAR file. Skipping additional class path.");
      return Optional.empty();
    }
  }

  private String analyzeDependencies(Path jdepsPath, List<Path> classPath, Path jarFilePath, VersionData versionData) {
    List<String> analyzedDependencies;
    try {
      log.info("Analyze jdk and class path for needed modules...");
      analyzedDependencies = jdepAnalyzer.analyzeDependencies(jdepsPath.toString(), classPath, jarFilePath, String.valueOf(versionData.getMajor()));
    } catch (IOException e) {
      var message = format("Could not analyze needed java modules. Skip analyze and continue. Reason: %s", e.getMessage());
      log.warn(message);
      log.debug(message, e);
      return "";
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      var message = format("Could not analyze needed java modules. Skip analyze and continue. Reason: %s", e.getMessage());
      log.warn(message);
      log.debug(message, e);
      return "";
    }

    var analyzedModules = String.join(",", analyzedDependencies);

    if (!analyzedModules.isBlank()) {
      return warp4jCommandConfiguration.getAdditionalModules().isBlank() ? analyzedModules : analyzedModules + "," + warp4jCommandConfiguration.getAdditionalModules();
    }

    return analyzedModules;
  }
}
