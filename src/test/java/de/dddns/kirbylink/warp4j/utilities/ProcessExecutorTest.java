package de.dddns.kirbylink.warp4j.utilities;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.io.IOException;
import java.util.List;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

class ProcessExecutorTest {

  @Test
  void testExecuteSuccessfulCommand() throws IOException, InterruptedException {
    // Given
    var mockRunner = mock(ProcessRunner.class);
    when(mockRunner.execute(List.of("echo", "Hello"))).thenReturn(new ProcessExecutor.ExecutionResult(0, List.of("Hello"), List.of()));

    var executor = new ProcessExecutor(mockRunner);

    // When
    var result = executor.execute(List.of("echo", "Hello"));

    // Then
    assertThat(result.getExitCode()).isZero();
    assertThat(result.getOutput()).containsExactly("Hello");
    assertThat(result.getErrorOutput()).isEmpty();
  }

  @Test
  void testExecuteUnknownCommandFails() throws IOException, InterruptedException {
    // Given
    var mockRunner = mock(ProcessRunner.class);
    when(mockRunner.execute(List.of("unknowncommand"))).thenThrow(new IOException("Unknown command"));

    // When
    var executor = new ProcessExecutor(mockRunner);

    // Then
    assertThatThrownBy(() -> executor.execute(List.of("unknowncommand"))).isInstanceOf(IOException.class).hasMessageContaining("Unknown command");
  }

  @Test
  void testExecuteCommandWithErrorOutput() throws IOException, InterruptedException {
    // Given
    var mockRunner = mock(ProcessRunner.class);
    when(mockRunner.execute(List.of("failingCommand"))).thenReturn(new ProcessExecutor.ExecutionResult(1, List.of(), List.of("Error occurred")));

    var executor = new ProcessExecutor(mockRunner);

    // When
    var result = executor.execute(List.of("failingCommand"));

    // Then
    assertThat(result.getExitCode()).isNotZero();
    assertThat(result.getOutput()).isEmpty();
    assertThat(result.getErrorOutput()).containsExactly("Error occurred");
  }

  @Test
  void testExecuteAndParse() throws IOException, InterruptedException {
    // Given
    var mockRunner = mock(ProcessRunner.class);

    when(mockRunner.executeAndParse(eq(List.of("echo", "one two three")), any())).thenAnswer(invocation -> {
      List<String> output = List.of("one two three");
      @SuppressWarnings("unchecked")
      var parser = (Function<List<String>, List<String>>) invocation.getArgument(1);

      return parser.apply(output);
    });

    var executor = new ProcessExecutor(mockRunner);

    // When
    List<String> words = executor.executeAndParse(List.of("echo", "one two three"), output -> List.of(output.get(0).split(" ")));

    // Then
    assertThat(words).containsExactly("one", "two", "three");
  }

}

