package se.vandmo.dependencylock.maven;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.maven.plugin.logging.Log;

public final class LockedDependencies extends LockedEntities<Dependency> {

  private LockedDependencies(Dependencies lockedDependencies, Log log) {
    super(lockedDependencies, log);
  }

  public static LockedDependencies from(Dependencies artifacts, Log log) {
    return new LockedDependencies(requireNonNull(artifacts), log);
  }

  public Diff compareWith(Dependencies dependencies, Filters filters) {
    return new Diff(super.compareWith(dependencies, filters));
  }

  @Override
  List<String> findDiffs(
      AtomicReference<Dependency> lockedDependencyRef,
      Dependency actualDependency,
      Filters filters) {
    List<String> wrongs = new ArrayList<>();
    wrongs.addAll(diffOptional(lockedDependencyRef.get(), actualDependency));
    wrongs.addAll(diffScope(lockedDependencyRef.get(), actualDependency));
    wrongs.addAll(diffIntegrity(lockedDependencyRef, actualDependency, filters));
    wrongs.addAll(
        diffVersion(lockedDependencyRef, actualDependency, Dependency::withVersion, filters));
    return wrongs;
  }

  private List<String> diffOptional(Dependency lockedDependency, Dependency actualDependency) {
    if (lockedDependency.optional == actualDependency.optional) {
      return emptyList();
    } else {
      return singletonList("optional");
    }
  }

  private List<String> diffScope(Dependency lockedDependency, Dependency actualDependency) {
    if (lockedDependency.scope.equals(actualDependency.scope)) {
      return emptyList();
    } else {
      return singletonList("scope");
    }
  }

  public static final class Diff {
    Diff(DiffReport diffReport) {
      this.diffReport = diffReport;
    }

    private final DiffReport diffReport;

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
