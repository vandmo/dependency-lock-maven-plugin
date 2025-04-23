package se.vandmo.dependencylock.maven;

import static java.util.Objects.requireNonNull;

import java.util.Optional;
import java.util.stream.Stream;
import org.apache.maven.plugin.logging.Log;

public final class LockedProject {
  public final Optional<Build> build;
  public final Dependencies dependencies;
  private final Log log;

  private LockedProject(Optional<Build> build, Dependencies dependencies, Log log) {
    this.dependencies = dependencies;
    this.build = build;
    this.log = log;
  }

  public static LockedProject from(Project project, Log log) {
    return new LockedProject(project.build, project.dependencies, log);
  }

  public static LockedProject from(Dependencies dependencies, Build build, Log log) {
    return new LockedProject(Optional.of(build), requireNonNull(dependencies), log);
  }

  public static LockedProject from(Dependencies dependencies, Log log) {
    return new LockedProject(Optional.empty(), requireNonNull(dependencies), log);
  }

  public Diff compareWith(Project project, Filters filters) {
    final DiffReport dependenciesDiff =
        LockedDependencies.from(dependencies, log)
            .compareWith(project.dependencies, filters)
            .getReport();
    if (build.isPresent()) {
      return new Diff(
          dependenciesDiff,
          LockedBuild.from(build.get(), log)
              .compareWith(project.build.orElse(Build.empty()), filters));
    }
    return new Diff(dependenciesDiff);
  }

  public static final class Diff {
    private final Optional<LockedBuild.Diff> buildDiff;
    private final DiffReport dependenciesDiff;

    Diff(DiffReport dependenciesDiff, LockedBuild.Diff buildDiff) {
      this(Optional.of(buildDiff), dependenciesDiff);
    }

    Diff(DiffReport dependenciesDiff) {
      this(Optional.empty(), dependenciesDiff);
    }

    private Diff(Optional<LockedBuild.Diff> buildDiff, DiffReport dependenciesDiff) {
      this.buildDiff = buildDiff;
      this.dependenciesDiff = dependenciesDiff;
    }

    public boolean equals() {
      if (!dependenciesDiff.equals()) {
        return false;
      }
      if (!buildDiff.isPresent()) {
        return true;
      }
      return buildDiff.get().equals();
    }

    public Stream<String> report() {
      return Stream.concat(
          dependenciesDiff.report("dependencies"),
          buildDiff.map(LockedBuild.Diff::report).orElse(Stream.empty()));
    }

    public void logTo(Log log) {
      report().forEach(log::error);
    }
  }
}
