package se.vandmo.dependencylock.maven.json;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.StringWriter;
import org.junit.Test;
import se.vandmo.dependencylock.maven.json.JsonUtils;

public final class JsonUtilsTests {

  @Test
  public void getBooleanOrDefault() {
    ObjectNode json = forBooleanTests();
    assertTrue(JsonUtils.getBooleanOrDefault(json, "something-true", false));
    assertFalse(JsonUtils.getBooleanOrDefault(json, "something-false", true));
    assertTrue(JsonUtils.getBooleanOrDefault(json, "nothing", true));
    assertFalse(JsonUtils.getBooleanOrDefault(json, "nothing", false));
  }

  @Test
  public void getBooleanOrDefault_illegal() {
    ObjectNode json = forBooleanTests();
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> JsonUtils.getBooleanOrDefault(json, "something-else", true));
    assertEquals("'something-else' is not a boolean value", exception.getMessage());
  }

  private static ObjectNode forBooleanTests() {
    ObjectNode json = JsonNodeFactory.instance.objectNode();
    json.set("something-true", JsonNodeFactory.instance.booleanNode(true));
    json.set("something-false", JsonNodeFactory.instance.booleanNode(false));
    json.set("something-else", JsonNodeFactory.instance.textNode("some text"));
    return json;
  }

  @Test
  public void shouldHaveGoodStartAndEnding() {
    ObjectNode json = JsonNodeFactory.instance.objectNode();
    json.set("something", JsonNodeFactory.instance.textNode("somevalue"));
    StringWriter sw = new StringWriter();
    JsonUtils.writeJson(sw, json);
    String written = sw.toString();
    assertTrue(written.endsWith(System.lineSeparator() + "}" + System.lineSeparator()));
    assertTrue(written.startsWith("{" + System.lineSeparator()));
  }
}
