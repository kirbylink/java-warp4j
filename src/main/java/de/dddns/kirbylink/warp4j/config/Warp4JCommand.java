package de.dddns.kirbylink.warp4j.config;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import javax.naming.NoPermissionException;
import de.dddns.kirbylink.warp4j.utilities.ComponentFactory;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Slf4j
@RequiredArgsConstructor
@Command(name = "warp4j", mixinStandardHelpOptions = true, version = "1.0", description = "Turn JAR into a self-contained executable")
public class Warp4JCommand implements Callable<Integer> {

  @Option(names = {"-j", "--java-version"}, description = "Override JDK/JRE version (default: 17)")
  private String javaVersion = "17";

  @Option(names = {"-cp", "--class-path"}, description = "Additional classpaths for jdeps seperated by comma")
  private String classPath;

  @Option(names = {"--spring-boot"}, description = "Extract class-path from JAR to fetch BOOT-INF/lib/ modules")
  private boolean isSpringBoot;

  @Option(names = {"-o", "--output"}, description = "Output directory (default: ./warped)")
  private Path outputDirectoryPath = Path.of("./warped");

  @Option(names = {"-p", "--prefix"}, description = "Prefix for extracted application folder")
  private String prefix;

  @Option(names = {"--optimize"}, description = "Use optimized JRE instead of JDK")
  private boolean isOptimize;

  @Option(names = {"--add-modules"}, description = " A list of additional java modules that should be added to the optimized JDK. Separate each module with commas and no spaces")
  private String additionalModules = "";

  @Option(names = {"--linux"}, description = "Create binary for Linux")
  private boolean isLinux;

  @Option(names = {"--macos"}, description = "Create binary for macOS")
  private boolean isMacos;

  @Option(names = {"--windows"}, description = "Create binary for Windows")
  private boolean isWindows;

  @Option(names = {"--arch"}, description = "Target architecture (x64 or aarch64) Default: both")
  private String architecture;

  @Option(names = {"-s", "--silent"}, description = "Use javaw.exe instead of java.exe (Windows only)")
  private boolean isSilent;

  @Option(names = {"--pull"}, description = "Check if more recent JDK/JRE distro is available. By default latest cached version that matches")
  private boolean isPull;

  @Option(names = {"-h", "--help"}, usageHelp = true, description = "Show this message and exit")
  private boolean helpRequested;

  @Option(names = {"--jar"}, description = "JAR file to be converted", required = true)
  private Path jarFilePath;

  @Option(names = {"--jdk"}, description = "Path to JDK that contains the binaries jdep(.exe) and jlink(.exe)", required = false)
  private String jdkPath = "";

  @Option(names = {"-v", "--verbose"}, description = "Enable verbose logging")
  boolean verbose;


  @Override
  public Integer call() {
    try {
      var warp4JCommandConfiguration = Warp4JCommandConfiguration.builder()
          .additionalModules(additionalModules)
          .architecture(architecture)
          .springBoot(isSpringBoot)
          .classPath(collectClassPath(classPath))
          .jarFilePath(jarFilePath)
          .jdkPath(jdkPath)
          .javaVersion(javaVersion)
          .linux(isLinux)
          .macos(isMacos)
          .optimize(isOptimize)
          .outputDirectoryPath(outputDirectoryPath)
          .prefix(prefix)
          .pull(isPull)
          .silent(isSilent)
          .verbose(verbose)
          .windows(isWindows)
          .build();

      var downloadUtilities = ComponentFactory.createDownloadUtilities();
      var adoptiumClient = ComponentFactory.createAdoptiumClient();
      var downloadService = ComponentFactory.createDownloadService(downloadUtilities, adoptiumClient);
      var jdkCollectorService = ComponentFactory.createCachedJdkCollectorService();
      var fileService = ComponentFactory.createFileService();
      var processRunner = ComponentFactory.createProcessRunner();
      var processExecutor = ComponentFactory.createProcessExecutor(processRunner);
      var jdepAnalyzer = ComponentFactory.createJdepAnalyzer(processExecutor);
      var jlinkOptimizer = ComponentFactory.createJLinkOptimizer(processExecutor);
      var optimizerService = ComponentFactory.createOptimizerService(jdepAnalyzer, jlinkOptimizer, warp4JCommandConfiguration);
      var warpPacker = ComponentFactory.createWarpPacker(processExecutor);
      var warpService = ComponentFactory.createWarpService(warpPacker);
      var warp4JService = ComponentFactory.createWarp4JService(downloadService, jdkCollectorService, fileService, optimizerService, warpService);


      return warp4JService.createExecutableJarFile(warp4JCommandConfiguration);
    } catch (InterruptedException e) {
      log.warn(e.getMessage());
      log.debug(e.getMessage(), e);
      Thread.currentThread().interrupt();
      return 70;
    } catch (UnsupportedOperationException e) {
      log.warn(e.getMessage());
      log.debug(e.getMessage(), e);
      return 71;
    } catch (FileNotFoundException e) {
      log.warn(e.getMessage());
      log.debug(e.getMessage(), e);
      return 72;
    } catch (IOException e) {
      log.warn(e.getMessage());
      log.debug(e.getMessage(), e);
      return 74;
    } catch (NoPermissionException e) {
      log.warn(e.getMessage());
      log.debug(e.getMessage(), e);
      return 77;
    }
  }

  private List<Path> collectClassPath(String classPath) {
    var optionalClassPath = Optional.ofNullable(classPath);

    if (optionalClassPath.isEmpty()) {
      return Collections.emptyList();
    }

    return Arrays.stream(optionalClassPath.get().split(","))
      .map(Path::of)
      .toList();
  }

  @Getter
  @Builder
  @ToString
  public static class Warp4JCommandConfiguration {
    private final String javaVersion;
    private final List<Path> classPath;
    private final boolean springBoot;
    private final Path outputDirectoryPath;
    private final String prefix;
    private final boolean optimize;
    private final String additionalModules;
    private final boolean linux;
    private final boolean macos;
    private final boolean windows;
    private final String architecture;
    private final boolean silent;
    private final boolean pull;
    private final Path jarFilePath;
    private final String jdkPath;
    private final boolean verbose;
  }
}

