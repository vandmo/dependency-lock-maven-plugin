package se.vandmo.dependencylock.maven.json;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.StringWriter;
import org.junit.Test;

public final class JsonUtilsTests {

  @Test
  public void getNonBlankStringValue_valid() {
    ObjectNode json = forStringTests();
    assertEquals("cast-iron-pan", JsonUtils.getNonBlankStringValue(json, "something-useful"));
  }

  @Test
  public void getNonBlankStringValue_blank() {
    ObjectNode json = forStringTests();
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> JsonUtils.getNonBlankStringValue(json, "something-null"));
    assertEquals("Missing value for 'something-null'", exception.getMessage());
  }

  @Test
  public void getNonBlankStringValue_null() {
    ObjectNode json = forStringTests();
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> JsonUtils.getNonBlankStringValue(json, "something-null"));
    assertEquals("Missing value for 'something-null'", exception.getMessage());
  }

  @Test
  public void getNonBlankStringValue_missing() {
    ObjectNode json = forStringTests();
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> JsonUtils.getNonBlankStringValue(json, "something-missing"));
    assertEquals("Missing value for 'something-missing'", exception.getMessage());
  }

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
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> JsonUtils.getBooleanOrDefault(json, "something-else", true));
    assertEquals("'something-else' is not a boolean value", exception.getMessage());
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

  private static ObjectNode forBooleanTests() {
    ObjectNode json = JsonNodeFactory.instance.objectNode();
    json.set("something-true", JsonNodeFactory.instance.booleanNode(true));
    json.set("something-false", JsonNodeFactory.instance.booleanNode(false));
    json.set("something-else", JsonNodeFactory.instance.textNode("some text"));
    return json;
  }

  private static ObjectNode forStringTests() {
    ObjectNode json = JsonNodeFactory.instance.objectNode();
    json.set("something-blank", JsonNodeFactory.instance.textNode(" \t"));
    json.set("something-useful", JsonNodeFactory.instance.textNode("cast-iron-pan"));
    json.set("something-null", JsonNodeFactory.instance.nullNode());
    return json;
  }
}
