package de.dddns.kirbylink.warp4j.utilities;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import de.dddns.kirbylink.warp4j.config.Warp4JCommand;
import de.dddns.kirbylink.warp4j.service.CachedJdkCollectorService;
import de.dddns.kirbylink.warp4j.service.DownloadService;
import de.dddns.kirbylink.warp4j.service.FileService;
import de.dddns.kirbylink.warp4j.service.OptimizerService;
import de.dddns.kirbylink.warp4j.service.Warp4JService;
import de.dddns.kirbylink.warp4j.service.WarpService;

class ComponentFactoryTest {

  @Test
  void createWarp4JCommand_returnsCorrectType() {
    // Given

    // When
    var command = ComponentFactory.createWarp4JCommand();

    // Then
    assertThat(command).isInstanceOf(Warp4JCommand.class);
  }

  @Test
  void createDownloadUtilities_returnsCorrectType() {
    // Given

    // When
    var util = ComponentFactory.createDownloadUtilities();

    // Then
    assertThat(util).isInstanceOf(DownloadUtilities.class);
  }

  @Test
  void createAdoptiumClient_returnsCorrectType() {
    // Given

    // When
    var client = ComponentFactory.createAdoptiumClient();

    // Then
    assertThat(client).isInstanceOf(AdoptiumClient.class);
  }

  @Test
  void createDownloadService_returnsCorrectType() {
    // Given

    // When
    var downloadService = ComponentFactory.createDownloadService(new DownloadUtilities(), new AdoptiumClient());

    // Then
    assertThat(downloadService).isInstanceOf(DownloadService.class);
  }

  @Test
  void createCachedJdkCollectorService_returnsCorrectType() {
    // Given

    // When
    var collector = ComponentFactory.createCachedJdkCollectorService();

    // Then
    assertThat(collector).isInstanceOf(CachedJdkCollectorService.class);
  }

  @Test
  void createFileService_returnsCorrectType() {
    // Given

    // When
    var fileService = ComponentFactory.createFileService();

    // Then
    assertThat(fileService).isInstanceOf(FileService.class);
  }

  @Test
  void createProcessRunner_returnsCorrectType() {
    // Given

    // When
    var runner = ComponentFactory.createProcessRunner();

    // Then
    assertThat(runner).isInstanceOf(DefaultProcessRunner.class);
  }

  @Test
  void createProcessExecutor_returnsCorrectType() {
    // Given

    // When
    var executor = ComponentFactory.createProcessExecutor(new DefaultProcessRunner());

    // Then
    assertThat(executor).isInstanceOf(ProcessExecutor.class);
  }

  @Test
  void createWarpPacker_returnsCorrectType() {
    // Given

    // When
    var packer = ComponentFactory.createWarpPacker(new ProcessExecutor(new DefaultProcessRunner()));

    // Then
    assertThat(packer).isInstanceOf(WarpPacker.class);
  }

  @Test
  void createJdepAnalyzer_returnsCorrectType() {
    // Given

    // When
    var analyzer = ComponentFactory.createJdepAnalyzer(new ProcessExecutor(new DefaultProcessRunner()));

    // Then
    assertThat(analyzer).isInstanceOf(JdepAnalyzer.class);
  }

  @Test
  void createJLinkOptimizer_returnsCorrectType() {
    // Given

    // When
    var optimizer = ComponentFactory.createJLinkOptimizer(new ProcessExecutor(new DefaultProcessRunner()));

    // Then
    assertThat(optimizer).isInstanceOf(JLinkOptimizer.class);
  }

  @Test
  void createOptimizerService_returnsCorrectType() {
    // Given

    // When
    var optimizerService = ComponentFactory.createOptimizerService(null, null, null);

    // Then
    assertThat(optimizerService).isInstanceOf(OptimizerService.class);
  }

  @Test
  void createWarpService_returnsCorrectType() {
    // Given

    // When
    var warpService = ComponentFactory.createWarpService(new WarpPacker(new ProcessExecutor(new DefaultProcessRunner())));

    // Then
    assertThat(warpService).isInstanceOf(WarpService.class);
  }

  @Test
  void createWarp4JService_returnsCorrectType() {
    // Given

    // When
    var service = ComponentFactory.createWarp4JService(null, null, null, null, null);

    // Then
    assertThat(service).isInstanceOf(Warp4JService.class);
  }
}

