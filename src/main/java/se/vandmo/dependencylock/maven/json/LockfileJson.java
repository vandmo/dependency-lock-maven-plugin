package se.vandmo.dependencylock.maven.json;

import static java.util.Objects.requireNonNull;
import static se.vandmo.dependencylock.maven.json.JsonUtils.getNonBlankStringValue;
import static se.vandmo.dependencylock.maven.json.JsonUtils.possiblyGetStringValue;
import static se.vandmo.dependencylock.maven.json.JsonUtils.readJson;

import com.fasterxml.jackson.databind.JsonNode;
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
import java.util.TreeMap;
import java.util.stream.Stream;
import org.apache.maven.plugin.logging.Log;
import se.vandmo.dependencylock.maven.Artifact;
import se.vandmo.dependencylock.maven.ArtifactIdentifier;
import se.vandmo.dependencylock.maven.Artifacts;
import se.vandmo.dependencylock.maven.Dependencies;
import se.vandmo.dependencylock.maven.Dependency;
import se.vandmo.dependencylock.maven.Extension;
import se.vandmo.dependencylock.maven.Extensions;
import se.vandmo.dependencylock.maven.LockFileAccessor;
import se.vandmo.dependencylock.maven.LockedProject;
import se.vandmo.dependencylock.maven.Lockfile;
import se.vandmo.dependencylock.maven.Plugin;
import se.vandmo.dependencylock.maven.Plugins;

public final class LockfileJson implements Lockfile {

  private final LockFileAccessor dependenciesLockFile;
  private final Log log;

  private LockfileJson(LockFileAccessor dependenciesLockFile, Log log) {
    this.dependenciesLockFile = dependenciesLockFile;
    this.log = log;
  }

  public static LockfileJson from(LockFileAccessor dependenciesLockFile, Log log) {
    return new LockfileJson(requireNonNull(dependenciesLockFile), requireNonNull(log));
  }

  public LockedProject read() {
    JsonNode json = readJsonNode();
    if (!json.isObject()) {
      throw new IllegalStateException("Expected top level type to be an object");
    }
    return fromJson(json, log);
  }

  private static LockedProject fromJson(JsonNode json, Log log) {
    final Map<String, Artifact> artifactMap = loadArtifactsFromJson(json.get("artifacts"));
    final Plugins plugins = loadPluginsFromJson(json.get("plugins"), artifactMap);
    final Extensions extensions = loadExtensionsFromJson(json.get("extensions"), artifactMap);
    final Dependencies dependencies =
        loadDependenciesFromJson(json.get("dependencies"), artifactMap);
    return LockedProject.from(plugins, dependencies, extensions, log);
  }

  private static Map<String, Artifact> loadArtifactsFromJson(JsonNode json) {
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

  private static Artifact parseArtifact(JsonNode json) {
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

  private static Dependencies loadDependenciesFromJson(
      JsonNode json, Map<String, Artifact> artifacts) {
    if (null == json) {
      return Dependencies.fromDependencies(Collections.emptyList());
    }
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

  private static Plugins loadPluginsFromJson(JsonNode json, Map<String, Artifact> artifacts) {
    if (null == json) {
      return Plugins.from(Collections.emptyList());
    }
    if (!json.isArray()) {
      throw new IllegalStateException("Needs to be an array");
    }
    List<Plugin> lockedPlugins = new ArrayList<>();
    for (JsonNode entry : json) {
      lockedPlugins.add(lockedPluginFromJson(entry, artifacts));
    }
    return Plugins.from(lockedPlugins);
  }

  private static Extensions loadExtensionsFromJson(JsonNode json, Map<String, Artifact> artifacts) {
    if (null == json) {
      return Extensions.from(Collections.emptyList());
    }
    if (!json.isArray()) {
      throw new IllegalStateException("Needs to be an array");
    }
    final List<Extension> lockedPlugins = new ArrayList<>(json.size());
    for (JsonNode entry : json) {
      lockedPlugins.add(lockedExtensionFromJson(entry, artifacts));
    }
    return Extensions.from(lockedPlugins);
  }

  private static Plugin lockedPluginFromJson(JsonNode json, Map<String, Artifact> artifacts) {
    final String artifactKey = getNonBlankStringValue(json, "artifact");
    final Artifact artifact = artifacts.get(artifactKey);
    if (null == artifact) {
      throw new IllegalArgumentException("Artifact not found: " + artifactKey);
    }
    final Plugin.ArtifactsBuilderStage builderStage = Plugin.forArtifact(artifact);
    JsonNode dependenciesNode = json.get("dependencies");
    final List<Artifact> dependencies;
    if (null == dependenciesNode) {
      dependencies = Collections.emptyList();
    } else {
      if (!dependenciesNode.isArray()) {
        throw new IllegalStateException("Needs to be an array");
      }
      dependencies = new ArrayList<>(dependenciesNode.size());
      for (JsonNode dependency : dependenciesNode) {
        final String dependencyKey = dependency.asText();
        final Artifact artifactDependency = artifacts.get(dependencyKey);
        if (null == artifactDependency) {
          throw new IllegalArgumentException("Dependency not found: " + dependencyKey);
        }
        dependencies.add(artifactDependency);
      }
    }
    return builderStage.artifacts(Artifacts.fromArtifacts(dependencies)).build();
  }

  private static Extension lockedExtensionFromJson(JsonNode json, Map<String, Artifact> artifacts) {
    final String artifactKey = getNonBlankStringValue(json, "artifact");
    final Artifact artifact = artifacts.get(artifactKey);
    if (null == artifact) {
      throw new IllegalArgumentException("Artifact not found: " + artifactKey);
    }
    return Extension.of(artifact);
  }

  private JsonNode readJsonNode() {
    try (Reader reader = dependenciesLockFile.reader()) {
      return readJson(reader);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public void write(LockedProject contents) {
    JsonNode json = write(contents, JsonNodeFactory.instance);
    try (Writer writer = dependenciesLockFile.writer()) {
      JsonUtils.writeJson(writer, json);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private JsonNode write(LockedProject contents, JsonNodeFactory jsonNodeFactory) {
    ObjectNode json = jsonNodeFactory.objectNode();
    ObjectNode output = jsonNodeFactory.objectNode();
    collectArtifacts(contents)
        .forEach((key, value) -> output.set(key, writeJson(value, jsonNodeFactory)));
    json.set("artifacts", output);
    json.set("plugins", asJson(contents.plugins, jsonNodeFactory));
    json.set("dependencies", asJson(contents.dependencies, jsonNodeFactory));
    json.set("extensions", asJson(contents.extensions, jsonNodeFactory));
    return json;
  }

  private JsonNode writeJson(Artifact artifact, JsonNodeFactory factory) {
    ObjectNode json = factory.objectNode();
    final ArtifactIdentifier artifactIdentifier = artifact.identifier;
    json.put("groupId", artifactIdentifier.groupId);
    json.put("artifactId", artifactIdentifier.artifactId);
    json.put("version", artifact.version);
    artifactIdentifier.classifier.ifPresent(
        actualClassifier -> json.put("classifier", actualClassifier));
    json.put("type", artifactIdentifier.type);
    json.put("integrity", artifact.getIntegrityForLockFile());
    return json;
  }

  private Map<String, Artifact> collectArtifacts(LockedProject contents) {
    Map<String, Artifact> artifacts = new HashMap<>();
    Stream.of(contents.dependencies, contents.extensions, contents.plugins)
        .flatMap(entityWithArtifacts -> entityWithArtifacts.artifacts())
        .forEach(artifact -> artifacts.putIfAbsent(artifact.getArtifactKey(), artifact));
    return new TreeMap<>(artifacts);
  }

  private JsonNode asJson(Plugins plugins, JsonNodeFactory jsonNodeFactory) {
    ArrayNode json = jsonNodeFactory.arrayNode();
    for (Plugin lockedPlugin : plugins) {
      json.add(asJson(lockedPlugin, jsonNodeFactory));
    }
    return json;
  }

  private JsonNode asJson(Dependencies dependencies, JsonNodeFactory jsonNodeFactory) {
    ArrayNode json = jsonNodeFactory.arrayNode();
    for (Dependency dependency : dependencies) {
      json.add(asJson(dependency, jsonNodeFactory));
    }
    return json;
  }

  private JsonNode asJson(Extensions extensions, JsonNodeFactory jsonNodeFactory) {
    ArrayNode json = jsonNodeFactory.arrayNode();
    for (Extension extension : extensions) {
      json.add(asJson(extension, jsonNodeFactory));
    }
    return json;
  }

  private JsonNode asJson(Plugin lockedPlugin, JsonNodeFactory jsonNodeFactory) {
    ObjectNode json = jsonNodeFactory.objectNode();
    json.put("artifact", lockedPlugin.toString_withoutIntegrity());
    ArrayNode dependencies = jsonNodeFactory.arrayNode();
    for (Artifact artifact : lockedPlugin.dependencies) {
      dependencies.add(artifact.getArtifactKey());
    }
    json.set("dependencies", dependencies);
    return json;
  }

  private JsonNode asJson(Extension lockedExtension, JsonNodeFactory jsonNodeFactory) {
    ObjectNode json = jsonNodeFactory.objectNode();
    json.put("artifact", lockedExtension.getArtifactKey());
    return json;
  }

  private JsonNode asJson(Dependency dependency, JsonNodeFactory jsonNodeFactory) {
    ObjectNode json = jsonNodeFactory.objectNode();
    json.put("artifact", dependency.getArtifactKey());
    json.put("scope", dependency.scope);
    json.put("optional", dependency.optional);
    return json;
  }
}
