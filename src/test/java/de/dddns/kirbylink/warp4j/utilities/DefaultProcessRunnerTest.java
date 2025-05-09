package de.dddns.kirbylink.warp4j.utilities;

import static org.assertj.core.api.Assertions.assertThat;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;

class DefaultProcessRunnerTest {

  @Test
  void testExecuteJavaVersion() throws IOException, InterruptedException {
    var runner = new DefaultProcessRunner();
    var result = runner.execute(List.of("java", "-version"));

    assertThat(result.getExitCode()).isZero();
    assertThat(result.getOutput()).isEmpty();
    assertThat(result.getErrorOutput()).isNotEmpty();
  }
}