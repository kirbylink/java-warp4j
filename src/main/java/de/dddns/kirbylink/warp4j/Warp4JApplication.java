package de.dddns.kirbylink.warp4j;

import de.dddns.kirbylink.warp4j.utilities.ComponentFactory;
import picocli.CommandLine;

public class Warp4JApplication {

  public static void main(String[] args) {
    var warp4jCommand = ComponentFactory.createWarp4JCommand();

    var exitCode = new CommandLine(warp4jCommand).execute(args);

    if (exitCode != 0) {
      System.exit(exitCode);
    }
  }
}
