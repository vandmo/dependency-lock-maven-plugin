package se.vandmo.dependencylock.maven;

import static java.lang.String.format;
import static java.util.Locale.ROOT;
import static java.util.Objects.requireNonNull;
import static se.vandmo.dependencylock.maven.lang.Strings.joinNouns;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.maven.plugin.logging.Log;

public final class LockedArtifacts {

  public final Artifacts lockedArtifacts;
  private final DiffHelper diffHelper;
  private final Log log;

  private LockedArtifacts(Artifacts lockedArtifacts, Log log) {
    this.lockedArtifacts = lockedArtifacts;
    this.diffHelper = new DiffHelper(log);
    this.log = log;
  }

  public static LockedArtifacts from(Artifacts artifacts, Log log) {
    return new LockedArtifacts(requireNonNull(artifacts), log);
  }

  public DiffReport compareWith(Artifacts dependencies, Filters filters) {
    LockFileExpectationsDiff expectationsDiff = new LockFileExpectationsDiff(dependencies, filters);
    List<String> extraneous = diffHelper.findExtraneous(dependencies, lockedArtifacts, filters);
    return new DiffReport(expectationsDiff.different, expectationsDiff.missing, extraneous);
  }

  private final class LockFileExpectationsDiff {
    private List<String> missing = new ArrayList<>();
    private List<String> different = new ArrayList<>();

    private LockFileExpectationsDiff(Artifacts artifacts, Filters filters) {
      for (Artifact lockedDependency : lockedArtifacts) {
        final ArtifactIdentifier identifier = lockedDependency.identifier;
        Optional<Artifact> possiblyOtherArtifact = artifacts.by(identifier);
        if (!possiblyOtherArtifact.isPresent()) {
          if (filters.allowMissing(lockedDependency)) {
            log.info(format(ROOT, "Ignoring missing %s", identifier));
          } else {
            missing.add(identifier.toString());
          }
        } else {
          Artifact actualDependency = possiblyOtherArtifact.get();
          AtomicReference<Artifact> lockedDependencyRef = new AtomicReference<>(lockedDependency);
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
        AtomicReference<Artifact> lockedArtifactRef, Artifact actualArtifact, Filters filters) {
      List<String> wrongs = new ArrayList<>();
      Artifact lockedDependency = lockedArtifactRef.get();
      wrongs.addAll(diffHelper.diffIntegrity(lockedDependency, actualArtifact, filters));
      wrongs.addAll(
          diffHelper.diffVersion(
              lockedArtifactRef, actualArtifact, Artifact::withVersion, filters));
      return wrongs;
    }
  }
}
