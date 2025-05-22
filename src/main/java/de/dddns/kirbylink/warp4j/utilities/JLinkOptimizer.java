package de.dddns.kirbylink.warp4j.utilities;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import de.dddns.kirbylink.warp4j.model.Platform;
import de.dddns.kirbylink.warp4j.model.Target;
import de.dddns.kirbylink.warp4j.model.Warp4JRuntimeException;
import de.dddns.kirbylink.warp4j.model.adoptium.v3.VersionData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class JLinkOptimizer {

  private final ProcessExecutor processExecutor;

  public Path createOptimizedRuntime(Target target, Path applicationDataDirectory, Path jmodsPath, VersionData versionData, Path jlinkPath, String modules) throws IOException, InterruptedException {
    if (target.getPlatform().equals(Platform.MACOS)) {
      jmodsPath = jmodsPath.resolve("Contents/Home/");
    }
    jmodsPath = jmodsPath.resolve("jmods");

    var stripDebug = (versionData.getMajor() >= 13) ? "strip-java-debug-attributes" : "strip-debug";

    var outputPath = applicationDataDirectory.resolve("bundle").resolve(target.getPlatform().getValue()).resolve(target.getArchitecture().getValue()).resolve("java");

    log.info("Creating minimal runtime for {}...", target.getPlatform());

    List<String> command = List.of(jlinkPath.toString(), "--no-header-files", "--no-man-pages", "--" + stripDebug, "--module-path", jmodsPath.toString(), "--add-modules", modules, "--output", outputPath.toString());

    log.debug("Executing command: {}", String.join(" ", command));

    var result = processExecutor.execute(command);

    log.debug(result.toString());

    if (result.getExitCode() != 0) {
      log.error("Failed create minimal runtime. Result: {}", String.join("\n", result.getOutput()));
      throw new Warp4JRuntimeException("Failed to optimize runtime");
    }
    return outputPath;
  }
}
