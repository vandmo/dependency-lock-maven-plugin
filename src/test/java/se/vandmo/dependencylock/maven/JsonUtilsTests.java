package se.vandmo.dependencylock.maven;

import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.StringWriter;
import org.junit.Test;


public final class JsonUtilsTests {

  @Test
  public void shouldHaveGoodStartAndEnding() {
    ObjectNode json = JsonNodeFactory.instance.objectNode();
    json.set("something", JsonNodeFactory.instance.textNode("somevalue"));
    StringWriter sw = new StringWriter();
    JsonUtils.writeJson(sw, json);
    String written = sw.toString();
    assertTrue(written.endsWith(System.lineSeparator()+"}"+System.lineSeparator()));
    assertTrue(written.startsWith("{"+System.lineSeparator()));
  }

}
