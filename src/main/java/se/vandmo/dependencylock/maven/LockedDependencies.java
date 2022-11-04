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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.maven.plugin.logging.Log;

public final class LockedDependencies {

  public final Artifacts lockedDependencies;
  private final Log log;

  private LockedDependencies(Artifacts lockedDependencies, Log log) {
    this.lockedDependencies = lockedDependencies;
    this.log = log;
  }

  public static LockedDependencies from(Artifacts artifacts, Log log) {
    return new LockedDependencies(requireNonNull(artifacts), log);
  }

  public Diff compareWith(Artifacts artifacts, Filters filters) {
    LockFileExpectationsDiff expectationsDiff = new LockFileExpectationsDiff(artifacts, filters);
    List<String> extraneous = findExtraneous(artifacts, filters);
    return new Diff(expectationsDiff, extraneous);
  }

  private final class LockFileExpectationsDiff {
    private List<String> missing = new ArrayList<>();
    private List<String> different = new ArrayList<>();

    private LockFileExpectationsDiff(Artifacts artifacts, Filters filters) {
      for (Artifact lockedDependency : lockedDependencies) {
        Optional<Artifact> possiblyOtherArtifact = artifacts.by(lockedDependency.identifier);
        if (!possiblyOtherArtifact.isPresent()) {
          if (filters.allowMissing(lockedDependency)) {
            log.info(format(ROOT, "Ignoring missing %s", lockedDependency.identifier));
          } else {
            missing.add(lockedDependency.identifier.toString());
          }
        } else {
          Artifact actualArtifact = possiblyOtherArtifact.get();
          AtomicReference<Artifact> lockedDependencyRef = new AtomicReference<>(lockedDependency);
          List<String> wrongs = findDiffs(lockedDependencyRef, actualArtifact, filters);
          if (!wrongs.isEmpty()) {
            different.add(
                format(
                    ROOT,
                    "Expected %s but found %s, wrong %s",
                    lockedDependencyRef.get(),
                    actualArtifact,
                    joinNouns(wrongs)));
          }
        }
      }
    }

    private List<String> findDiffs(
        AtomicReference<Artifact> lockedDependencyRef, Artifact actualArtifact, Filters filters) {
      List<String> wrongs = new ArrayList<>();
      wrongs.addAll(diffOptional(lockedDependencyRef.get(), actualArtifact));
      wrongs.addAll(diffScope(lockedDependencyRef.get(), actualArtifact));
      wrongs.addAll(diffIntegrity(lockedDependencyRef.get(), actualArtifact, filters));
      wrongs.addAll(diffVersion(lockedDependencyRef, actualArtifact, filters));
      return wrongs;
    }

    private List<String> diffVersion(
        AtomicReference<Artifact> lockedDependencyRef, Artifact actualArtifact, Filters filters) {
      Artifact lockedDependency = lockedDependencyRef.get();
      Filters.VersionConfiguration versionConfiguration =
          filters.versionConfiguration(lockedDependency);
      switch (versionConfiguration.type) {
        case check:
          if (lockedDependency.version.equals(actualArtifact.version)) {
            return emptyList();
          } else {
            return asList("version");
          }
        case useProjectVersion:
          log.info(format(ROOT, "Using project version for %s", lockedDependency));
          lockedDependencyRef.set(
              lockedDependency.withVersion(versionConfiguration.projectVersion));
          if (versionConfiguration.projectVersion.equals(actualArtifact.version)) {
            return emptyList();
          } else {
            return asList("version (expected project version)");
          }
        case snapshot:
          log.info(format(ROOT, "Allowing snapshot version for %s", lockedDependency));
          if (snapshotMatch(lockedDependency.version, actualArtifact.version)) {
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

    private List<String> diffOptional(Artifact lockedDependency, Artifact actualArtifact) {
      if (lockedDependency.optional == actualArtifact.optional) {
        return emptyList();
      } else {
        return asList("optional");
      }
    }

    private List<String> diffScope(Artifact lockedDependency, Artifact actualArtifact) {
      if (lockedDependency.scope.equals(actualArtifact.scope)) {
        return emptyList();
      } else {
        return asList("scope");
      }
    }

    private List<String> diffIntegrity(
        Artifact lockedDependency, Artifact actualArtifact, Filters filters) {
      if (lockedDependency.integrity.equals(actualArtifact.integrity)) {
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

  private List<String> findExtraneous(Artifacts artifacts, Filters filters) {
    List<String> extraneous = new ArrayList<>();
    for (Artifact artifact : artifacts.artifacts) {
      if (!by(artifact.identifier).isPresent()) {
        if (filters.allowSuperfluous(artifact)) {
          log.info(format(ROOT, "Ignoring extraneous %s", artifact.identifier));
        } else {
          extraneous.add(artifact.toString_withoutIntegrity());
        }
      }
    }
    return extraneous;
  }

  public Optional<Artifact> by(ArtifactIdentifier identifier) {
    for (Artifact lockedDependency : lockedDependencies) {
      if (lockedDependency.identifier.equals(identifier)) {
        return Optional.of(lockedDependency);
      }
    }
    return Optional.empty();
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

  // Visible for testing
  static boolean snapshotMatch(String version, String otherVersion) {
    if (version.equals(otherVersion)) {
      return true;
    }
    return stripSnapshot(version).equals(stripSnapshot(otherVersion));
  }

  private static final Pattern SNAPSHOT_TIMESTAMP =
      Pattern.compile("^((?<base>.*)-)?([0-9]{8}\\.[0-9]{6}-[0-9]+)$");

  // Visible for testing
  static String stripSnapshot(String version) {
    if (version.endsWith("-SNAPSHOT")) {
      return version.substring(0, version.length() - 9);
    }
    Matcher matcher = SNAPSHOT_TIMESTAMP.matcher(version);
    if (matcher.matches()) {
      return matcher.group("base");
    } else {
      return version;
    }
  }
}
