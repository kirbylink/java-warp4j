package de.dddns.kirbylink.warp4j.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.stream.Stream;
import javax.naming.NoPermissionException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;
import de.dddns.kirbylink.warp4j.service.Warp4JService;
import de.dddns.kirbylink.warp4j.utilities.ComponentFactory;

class Warp4JCommandTest {
  private MockedStatic<ComponentFactory> componentFactory;
  private Warp4JService mockedWarp4JService;

  private Warp4JCommand warp4jCommand;

  @BeforeEach
  void setUp() {
    componentFactory = mockStatic(ComponentFactory.class);
    mockedWarp4JService = mock(Warp4JService.class);
    componentFactory.when(() -> ComponentFactory.createWarp4JService(any(), any(), any(), any(), any())).thenReturn(mockedWarp4JService);
    warp4jCommand = new Warp4JCommand();
  }

  @AfterEach
  void tearDown() {
    componentFactory.close();
  }

  @Test
  void testCall_WhenNoErrorsOccure_ThenExitCodeIsZero() throws NoPermissionException, IOException, InterruptedException {
    // Given
    when(mockedWarp4JService.createExecutableJarFile(any())).thenReturn(0);

    // When
    var result = warp4jCommand.call();

    // Then
    assertThat(result).isZero();
  }

  @ParameterizedTest
  @MethodSource("provideExceptionAndExitValues")
  void testCall_WhenErrorsOccure_ThenExitCodeIsNotZero(Class<Exception> exception, int expectedExitCode) throws NoPermissionException, IOException, InterruptedException {
    // Given
    doThrow(exception).when(mockedWarp4JService).createExecutableJarFile(any());


    // When
    var actualExitCode = warp4jCommand.call();

    // Then
    assertThat(actualExitCode).isEqualTo(expectedExitCode);
  }

  static Stream<Arguments> provideExceptionAndExitValues() {
    return Stream.of(
        Arguments.of(InterruptedException.class, 70),
        Arguments.of(UnsupportedOperationException.class, 71),
        Arguments.of(FileNotFoundException.class, 72),
        Arguments.of(IOException.class, 74),
        Arguments.of(NoPermissionException.class, 77)
    );
  }
}
