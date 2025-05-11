package de.dddns.kirbylink.warp4j;

import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import org.junit.jupiter.api.Test;

class Warp4JApplicationIntegrationTest {

  @Test
  void testWarp4JApplication() {
    // Given

    // When
    var throwAbleMethod = catchThrowable(Warp4JApplication::new);

    // Then
    assertThat(throwAbleMethod).isNull();

  }

  @Test
  void testMainAndPrintHelp() {
    // Given

    // When
    var throwAbleMethod = catchThrowable(() -> {
      Warp4JApplication.main(new String[] {"-h"});
    });

    // Then
    assertThat(throwAbleMethod).isNull();
  }
}
