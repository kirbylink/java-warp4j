package de.dddns.kirbylink.warp4j.config;

import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import lombok.experimental.UtilityClass;

@UtilityClass
public class LoggingConfiguration {
  @SuppressWarnings("squid:S4792") // This logger level change is restricted to application-specific classes and is controlled via CLI (e.g., --verbose)
  public static void setRootLogLevel(Level level) {
    var loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
    loggerContext.getLogger("de.dddns.kirbylink").setLevel(level);
  }
}
