package de.dddns.kirbylink.warp4j.config;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.experimental.UtilityClass;

@UtilityClass
public class Warp4JResources {

  private static final Properties PROPERTIES = new Properties();

  static {
    try (var in = Warp4JResources.class.getResourceAsStream("/warp4j.properties")) {
      PROPERTIES.load(Objects.requireNonNull(in));
      resolvePlaceholders(PROPERTIES);
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to load warp4j.properties", e);
    }
  }

  public static String get(String key) {
    return PROPERTIES.getProperty(key);
  }

  public static String format(String key, Object... args) {
    return String.format(get(key), args);
  }

  public static String getTemplate(String name) {
    try (var inputStream = Warp4JResources.class.getClassLoader().getResourceAsStream("templates/" + name)) {
      if (inputStream == null) {
        throw new IllegalArgumentException("Template not found: " + name);
      }
      return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static void resolvePlaceholders(Properties props) {
    for (String key : props.stringPropertyNames()) {
      var value = props.getProperty(key);
      props.setProperty(key, resolve(value, props));
    }
  }

  private static String resolve(String value, Properties props) {
    var matcher = Pattern.compile("\\$\\{([^}]+)}").matcher(value);
    var buffer = new StringBuffer();
    while (matcher.find()) {
      var refKey = matcher.group(1);
      var refValue = props.getProperty(refKey, "");
      matcher.appendReplacement(buffer, Matcher.quoteReplacement(refValue));
    }
    matcher.appendTail(buffer);
    return buffer.toString();
  }
}

