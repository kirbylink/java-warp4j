package de.dddns.kirbylink.warp4j.config;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import javax.naming.NoPermissionException;
import de.dddns.kirbylink.warp4j.model.Architecture;
import de.dddns.kirbylink.warp4j.model.Platform;
import de.dddns.kirbylink.warp4j.model.Target;
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

  @Option(names = {"--linux-x64"}, description = "Create binary for Linux with x64 architecture")
  private boolean isLinuxX64;

  @Option(names = {"--linux-aarch64"}, description = "Create binary for Linux with aarch64 architecture")
  private boolean isLinuxAarch64;

  @Option(names = {"--macos-x64"}, description = "Create binary for macOS with x64 architecture")
  private boolean isMacosX64;

  @Option(names = {"--macos-aarch64"}, description = "Create binary for macOS with aarch64 architecture")
  private boolean isMacosAarch64;

  @Option(names = {"--windows-x64"}, description = "Create binary for Windows with x64 architecture")
  private boolean isWindowsX64;

  @Option(names = {"--windows-aarch64"}, description = "Create binary for Windows with aarch64 architecture")
  private boolean isWindowsAarch64;

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
          .linuxX64(isLinuxX64)
          .linuxAarch64(isLinuxAarch64)
          .macos(isMacos)
          .macosX64(isMacosX64)
          .macosAarch64(isMacosAarch64)
          .optimize(isOptimize)
          .outputDirectoryPath(outputDirectoryPath)
          .prefix(prefix)
          .pull(isPull)
          .silent(isSilent)
          .verbose(verbose)
          .windows(isWindows)
          .windowsX64(isWindowsX64)
          .windowsAarch64(isWindowsAarch64)
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
    private final boolean linuxX64;
    private final boolean linuxAarch64;
    private final boolean macos;
    private final boolean macosX64;
    private final boolean macosAarch64;
    private final boolean windows;
    private final boolean windowsX64;
    private final boolean windowsAarch64;
    private final String architecture;
    private final boolean silent;
    private final boolean pull;
    private final Path jarFilePath;
    private final String jdkPath;
    private final boolean verbose;

    public Set<Target> getAllSelectedTargets() {
      Set<Target> targets = new HashSet<>();
      targets.addAll(getExplicitTargets());

      var architectures = getArchitecture() != null && !getArchitecture().isBlank() ? List.of(Architecture.fromValue(getArchitecture())) : List.of(Architecture.values());
      var platforms = new ArrayList<Platform>();
      if (isLinux()) {
        platforms.add(Platform.LINUX);
      }
      if (isMacos()) {
        platforms.add(Platform.MACOS);
      }
      if (isWindows()) {
        platforms.add(Platform.WINDOWS);
      }

      platforms.stream()
          .flatMap(platform -> architectures.stream()
              .map(arch -> new Target(platform, arch)))
          .filter(Warp4JConfiguration::supportedPlatformAndArchitectureByWarp)
          .forEach(targets::add);

      if (targets.isEmpty()) {
        targets.addAll(Warp4JCommandConfiguration.getAllValidTargets());
      }
      return targets;
    }

    private List<Target> getExplicitTargets() {
      List<Target> targets = new ArrayList<>();
      if (isLinuxX64()) {
        targets.add(new Target(Platform.LINUX, Architecture.X64));
      }
      if (isLinuxAarch64()) {
        targets.add(new Target(Platform.LINUX, Architecture.AARCH64));
      }
      if (isMacosX64()) {
        targets.add(new Target(Platform.MACOS, Architecture.X64));
      }
      if (isMacosAarch64()) {
        targets.add(new Target(Platform.MACOS, Architecture.AARCH64));
      }
      if (isWindowsX64()) {
        targets.add(new Target(Platform.WINDOWS, Architecture.X64));
      }
      if (isWindowsAarch64()) {
        targets.add(new Target(Platform.WINDOWS, Architecture.AARCH64));
      }
      return targets;
    }

    private static Set<Target> getAllValidTargets() {
      return Set.of(
          new Target(Platform.LINUX, Architecture.X64),
          new Target(Platform.LINUX, Architecture.AARCH64),
          new Target(Platform.MACOS, Architecture.X64),
          new Target(Platform.MACOS, Architecture.AARCH64),
          new Target(Platform.WINDOWS, Architecture.X64),
          new Target(Platform.WINDOWS, Architecture.AARCH64)
      );
    }
  }
}

