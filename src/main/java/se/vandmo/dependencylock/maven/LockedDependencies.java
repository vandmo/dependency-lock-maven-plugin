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
  private final DiffHelper diffHelper;
  private final Log log;

  private LockedDependencies(Dependencies lockedDependencies, Log log) {
    this.lockedDependencies = lockedDependencies;
    this.diffHelper = new DiffHelper(log);
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
      return diffHelper.diffVersion(
          lockedDependencyRef, actualDependency, Dependency::withVersion, filters);
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
        Dependency lockedDependency, Dependency actualDependency, Filters filters) {
      return diffHelper.diffIntegrity(lockedDependency, actualDependency, filters);
    }
  }

  private List<String> findExtraneous(Dependencies dependencies, Filters filters) {
    return diffHelper.findExtraneous(dependencies, lockedDependencies, filters);
  }

  public static final class Diff {
    private final DiffReport diffReport;

    private Diff(LockFileExpectationsDiff lockFileExpectationsDiff, List<String> extraneous) {
      this.diffReport =
          new DiffReport(
              lockFileExpectationsDiff.different, lockFileExpectationsDiff.missing, extraneous);
    }

    DiffReport getReport() {
      return diffReport;
    }

    public boolean equals() {
      return this.diffReport.equals();
    }

    public void logTo(Log log) {
      this.diffReport.report("dependencies").forEach(log::error);
    }
  }
}
