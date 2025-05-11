package de.dddns.kirbylink.warp4j.utilities;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.file.Path;

public class DownloadUtilities {
  public void downloadFile(URL url, Path targetPath) throws IOException {
    try (var readableByteChannel = Channels.newChannel(url.openStream());
        var fileOutputStream = new FileOutputStream(targetPath.toString())) {
      var fileChannel = fileOutputStream.getChannel();
      fileChannel.transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
    }
  }
}
