package se.vandmo.dependencylock.maven;

import static org.apache.commons.lang3.StringUtils.isBlank;

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
      throw new IllegalArgumentException("Missing value for "+fieldName);
    }
    return value;
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
          .writerWithDefaultPrettyPrinter()
          .writeValue(writer, json);
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

}
