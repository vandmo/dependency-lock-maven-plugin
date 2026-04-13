package se.vandmo.dependencylock.maven.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import se.vandmo.dependencylock.maven.versions.VersionConstraintVisitor;

final class VersionConstraintJsonSerializer
    implements VersionConstraintVisitor<JsonNode, JsonNodeFactory> {

  VersionConstraintJsonSerializer() {
    super();
  }

  @Override
  public JsonNode onVersion(String version, JsonNodeFactory context) {
    return context.textNode(version);
  }

  @Override
  public JsonNode onProjectVersion(JsonNodeFactory context) {
    return context.textNode(JsonConstants.USE_PROJECT_VERSION);
  }

  @Override
  public JsonNode onIgnoreVersion(JsonNodeFactory context) {
    return context.textNode(JsonConstants.IGNORED);
  }
}
