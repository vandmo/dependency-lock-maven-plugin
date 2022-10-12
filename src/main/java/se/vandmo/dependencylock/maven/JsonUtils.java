package se.vandmo.dependencylock.maven;

import static java.lang.String.format;
import static java.util.Locale.ROOT;
import static org.apache.commons.lang3.StringUtils.isBlank;

import com.fasterxml.jackson.core.JsonGenerator.Feature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.util.Optional;

public final class JsonUtils {
  private JsonUtils() {}

  public static String getStringValue(JsonNode json, String fieldName) {
    String value = json.get(fieldName).textValue();
    if (isBlank(value)) {
      throw new IllegalArgumentException("Missing value for " + fieldName);
    }
    return value;
  }

  public static boolean getBooleanOrDefault(JsonNode json, String fieldName, boolean defaultValue) {
    if (!json.has(fieldName)) {
      return defaultValue;
    }
    JsonNode value = json.get(fieldName);
    if (!value.isBoolean()) {
      throw new IllegalArgumentException(format(ROOT, "'%s' is not a boolean value", fieldName));
    }
    return value.booleanValue();
  }

  public static Optional<String> possiblyGetStringValue(JsonNode json, String fieldName) {
    if (!json.has(fieldName)) {
      return Optional.empty();
    }
    return Optional.of(getStringValue(json, fieldName));
  }

  public static JsonNode readJson(Reader reader) {
    try {
      return new ObjectMapper().readTree(reader);
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  public static void writeJson(Writer writer, JsonNode json) {
    try {
      new ObjectMapper()
          .disable(Feature.AUTO_CLOSE_TARGET)
          .writerWithDefaultPrettyPrinter()
          .writeValue(writer, json);
      writer.write(System.lineSeparator());
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }
}
