package se.vandmo.dependencylock.maven;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static java.util.Locale.ROOT;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.plugin.logging.Log;

public final class LockedDependencies {

  public final List<LockedDependency> lockedDependencies;
  private final Log log;

  private LockedDependencies(List<LockedDependency> lockedDependencies, Log log) {
    this.lockedDependencies = lockedDependencies;
    this.log = log;
  }

  public static LockedDependencies fromJson(JsonNode json, Log log) {
    if (!json.isArray()) {
      throw new IllegalStateException("Needs to be an array");
    }
    List<LockedDependency> lockedDependencies = new ArrayList<>();
    for (JsonNode entry : json) {
      lockedDependencies.add(LockedDependency.fromJson(entry));
    }
    return new LockedDependencies(unmodifiableList(lockedDependencies), log);
  }

  public static LockedDependencies empty(Log log) {
    return new LockedDependencies(emptyList(), log);
  }

  public static LockedDependencies from(Artifacts artifacts, Log log) {
    List<LockedDependency> lockedDependencies = new ArrayList<>();
    for (Artifact artifact : artifacts.artifacts) {
      lockedDependencies.add(LockedDependency.from(artifact));
    }
    return new LockedDependencies(unmodifiableList(lockedDependencies), log);
  }

  public JsonNode asJson() {
    ArrayNode json = JsonNodeFactory.instance.arrayNode();
    for (LockedDependency lockedDependency : lockedDependencies) {
      json.add(lockedDependency.asJson());
    }
    return json;
  }

  public LockedDependencies updateWith(Artifacts artifacts) {
    List<LockedDependency> updatedLockedDependencies = new ArrayList<>();
    for (Artifact artifact : artifacts.artifacts) {
      updatedLockedDependencies.add(maybeChangeTo_UseMine(artifact));
    }
    return new LockedDependencies(unmodifiableList(updatedLockedDependencies), log);
  }

  private LockedDependency maybeChangeTo_UseMine(Artifact artifact) {
    LockedDependency lockedDependency = LockedDependency.from(artifact);
    Optional<LockedVersion> possiblyExistingVersion = getExistingVersion(artifact.identifier);
    if (possiblyExistingVersion.isPresent()) {
      if (possiblyExistingVersion.get().useMine) {
        return lockedDependency.withVersion(LockedVersion.USE_MINE);
      }
    }
    return lockedDependency;
  }

  private Optional<LockedVersion> getExistingVersion(ArtifactIdentifier identifier) {
    return
        by(identifier)
            .map(lockedDependency -> lockedDependency.version);
  }

  public Diff compareWith(Artifacts artifacts, String projectVersion, ArtifactFilter useMyVersionForFilter) {
    List<String> missing = new ArrayList<>();
    List<String> different = new ArrayList<>();
    List<String> added = new ArrayList<>();
    for (LockedDependency lockedDependency : lockedDependencies) {
      Optional<Artifact> possiblyOtherArtifact = artifacts.by(lockedDependency.identifier);
      if (!possiblyOtherArtifact.isPresent()) {
        missing.add(lockedDependency.toString());
      } else {
        Artifact otherArtifact = possiblyOtherArtifact.get();
        LockedDependency possiblyChangedLockedDependency = maybeChangeTo_UseMine(
            useMyVersionForFilter, lockedDependency, otherArtifact);
        if (!possiblyChangedLockedDependency.matches(otherArtifact, projectVersion)) {
          different.add(format(ROOT, "Expected %s but found %s", lockedDependency, otherArtifact));
        }
      }
    }
    for (Artifact otherArtifact : artifacts.artifacts) {
      if (!by(otherArtifact.identifier).isPresent()) {
        added.add(otherArtifact.toString());
      }
    }
    return new Diff(missing, different, added);
  }

  private LockedDependency maybeChangeTo_UseMine(ArtifactFilter useMyVersionForFilter,
      LockedDependency lockedDependency, Artifact otherArtifact) {
    if (useMyVersionForFilter.include(otherArtifact.toMavenArtifact())) {
      log.info(format(ROOT,"Using my version for %s", otherArtifact));
      return lockedDependency.withVersion(LockedVersion.USE_MINE);
    } else {
      return lockedDependency;
    }
  }

  public Optional<LockedDependency> by(ArtifactIdentifier identifier) {
    for (LockedDependency lockedDependency : lockedDependencies) {
      if (lockedDependency.identifier.equals(identifier)) {
        return Optional.of(lockedDependency);
      }
    }
    return Optional.empty();
  }

  public static final class Diff {
    private final List<String> missing;
    private final List<String> different;
    private final List<String> added;
    private Diff(List<String> missing, List<String> different, List<String> added) {
      this.missing = missing;
      this.different = different;
      this.added = added;
    }
    public boolean equals() {
      return missing.isEmpty() && different.isEmpty() && added.isEmpty();
    }
    public void logTo(Log log) {
      if (!missing.isEmpty()) {
        log.error("Missing dependencies:");
        missing.forEach(line -> log.error("  "+line));
      }
      if (!different.isEmpty()) {
        log.error("The following dependencies differ:");
        different.forEach(line -> log.error("  "+line));
      }
      if (!added.isEmpty()) {
        log.error("Extraneous dependencies:");
        added.forEach(line -> log.error("  "+line));
      }
    }
  }
}
