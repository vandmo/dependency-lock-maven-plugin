package se.vandmo.dependencylock.maven;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Locale.ROOT;
import static java.util.Objects.requireNonNull;
import static se.vandmo.dependencylock.maven.lang.Strings.joinNouns;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.maven.plugin.logging.Log;

public final class LockedDependencies {

  public final Dependencies lockedDependencies;
  private final Log log;

  private LockedDependencies(Dependencies lockedDependencies, Log log) {
    this.lockedDependencies = lockedDependencies;
    this.log = log;
  }

  public static LockedDependencies from(Dependencies artifacts, Log log) {
    return new LockedDependencies(requireNonNull(artifacts), log);
  }

  public Diff compareWith(Dependencies dependencies, Filters filters) {
    LockFileExpectationsDiff expectationsDiff = new LockFileExpectationsDiff(dependencies, filters);
    List<String> extraneous = findExtraneous(dependencies, filters);
    return new Diff(expectationsDiff, extraneous);
  }

  private final class LockFileExpectationsDiff {
    private List<String> missing = new ArrayList<>();
    private List<String> different = new ArrayList<>();

    private LockFileExpectationsDiff(Dependencies artifacts, Filters filters) {
      for (Dependency lockedDependency : lockedDependencies) {
        final ArtifactIdentifier identifier = lockedDependency.getArtifactIdentifier();
        Optional<Dependency> possiblyOtherArtifact = artifacts.by(identifier);
        if (!possiblyOtherArtifact.isPresent()) {
          if (filters.allowMissing(lockedDependency)) {
            log.info(format(ROOT, "Ignoring missing %s", identifier));
          } else {
            missing.add(identifier.toString());
          }
        } else {
          Dependency actualDependency = possiblyOtherArtifact.get();
          AtomicReference<Dependency> lockedDependencyRef = new AtomicReference<>(lockedDependency);
          List<String> wrongs = findDiffs(lockedDependencyRef, actualDependency, filters);
          if (!wrongs.isEmpty()) {
            different.add(
                format(
                    ROOT,
                    "Expected %s but found %s, wrong %s",
                    lockedDependencyRef.get(),
                    actualDependency,
                    joinNouns(wrongs)));
          }
        }
      }
    }

    private List<String> findDiffs(
        AtomicReference<Dependency> lockedDependencyRef,
        Dependency actualDependency,
        Filters filters) {
      List<String> wrongs = new ArrayList<>();
      wrongs.addAll(diffOptional(lockedDependencyRef.get(), actualDependency));
      wrongs.addAll(diffScope(lockedDependencyRef.get(), actualDependency));
      wrongs.addAll(diffIntegrity(lockedDependencyRef.get(), actualDependency, filters));
      wrongs.addAll(diffVersion(lockedDependencyRef, actualDependency, filters));
      return wrongs;
    }

    private List<String> diffVersion(
        AtomicReference<Dependency> lockedDependencyRef,
        Dependency actualDependency,
        Filters filters) {
      Dependency lockedDependency = lockedDependencyRef.get();
      Filters.VersionConfiguration versionConfiguration =
          filters.versionConfiguration(lockedDependency);
      switch (versionConfiguration.type) {
        case check:
          if (lockedDependency.artifact.version.equals(actualDependency.artifact.version)) {
            return emptyList();
          } else {
            return asList("version");
          }
        case useProjectVersion:
          log.info(format(ROOT, "Using project version for %s", lockedDependency));
          lockedDependencyRef.set(
              lockedDependency.withVersion(versionConfiguration.projectVersion));
          if (versionConfiguration.projectVersion.equals(actualDependency.artifact.version)) {
            return emptyList();
          } else {
            return asList("version (expected project version)");
          }
        case snapshot:
          log.info(format(ROOT, "Allowing snapshot version for %s", lockedDependency));
          if (VersionUtils.snapshotMatch(
              lockedDependency.artifact.version, actualDependency.artifact.version)) {
            return emptyList();
          } else {
            return asList("version (allowing snapshot version)");
          }
        case ignore:
          log.info(format(ROOT, "Ignoring version for %s", lockedDependency));
          return emptyList();
        default:
          throw new RuntimeException("Unsupported enum value");
      }
    }

    private List<String> diffOptional(Dependency lockedDependency, Dependency actualDependency) {
      if (lockedDependency.optional == actualDependency.optional) {
        return emptyList();
      } else {
        return asList("optional");
      }
    }

    private List<String> diffScope(Dependency lockedDependency, Dependency actualDependency) {
      if (lockedDependency.scope.equals(actualDependency.scope)) {
        return emptyList();
      } else {
        return asList("scope");
      }
    }

    private List<String> diffIntegrity(
        Dependency lockedDependency, Dependency actualArtifact, Filters filters) {
      if (lockedDependency.artifact.integrity.equals(actualArtifact.artifact.integrity)) {
        return emptyList();
      } else {
        DependencySetConfiguration.Integrity integrityConfiguration =
            filters.integrityConfiguration(lockedDependency);
        switch (integrityConfiguration) {
          case check:
            return asList("integrity");
          case ignore:
            log.info(format(ROOT, "Ignoring integrity for %s", lockedDependency));
            return emptyList();
          default:
            throw new RuntimeException("Unsupported enum value");
        }
      }
    }
  }

  private List<String> findExtraneous(Dependencies dependencies, Filters filters) {
    List<String> extraneous = new ArrayList<>();
    for (Dependency dependency : dependencies) {
      final Artifact artifact = dependency.artifact;
      if (!lockedDependencies.by(artifact.identifier).isPresent()) {
        if (filters.allowSuperfluous(dependency)) {
          log.info(format(ROOT, "Ignoring extraneous %s", artifact.identifier));
        } else {
          extraneous.add(dependency.toString_withoutIntegrity());
        }
      }
    }
    return extraneous;
  }

  public static final class Diff {
    private final List<String> missing;
    private final List<String> different;
    private final List<String> extraneous;

    private Diff(LockFileExpectationsDiff lockFileExpectationsDiff, List<String> extraneous) {
      this.missing = lockFileExpectationsDiff.missing;
      this.different = lockFileExpectationsDiff.different;
      this.extraneous = extraneous;
    }

    public boolean equals() {
      return missing.isEmpty() && different.isEmpty() && extraneous.isEmpty();
    }

    public void logTo(Log log) {
      if (!missing.isEmpty()) {
        log.error("Missing dependencies:");
        missing.forEach(line -> log.error("  " + line));
      }
      if (!extraneous.isEmpty()) {
        log.error("Extraneous dependencies:");
        extraneous.forEach(line -> log.error("  " + line));
      }
      if (!different.isEmpty()) {
        log.error("The following dependencies differ:");
        different.forEach(line -> log.error("  " + line));
      }
    }
  }
}
