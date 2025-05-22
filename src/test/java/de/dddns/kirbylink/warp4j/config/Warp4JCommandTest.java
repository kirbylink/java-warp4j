package de.dddns.kirbylink.warp4j.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Set;
import java.util.stream.Stream;
import javax.naming.NoPermissionException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;
import de.dddns.kirbylink.warp4j.config.Warp4JCommand.Warp4JCommandConfiguration;
import de.dddns.kirbylink.warp4j.model.Architecture;
import de.dddns.kirbylink.warp4j.model.Platform;
import de.dddns.kirbylink.warp4j.model.Target;
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
  
  @Nested
  @DisplayName("Test for Warp4JCommandConfiguration")
  class TestWarp4JCommandConfiguration {
    @Test
    void testGetAllSelectedTargets_WhenNothingIsSelected_ThenAllTargetsReturned() {
      // Given
      var warp4JCommandConfiguration = Warp4JCommandConfiguration.builder().build();
      var expectedTargets = Set.of(
          new Target(Platform.LINUX, Architecture.X64),
          new Target(Platform.LINUX, Architecture.AARCH64),
          new Target(Platform.MACOS, Architecture.X64),
          new Target(Platform.MACOS, Architecture.AARCH64),
          new Target(Platform.WINDOWS, Architecture.X64),
          new Target(Platform.WINDOWS, Architecture.AARCH64)
          );
      
      // When
      var actualTargets = warp4JCommandConfiguration.getAllSelectedTargets();
      
      // Then
      assertThat(actualTargets).isEqualTo(expectedTargets);
    }
    
    @ParameterizedTest
    @MethodSource("provideArchitectureAndPlatforms")
    void testGetAllSelectedTargets_WhenTargetsAreSelected_ThenTargetsReturned(boolean isLinux, boolean isMacos, boolean isWindows, 
        boolean isLinuxX64, boolean isLinuxAarch64, boolean isMacosX64, boolean isMacosAarch64,
        boolean isWindows64, boolean isWindowsAarch64, String expectedArchitecture, Set<Target> expectedTargets) {
      // Given
      var warp4JCommandConfiguration = Warp4JCommandConfiguration.builder()
          .linux(isLinux)
          .linuxAarch64(isLinuxAarch64)
          .linuxX64(isLinuxX64)
          .macos(isMacos)
          .macosAarch64(isMacosAarch64)
          .macosX64(isMacosX64)
          .windows(isWindows)
          .windowsAarch64(isWindowsAarch64)
          .windowsX64(isWindows64)
          .architecture(expectedArchitecture)
          .build();
      
      // When
      var actualTargets = warp4JCommandConfiguration.getAllSelectedTargets();
      
      // Then
      assertThat(actualTargets).isEqualTo(expectedTargets);
    }
    
    static Stream<Arguments> provideArchitectureAndPlatforms() {
      return Stream.of(
          Arguments.of(false, false, false, false, false, false, false, false, false, "", Set.of(new Target(Platform.LINUX, Architecture.X64), new Target(Platform.LINUX, Architecture.AARCH64), new Target(Platform.MACOS, Architecture.X64), new Target(Platform.MACOS, Architecture.AARCH64), new Target(Platform.WINDOWS, Architecture.X64),new Target(Platform.WINDOWS, Architecture.AARCH64))),
          Arguments.of(true, false, false, false, false, false, false, false, false, "", Set.of(new Target(Platform.LINUX, Architecture.X64), new Target(Platform.LINUX, Architecture.AARCH64))),
          Arguments.of(false, false, false, false, true, false, true, false, false, "", Set.of(new Target(Platform.LINUX, Architecture.AARCH64), new Target(Platform.MACOS, Architecture.AARCH64))),
          Arguments.of(true, true, true, false, false, false, false, false, false, "x64", Set.of(new Target(Platform.LINUX, Architecture.X64), new Target(Platform.MACOS, Architecture.X64), new Target(Platform.WINDOWS, Architecture.X64))),
          Arguments.of(true, false, false, false, false, false, false, false, true, "x64", Set.of(new Target(Platform.LINUX, Architecture.X64), new Target(Platform.WINDOWS, Architecture.AARCH64))),
          Arguments.of(true, true, true, true, false, true, false, true, false, "aarch64", Set.of(new Target(Platform.LINUX, Architecture.X64), new Target(Platform.LINUX, Architecture.AARCH64), new Target(Platform.MACOS, Architecture.X64), new Target(Platform.MACOS, Architecture.AARCH64), new Target(Platform.WINDOWS, Architecture.X64), new Target(Platform.WINDOWS, Architecture.AARCH64)))
      );
    }
  }
}
