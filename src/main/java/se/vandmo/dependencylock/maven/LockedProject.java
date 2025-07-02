package se.vandmo.dependencylock.maven;

import static java.util.Objects.requireNonNull;

import java.util.Optional;
import java.util.stream.Stream;
import org.apache.maven.plugin.logging.Log;

public final class LockedProject {
  public final Optional<Parents> parents;
  public final Optional<Plugins> plugins;
  public final Optional<Extensions> extensions;
  public final Dependencies dependencies;
  private final Log log;

  private LockedProject(
      Optional<Parents> parents, Optional<Plugins> plugins, Optional<Extensions> extensions, Dependencies dependencies, Log log) {
    this.parents = parents;
    this.dependencies = dependencies;
    this.build = build;
    this.log = log;
  }

  public static LockedProject from(Project project, Log log) {
    return new LockedProject(project.parents, project.build, project.dependencies, log);
  }

  public static LockedProject from(Dependencies dependencies, Optional<Parents> parents, Optional<Plugins> plugins, Optional<Extensions> extensions, Log log) {
    return new LockedProject(
        parent, Optional.of(build), requireNonNull(dependencies), log);
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
    private final DiffReport dependenciesDiff;
    private final Optional<LockedBuild.Diff> buildDiff;
    private final Optional<DiffReport> parentDiff;

    Diff(DiffReport dependenciesDiff, DiffReport parentsDiff, LockedBuild.Diff buildDiff) {
      this(Optional.of(buildDiff), Optional.of(parentsDiff), dependenciesDiff);
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
