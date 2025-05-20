package de.dddns.kirbylink.warp4j.utilities;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import de.dddns.kirbylink.warp4j.model.Target;
import de.dddns.kirbylink.warp4j.model.Warp4JRuntimeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class WarpPacker {

  private final ProcessExecutor processExecutor;

  public void warpApplication(Path warpPackerPath, Target target, Path bundlePath, String scriptName, Path outputPath, String prefix) throws IOException, InterruptedException {
    List<String> command = new ArrayList<>(List.of(warpPackerPath.toString(), "pack",
        "--arch", target.getPlatform().toString().toLowerCase() + "-" + target.getArchitecture().getValue(),
        "--input-dir", bundlePath.toString(),
        "--exec", scriptName,
        "--unique-id",
        "--output", outputPath.toString()));

    if (null != prefix && !prefix.isBlank()) {
      command.add("--prefix");
      command.add(prefix);
    }

    log.debug("Executing command: {}", String.join(" ", command));

    var result = processExecutor.execute(command);

    log.debug(result.toString());

    if (result.getExitCode() != 0) {
      log.error("Failed to pack application. Result: {}", String.join("\n", result.getOutput()));
      throw new Warp4JRuntimeException("Failed to optimize runtime");
    }
  }
}