package de.dddns.kirbylink.warp4j.utilities;

import de.dddns.kirbylink.warp4j.config.Warp4JCommand;
import de.dddns.kirbylink.warp4j.config.Warp4JCommand.Warp4JCommandConfiguration;
import de.dddns.kirbylink.warp4j.service.CachedJdkCollectorService;
import de.dddns.kirbylink.warp4j.service.DownloadService;
import de.dddns.kirbylink.warp4j.service.FileService;
import de.dddns.kirbylink.warp4j.service.OptimizerService;
import de.dddns.kirbylink.warp4j.service.Warp4JService;
import de.dddns.kirbylink.warp4j.service.WarpService;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ComponentFactory {
  public static Warp4JCommand createWarp4JCommand() {
    return new Warp4JCommand();
  }

  public static DownloadUtilities createDownloadUtilities() {
    return new DownloadUtilities();
  }

  public static DownloadService createDownloadService(DownloadUtilities downloadUtilities, AdoptiumClient adoptiumClient) {
    return new DownloadService(downloadUtilities, adoptiumClient);
  }

  public static CachedJdkCollectorService createCachedJdkCollectorService() {
    return new CachedJdkCollectorService();
  }

  public static AdoptiumClient createAdoptiumClient() {
    return new AdoptiumClient();
  }

  public static FileService createFileService() {
    return new FileService();
  }

  public static ProcessRunner createProcessRunner() {
    return new DefaultProcessRunner();
  }

  public static ProcessExecutor createProcessExecutor(ProcessRunner processRunner) {
    return new ProcessExecutor(processRunner);
  }

  public static WarpPacker createWarpPacker(ProcessExecutor processExecutor) {
    return new WarpPacker(processExecutor);
  }

  public static JdepAnalyzer createJdepAnalyzer(ProcessExecutor processExecutor) {
    return new JdepAnalyzer(processExecutor);
  }

  public static JLinkOptimizer createJLinkOptimizer(ProcessExecutor processExecutor) {
    return new JLinkOptimizer(processExecutor);
  }

  public static OptimizerService createOptimizerService(JdepAnalyzer jdepAnalyzer, JLinkOptimizer jlinkOptimizer, Warp4JCommandConfiguration warp4jCommandConfiguration) {
    return new OptimizerService(jdepAnalyzer, jlinkOptimizer, warp4jCommandConfiguration);
  }

  public static WarpService createWarpService(WarpPacker warpPacker) {
    return new WarpService(warpPacker);
  }

  public static Warp4JService createWarp4JService(DownloadService downloadService, CachedJdkCollectorService jdkCollectorService, FileService fileService, OptimizerService optimizerService, WarpService warpService) {
    return new Warp4JService(downloadService, jdkCollectorService, fileService, optimizerService, warpService);
  }
}
