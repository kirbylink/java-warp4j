package de.dddns.kirbylink.warp4j.utilities;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class JdepAnalyzer {
  private final ProcessExecutor processExecutor;

  public List<String> analyzeDependencies(String jdepsPath, List<Path> classPaths, Path jarFilePath, String javaVersionBase) throws IOException, InterruptedException {

    if (!javaVersionBase.matches("\\d+")) {
      throw new IllegalArgumentException("Invalid Java version: " + javaVersionBase);
    }

    var separator = System.getProperty("path.separator");
    var classPath = classPaths.stream()
        .map(path -> path.toAbsolutePath().toString())
        .collect(Collectors.joining(separator));

    List<String> command = new ArrayList<> (List.of(
        jdepsPath,
        "--list-deps",
        jarFilePath.toAbsolutePath().toString()));

    try (var jarFile = FileUtilities.createJarFile(jarFilePath)) {
      if (jarFile.isMultiRelease()) {
        log.debug("Add '--multi-release' argument to jdeps call.");
        command.add(2, "--multi-release");
        command.add(3, javaVersionBase);
      }
    }

    if (Integer.valueOf(javaVersionBase) >= 11) {
      log.debug("Add '--ignore-missing-deps' argument to jdeps call.");
      command.add(2, "--ignore-missing-deps");
    }

    if (!classPath.isBlank()) {
      log.debug("Add '--class-path' argument to jdeps call.");
      var size = command.size();
      command.add(size - 1, "--class-path");
      command.add(size, classPath);
    }

    log.debug("Executing command: {}", String.join(" ", command));

    return processExecutor.executeAndParse(command, output -> output.stream()
        .map(String::trim)
        .filter(line -> !line.isEmpty())
        .filter(dependency -> !dependency.endsWith(".jar") && !dependency.contains("JDK removed internal API"))
        .map(dependency -> dependency.contains("/") ? dependency.substring(0, dependency.indexOf('/')) : dependency)
        .distinct()
        .toList());
  }
}