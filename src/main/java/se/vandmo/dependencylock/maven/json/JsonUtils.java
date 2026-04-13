package se.vandmo.dependencylock.maven.json;

import static java.lang.String.format;
import static java.util.Locale.ROOT;
import static se.vandmo.dependencylock.maven.lang.Strings.isBlank;

import com.fasterxml.jackson.core.JsonGenerator.Feature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import se.vandmo.dependencylock.maven.Artifact;
import se.vandmo.dependencylock.maven.ArtifactIdentifier;
import se.vandmo.dependencylock.maven.Dependencies;
import se.vandmo.dependencylock.maven.Dependency;
import se.vandmo.dependencylock.maven.ProfileEntry;
import se.vandmo.dependencylock.maven.mojos.model.Activation;
import se.vandmo.dependencylock.maven.mojos.model.ActivationOS;
import se.vandmo.dependencylock.maven.mojos.model.ActivationProperty;
import se.vandmo.dependencylock.maven.mojos.model.Profile;

public final class JsonUtils {
  private JsonUtils() {}

  public static String getNonBlankStringValue(JsonNode json, String fieldName) {
    JsonNode jsonNode = json.get(fieldName);
    if (jsonNode == null) {
      throw missingValueFor(fieldName);
    }
    String value = jsonNode.textValue();
    if (isBlank(value)) {
      throw missingValueFor(fieldName);
    }
    return value;
  }

  private static IllegalArgumentException missingValueFor(String fieldName) {
    return new IllegalArgumentException(format(ROOT, "Missing value for '%s'", fieldName));
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
    return Optional.of(getNonBlankStringValue(json, fieldName));
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

  static Map<String, Artifact> loadArtifactsFromJson(JsonNode json) {
    if (null == json) {
      return Collections.emptyMap();
    }
    Iterator<Map.Entry<String, JsonNode>> entries = json.fields();
    Map<String, Artifact> artifacts = new HashMap<>();
    while (entries.hasNext()) {
      final Map.Entry<String, JsonNode> entry = entries.next();
      artifacts.put(entry.getKey(), parseArtifact(entry.getValue()));
    }
    return artifacts;
  }

  static Artifact parseArtifact(JsonNode json) {
    return Artifact.builder()
        .artifactIdentifier(
            ArtifactIdentifier.builder()
                .groupId(getNonBlankStringValue(json, "groupId"))
                .artifactId(getNonBlankStringValue(json, "artifactId"))
                .classifier(possiblyGetStringValue(json, "classifier"))
                .type(possiblyGetStringValue(json, "type"))
                .build())
        .version(getNonBlankStringValue(json, "version"))
        .integrity(getNonBlankStringValue(json, "integrity"))
        .build();
  }

  static Collection<ProfileEntry> loadProfilesFromJson(
      JsonNode json, Map<String, Artifact> artifactMap) {
    JsonNode profilesNode = json.get("profiles");
    if (profilesNode.isEmpty()) {
      return Collections.emptyList();
    }
    Collection<ProfileEntry> profileEntries = new ArrayList<>(profilesNode.size());
    for (Iterator<JsonNode> it = profilesNode.elements(); it.hasNext(); ) {
      JsonNode profileNode = it.next();
      final String profileId = profileNode.get("id").asText();
      final Activation activation = buildActivation(profileNode.get("activation"));
      final Dependencies profileDependencies =
          loadDependenciesFromJson(getDependencies(profileNode), artifactMap);
      Profile result = new Profile();
      result.setId(profileId);
      result.setActivation(activation);
      profileEntries.add(new ProfileEntry(result, profileDependencies));
    }
    return profileEntries;
  }

  private static Activation buildActivation(JsonNode activationNode) {
    if (activationNode == null || activationNode.isNull()) {
      return null;
    }
    Activation result = new Activation();
    result.setOs(buildActivationOS(activationNode.get("os")));
    result.setProperty(buildActivationProperty(activationNode.get("property")));
    return result;
  }

  private static ActivationProperty buildActivationProperty(JsonNode activationPropertyNode) {
    if (activationPropertyNode == null || activationPropertyNode.isNull()) {
      return null;
    }
    ActivationProperty result = new ActivationProperty();
    result.setName(possiblyGetStringValue(activationPropertyNode, "name").orElse(null));
    result.setValue(possiblyGetStringValue(activationPropertyNode, "value").orElse(null));
    return result;
  }

  private static ActivationOS buildActivationOS(JsonNode activationOSNode) {
    if (activationOSNode == null || activationOSNode.isNull()) {
      return null;
    }
    ActivationOS result = new ActivationOS();
    result.setFamily(possiblyGetStringValue(activationOSNode, "family").orElse(null));
    result.setName(possiblyGetStringValue(activationOSNode, "name").orElse(null));
    result.setArch(possiblyGetStringValue(activationOSNode, "arch").orElse(null));
    result.setVersion(possiblyGetStringValue(activationOSNode, "version").orElse(null));
    return result;
  }

  static JsonNode getDependencies(JsonNode json) {
    final JsonNode dependencies = json.get("dependencies");
    if (dependencies == null) {
      throw new IllegalStateException("Missing dependencies field");
    }
    return dependencies;
  }

  static Dependencies loadDependenciesFromJson(JsonNode json, Map<String, Artifact> artifacts) {
    final List<Dependency> dependencies = new ArrayList<>();
    if (!json.isArray()) {
      throw new IllegalStateException("Needs to be an array");
    }
    for (JsonNode dependency : json) {
      final String artifactKey = getNonBlankStringValue(dependency, "artifact");
      final Artifact artifact = artifacts.get(artifactKey);
      if (artifact == null) {
        throw new IllegalStateException("Artifact not found: " + artifactKey);
      }
      final String scope = possiblyGetStringValue(dependency, "scope").orElse(null);
      final boolean optional = dependency.get("optional").asBoolean();
      dependencies.add(Dependency.forArtifact(artifact).scope(scope).optional(optional).build());
    }
    return Dependencies.fromDependencies(dependencies);
  }
}
