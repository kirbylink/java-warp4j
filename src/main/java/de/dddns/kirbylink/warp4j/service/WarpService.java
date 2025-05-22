package de.dddns.kirbylink.warp4j.service;

import static java.lang.String.format;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import de.dddns.kirbylink.warp4j.model.Target;
import de.dddns.kirbylink.warp4j.utilities.FileUtilities;
import de.dddns.kirbylink.warp4j.utilities.WarpPacker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class WarpService {

  private final WarpPacker warpPacker;

  public boolean warpBundle(Target target, Path bundleDirectoryPath, Path bundleScriptPath, Path outputFilePath, Path warpPackerPath, String prefix) {
    log.info("Warp bundle with warp-packer for {} with architecture {} to {}", target.getPlatform(), target.getArchitecture(), outputFilePath);
    try {
      Files.createDirectories(outputFilePath.getParent());
      FileUtilities.deleteRecursively(outputFilePath);
      warpPacker.warpApplication(warpPackerPath, target, bundleDirectoryPath, bundleScriptPath.getFileName().toString(), outputFilePath, prefix);
      return true;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      var message = format("Could not warp bundle with warp-packer for %s with architecture %s. Skipping further processing for this combination. Reason: %s", target.getPlatform(), target.getArchitecture(), e.getMessage());
      log.warn(message);
      log.debug(message, e);
      return false;
    } catch (IOException e) {
      var message = format("Could not warp bundle with warp-packer for %s with architecture %s. Skipping further processing for this combination. Reason: %s", target.getPlatform(), target.getArchitecture(), e.getMessage());
      log.warn(message);
      log.debug(message, e);
      return false;
    }
  }
}
