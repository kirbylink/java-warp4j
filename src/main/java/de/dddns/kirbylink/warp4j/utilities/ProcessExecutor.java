package de.dddns.kirbylink.warp4j.utilities;

import java.io.IOException;
import java.util.List;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@RequiredArgsConstructor
public class ProcessExecutor {
  private final ProcessRunner processRunner;

  public ExecutionResult execute(List<String> command) throws IOException, InterruptedException {
    return processRunner.execute(command);
  }

  public <T> T executeAndParse(List<String> command, Function<List<String>, T> parser) throws IOException, InterruptedException {
    return processRunner.executeAndParse(command, parser);
  }

  @ToString
  @RequiredArgsConstructor
  public static class ExecutionResult {
    private final int exitCode;
    private final List<String> output;
    private final List<String> errorOutput;

    public int getExitCode() {
      return exitCode;
    }

    public List<String> getOutput() {
      return output;
    }

    public List<String> getErrorOutput() {
      return errorOutput;
    }
  }
}
