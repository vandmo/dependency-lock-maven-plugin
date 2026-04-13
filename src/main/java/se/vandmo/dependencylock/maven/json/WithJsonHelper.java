package se.vandmo.dependencylock.maven.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.stream.Stream;
import se.vandmo.dependencylock.maven.Artifact;
import se.vandmo.dependencylock.maven.ArtifactIdentifier;
import se.vandmo.dependencylock.maven.Dependencies;
import se.vandmo.dependencylock.maven.Dependency;
import se.vandmo.dependencylock.maven.ProfileEntry;
import se.vandmo.dependencylock.maven.lang.Strings;
import se.vandmo.dependencylock.maven.mojos.model.IActivation;
import se.vandmo.dependencylock.maven.mojos.model.IActivationOS;
import se.vandmo.dependencylock.maven.mojos.model.IActivationProperty;

class WithJsonHelper {
  final VersionConstraintJsonSerializer versionConstraintJsonSerializer;

  WithJsonHelper() {
    this.versionConstraintJsonSerializer = new VersionConstraintJsonSerializer();
  }

  final JsonNode buildArtifactJson(Artifact artifact, JsonNodeFactory factory) {
    ObjectNode output = factory.objectNode();
    final ArtifactIdentifier artifactIdentifier = artifact.identifier;
    output.put("groupId", artifactIdentifier.groupId);
    output.put("artifactId", artifactIdentifier.artifactId);
    output.set("version", artifact.version.accept(versionConstraintJsonSerializer, factory));
    artifactIdentifier.classifier.ifPresent(
        actualClassifier -> output.put("classifier", actualClassifier));
    output.put("type", artifactIdentifier.type);
    output.put("integrity", artifact.getIntegrityForLockFile());
    return output;
  }

  private JsonNode buildDependencyJson(Dependency dependency, JsonNodeFactory jsonNodeFactory) {
    ObjectNode json = jsonNodeFactory.objectNode();
    json.put("artifact", dependency.getArtifactKey());
    json.put("scope", dependency.scope);
    json.put("optional", dependency.optional);
    return json;
  }

  private static void addNodeIfNotNull(ObjectNode node, String key, JsonNode value) {
    if (value == null || value.isNull()) {
      return;
    }
    node.set(key, value);
  }

  private JsonNode buildActivationOsNode(
      IActivationOS activationOS, JsonNodeFactory jsonNodeFactory) {
    if (activationOS == null) {
      return jsonNodeFactory.nullNode();
    }
    final ObjectNode result = jsonNodeFactory.objectNode();
    setPropertyIfNotBlank(result, "family", activationOS.getFamily());
    setPropertyIfNotBlank(result, "name", activationOS.getName());
    setPropertyIfNotBlank(result, "arch", activationOS.getArch());
    setPropertyIfNotBlank(result, "version", activationOS.getVersion());
    return result;
  }

  private static void setPropertyIfNotBlank(ObjectNode node, String key, String value) {
    if (Strings.isBlank(value)) {
      return;
    }
    node.put(key, value);
  }

  private static JsonNode buildActivationPropertyNode(
      IActivationProperty activationProperty, JsonNodeFactory jsonNodeFactory) {
    if (activationProperty == null) {
      return jsonNodeFactory.nullNode();
    }
    final ObjectNode result = jsonNodeFactory.objectNode();
    setPropertyIfNotBlank(result, "name", activationProperty.getName());
    setPropertyIfNotBlank(result, "value", activationProperty.getValue());
    return result;
  }

  private JsonNode buildActivationNode(IActivation activation, JsonNodeFactory jsonNodeFactory) {
    final ObjectNode result = jsonNodeFactory.objectNode();
    addNodeIfNotNull(result, "os", buildActivationOsNode(activation.getOs(), jsonNodeFactory));
    addNodeIfNotNull(
        result, "property", buildActivationPropertyNode(activation.getProperty(), jsonNodeFactory));
    return result;
  }

  JsonNode buildProfilesJson(Stream<ProfileEntry> profiles, JsonNodeFactory jsonNodeFactory) {
    ArrayNode result = jsonNodeFactory.arrayNode();
    profiles
        .sorted(Comparator.comparing(entry -> entry.getProfile().getId()))
        .forEach(
            profile -> {
              ObjectNode profileNode = jsonNodeFactory.objectNode();
              profileNode.put("id", profile.getProfile().getId());
              profileNode.set(
                  "activation",
                  buildActivationNode(profile.getProfile().getActivation(), jsonNodeFactory));
              profileNode.set(
                  "dependencies",
                  buildDependenciesJson(profile.getDependencies(), jsonNodeFactory));
              result.add(profileNode);
            });
    return result;
  }

  final JsonNode buildDependenciesJson(Dependencies dependencies, JsonNodeFactory jsonNodeFactory) {
    ArrayNode json = jsonNodeFactory.arrayNode();
    for (Dependency dependency : dependencies) {
      json.add(buildDependencyJson(dependency, jsonNodeFactory));
    }
    return json;
  }

  final JsonNode buildArtifactsJson(Stream<Artifact> artifacts, JsonNodeFactory jsonNodeFactory) {
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
}
