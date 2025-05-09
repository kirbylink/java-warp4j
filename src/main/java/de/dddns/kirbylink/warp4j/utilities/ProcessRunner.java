package de.dddns.kirbylink.warp4j.utilities;

import java.io.IOException;
import java.util.List;
import java.util.function.Function;

public interface ProcessRunner {
  ProcessExecutor.ExecutionResult execute(List<String> command) throws IOException, InterruptedException;

  <T> T executeAndParse(List<String> command, Function<List<String>, T> parser) throws IOException, InterruptedException;
}
