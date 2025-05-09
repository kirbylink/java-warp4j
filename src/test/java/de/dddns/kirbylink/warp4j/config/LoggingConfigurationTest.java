package de.dddns.kirbylink.warp4j.config;

import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import org.junit.jupiter.api.Test;
import ch.qos.logback.classic.Level;

class LoggingConfigurationTest {

  @Test
  void testSetRootLogLevel_WhenLevelIsGiven_ThenNoExceptionIsThrown() {
    // Given
    var level = Level.DEBUG;

    // When
    var throwAbleMethod = catchThrowable(() -> {
      LoggingConfiguration.setRootLogLevel(level);
    });

    // Then
    assertThat(throwAbleMethod).isNull();
  }
}
