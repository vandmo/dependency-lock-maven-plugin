package se.vandmo.dependencylock.maven;

import static java.lang.String.format;
import static java.util.Collections.unmodifiableList;
import static java.util.Locale.ROOT;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.plugin.logging.Log;

public final class LockedDependencies {

  public final List<LockedDependency> lockedDependencies;
  private final Log log;

  private LockedDependencies(List<LockedDependency> lockedDependencies, Log log) {
    this.lockedDependencies = lockedDependencies;
    this.log = log;
  }

  public static LockedDependencies fromJson(
      JsonNode json, Log log, boolean enableIntegrityChecking) {
    if (!json.isArray()) {
      throw new IllegalStateException("Needs to be an array");
    }
    List<LockedDependency> lockedDependencies = new ArrayList<>();
    for (JsonNode entry : json) {
      lockedDependencies.add(LockedDependency.fromJson(entry, enableIntegrityChecking));
    }
    return new LockedDependencies(unmodifiableList(lockedDependencies), log);
  }

  public static LockedDependencies from(Artifacts artifacts, Log log, boolean integrityCheck) {
    List<LockedDependency> lockedDependencies = new ArrayList<>();
    for (Artifact artifact : artifacts.artifacts) {
      lockedDependencies.add(LockedDependency.from(artifact, integrityCheck));
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

  public Diff compareWith(Artifacts artifacts, String projectVersion, Filters filters) {
    LockFileExpectationsDiff expectationsDiff =
        new LockFileExpectationsDiff(artifacts, projectVersion, filters);
    List<String> unexpected = findUnexpected(artifacts, filters);
    return new Diff(expectationsDiff, unexpected);
  }

  private final class LockFileExpectationsDiff {
    private List<String> missing = new ArrayList<>();
    private List<String> different = new ArrayList<>();

    private LockFileExpectationsDiff(Artifacts artifacts, String projectVersion, Filters filters) {
      for (LockedDependency lockedDependency : lockedDependencies) {
        if (filters.ignoreFilter.include(lockedDependency.toArtifact().toMavenArtifact())) {
          log.info(format(ROOT, "Ignoring %s from lock file", lockedDependency));
          continue;
        }
        Predicate<Artifact> expectedDependency =
            resolve(filters.useMyVersionForFilter, lockedDependency, projectVersion);
        Optional<Artifact> possiblyOtherArtifact = artifacts.by(lockedDependency.identifier);
        if (!possiblyOtherArtifact.isPresent()) {
          missing.add(expectedDependency.toString());
        } else {
          Artifact otherArtifact = possiblyOtherArtifact.get();
          if (!expectedDependency.test(otherArtifact)) {
            different.add(
                format(ROOT, "Expected %s but found %s", expectedDependency, otherArtifact));
          }
        }
      }
    }
  }

  private List<String> findUnexpected(Artifacts artifacts, Filters filters) {
    List<String> unexpected = new ArrayList<>();
    for (Artifact artifact : artifacts.artifacts) {
      if (filters.ignoreFilter.include(artifact.toMavenArtifact())) {
        log.info(format(ROOT, "Ignoring %s from actual dependencies", artifact));
        continue;
      }
      if (!by(artifact.identifier).isPresent()) {
        unexpected.add(artifact.toString());
      }
    }
    return unexpected;
  }

  private Predicate<Artifact> resolve(
      ArtifactFilter useMyVersionForFilter,
      LockedDependency lockedDependency,
      String projectVersion) {
    boolean shouldUseMyVersion = shouldUseMyVersion(lockedDependency, useMyVersionForFilter);
    if (shouldUseMyVersion) {
      log.info(format(ROOT, "Using my version for %s", lockedDependency));
      return lockedDependency.withMyVersion(projectVersion);
    }
    return lockedDependency;
  }

  private boolean shouldUseMyVersion(
      LockedDependency lockedDependency, ArtifactFilter useMyVersionForFilter) {
    return useMyVersionForFilter.include(lockedDependency.toArtifact().toMavenArtifact());
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

    private Diff(LockFileExpectationsDiff lockFileExpectationsDiff, List<String> added) {
      this.missing = lockFileExpectationsDiff.missing;
      this.different = lockFileExpectationsDiff.different;
      this.added = added;
    }

    public boolean equals() {
      return missing.isEmpty() && different.isEmpty() && added.isEmpty();
    }

    public void logTo(Log log) {
      if (!missing.isEmpty()) {
        log.error("Missing dependencies:");
        missing.forEach(line -> log.error("  " + line));
      }
      if (!different.isEmpty()) {
        log.error("The following dependencies differ:");
        different.forEach(line -> log.error("  " + line));
      }
      if (!added.isEmpty()) {
        log.error("Extraneous dependencies:");
        added.forEach(line -> log.error("  " + line));
      }
    }
  }
}
