package de.dddns.kirbylink.warp4j.utilities;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DefaultProcessRunner implements ProcessRunner {

  @Override
  public ProcessExecutor.ExecutionResult execute(List<String> command) throws IOException, InterruptedException {
    var processBuilder = new ProcessBuilder(command);
    processBuilder.redirectErrorStream(false);
    var process = processBuilder.start();

    var output = readStream(process.getInputStream());
    var errorOutput = readStream(process.getErrorStream());

    var exitCode = process.waitFor();
    var processExceutionResult = new ProcessExecutor.ExecutionResult(exitCode, output, errorOutput);
    log.debug("Result of process excecution: {}", processExceutionResult);

    return processExceutionResult;
  }

  @Override
  public <T> T executeAndParse(List<String> command, Function<List<String>, T> parser) throws IOException, InterruptedException {
    var result = execute(command);
    return parser.apply(result.getOutput());
  }

  private List<String> readStream(java.io.InputStream inputStream) throws IOException {
    try (var reader = new BufferedReader(new InputStreamReader(inputStream))) {
      return reader.lines().toList();
    }
  }
}
