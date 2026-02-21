package se.vandmo.dependencylock.maven.json;

import static java.lang.String.format;
import static java.util.Locale.ROOT;
import static se.vandmo.dependencylock.maven.lang.Strings.isBlank;

import com.fasterxml.jackson.core.JsonGenerator.Feature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.stream.Stream;
import se.vandmo.dependencylock.maven.Artifact;
import se.vandmo.dependencylock.maven.ArtifactIdentifier;
import se.vandmo.dependencylock.maven.Dependencies;
import se.vandmo.dependencylock.maven.Dependency;

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

  static JsonNode buildArtifactJson(Artifact artifact, JsonNodeFactory factory) {
    ObjectNode output = factory.objectNode();
    final ArtifactIdentifier artifactIdentifier = artifact.identifier;
    output.put("groupId", artifactIdentifier.groupId);
    output.put("artifactId", artifactIdentifier.artifactId);
    output.put("version", artifact.version);
    artifactIdentifier.classifier.ifPresent(
        actualClassifier -> output.put("classifier", actualClassifier));
    output.put("type", artifactIdentifier.type);
    output.put("integrity", artifact.getIntegrityForLockFile());
    return output;
  }

  static JsonNode buildDependenciesJson(
      Dependencies dependencies, JsonNodeFactory jsonNodeFactory) {
    ArrayNode json = jsonNodeFactory.arrayNode();
    for (Dependency dependency : dependencies) {
      json.add(buildDependencyJson(dependency, jsonNodeFactory));
    }
    return json;
  }

  static JsonNode buildArtifactsJson(Stream<Artifact> artifacts, JsonNodeFactory jsonNodeFactory) {
    ObjectNode result = jsonNodeFactory.objectNode();
    groupArtifacts(artifacts)
        .forEach((key, value) -> result.set(key, buildArtifactJson(value, jsonNodeFactory)));
    return result;
  }

  private static Map<String, Artifact> groupArtifacts(Stream<Artifact> artifacts) {
    Map<String, Artifact> artifactsMap = new HashMap<>();
    final Consumer<Artifact> artifactConsumer =
        artifact -> artifactsMap.putIfAbsent(artifact.getArtifactKey(), artifact);
    artifacts.forEach(artifactConsumer);
    return new TreeMap<>(artifactsMap);
  }

  static ObjectNode buildProfilesJson(
      Stream<Map.Entry<String, Dependencies>> profiles, JsonNodeFactory jsonNodeFactory) {
    ObjectNode result = jsonNodeFactory.objectNode();
    profiles.forEach(
        profile -> {
          ObjectNode profileNode = jsonNodeFactory.objectNode();
          profileNode.set(
              "dependencies", buildDependenciesJson(profile.getValue(), jsonNodeFactory));
          result.set(profile.getKey(), profileNode);
        });
    return result;
  }

  private static JsonNode buildDependencyJson(
      Dependency dependency, JsonNodeFactory jsonNodeFactory) {
    ObjectNode json = jsonNodeFactory.objectNode();
    json.put("artifact", dependency.getArtifactKey());
    json.put("scope", dependency.scope);
    json.put("optional", dependency.optional);
    return json;
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

  static Map<String, Dependencies> loadProfilesFromJson(
      JsonNode json, Map<String, Artifact> artifactMap) {
    final Map<String, Dependencies> dependenciesByProfileId;
    JsonNode profilesNode = json.get("profiles");
    if (profilesNode.isEmpty()) {
      dependenciesByProfileId = Collections.emptyMap();
    } else {
      dependenciesByProfileId = new HashMap<>(profilesNode.size());
      for (Iterator<Map.Entry<String, JsonNode>> it = profilesNode.fields(); it.hasNext(); ) {
        Map.Entry<String, JsonNode> currentEntry = it.next();
        final String profileId = currentEntry.getKey();
        final JsonNode profileNode = currentEntry.getValue();
        final Dependencies profileDependencies =
            loadDependenciesFromJson(getDependencies(profileNode), artifactMap);
        dependenciesByProfileId.put(profileId, profileDependencies);
      }
    }
    return dependenciesByProfileId;
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
