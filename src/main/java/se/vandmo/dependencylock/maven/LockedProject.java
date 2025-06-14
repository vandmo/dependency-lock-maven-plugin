package se.vandmo.dependencylock.maven;

import static java.util.Objects.requireNonNull;

import java.util.Optional;
import java.util.stream.Stream;
import org.apache.maven.plugin.logging.Log;

public final class LockedProject {
  public final Optional<Parent> parent;
  public final Optional<Build> build;
  public final Dependencies dependencies;
  private final Log log;

  private LockedProject(
      Optional<Parent> parent, Optional<Build> build, Dependencies dependencies, Log log) {
    this.parent = parent;
    this.dependencies = dependencies;
    this.build = build;
    this.log = log;
  }

  public static LockedProject from(Project project, Log log) {
    return new LockedProject(project.parent, project.build, project.dependencies, log);
  }

  public static LockedProject from(Dependencies dependencies, Build build, Parent parent, Log log) {
    return new LockedProject(
        Optional.ofNullable(parent), Optional.of(build), requireNonNull(dependencies), log);
  }

  public static LockedProject from(Dependencies dependencies, Log log) {
    return new LockedProject(Optional.empty(), Optional.empty(), requireNonNull(dependencies), log);
  }

  public Diff compareWith(Project project, Filters filters) {
    final DiffReport dependenciesDiff =
        LockedDependencies.from(dependencies, log)
            .compareWith(project.dependencies, filters)
            .getReport();
    if (build.isPresent()) {
      return new Diff(
          dependenciesDiff,
          LockedParents.from(Parents.fromParent(project.parent.orElse(null)), log)
              .compareWith(Parents.fromParent(parent.orElse(null)), filters),
          LockedBuild.from(build.get(), log)
              .compareWith(project.build.orElse(Build.empty()), filters));
    }
    return new Diff(dependenciesDiff);
  }

  public static final class Diff {
    private final Optional<LockedBuild.Diff> buildDiff;
    private final DiffReport dependenciesDiff;
    private final Optional<DiffReport> parentDiff;

    Diff(DiffReport dependenciesDiff, DiffReport parentDiff, LockedBuild.Diff buildDiff) {
      this(Optional.of(buildDiff), Optional.of(parentDiff), dependenciesDiff);
    }

    Diff(DiffReport dependenciesDiff) {
      this(Optional.empty(), Optional.empty(), dependenciesDiff);
    }

    private Diff(
        Optional<LockedBuild.Diff> buildDiff,
        Optional<DiffReport> parentDiff,
        DiffReport dependenciesDiff) {
      this.buildDiff = buildDiff;
      this.parentDiff = parentDiff;
      this.dependenciesDiff = dependenciesDiff;
    }

    public boolean equals() {
      if (!dependenciesDiff.equals()) {
        return false;
      }
      if (buildDiff.isPresent() && !buildDiff.get().equals()) {
        return false;
      }
      if (parentDiff.isPresent() && !parentDiff.get().equals()) {
        return false;
      }
      return true;
    }

    public Stream<String> report() {
      Stream<String> result = dependenciesDiff.report("dependencies");
      result =
          Stream.concat(
              result, parentDiff.map(report -> report.report("parents")).orElse(Stream.empty()));
      result =
          Stream.concat(result, buildDiff.map(LockedBuild.Diff::report).orElse(Stream.empty()));
      return result;
    }

    public void logTo(Log log) {
      report().forEach(log::error);
    }
  }
}
