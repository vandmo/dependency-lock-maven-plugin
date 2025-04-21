package se.vandmo.dependencylock.maven;

import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

public final class Filters {

  private final List<DependencySetConfiguration> dependencySetConfigurations;
  private final String projectVersion;

  public Filters(List<DependencySetConfiguration> dependencySets, String projectVersion) {
    List<DependencySetConfiguration> dependencySetConfigurations = new ArrayList<>(dependencySets);
    Collections.reverse(dependencySetConfigurations);
    this.dependencySetConfigurations = unmodifiableList(dependencySetConfigurations);
    this.projectVersion = requireNonNull(projectVersion);
  }

  private <T> T configurationFor(
      Dependency dependency, Function<DependencySetConfiguration, T> extractor, T defaultValue) {
    return dependencySetConfigurations.stream()
        .filter(d -> d.matches(dependency))
        .map(extractor)
        .filter(v -> v != null)
        .findFirst()
        .orElse(defaultValue);
  }

  public VersionConfiguration versionConfiguration(Dependency artifact) {
    DependencySetConfiguration.Version type =
        configurationFor(artifact, d -> d.version, DependencySetConfiguration.Version.check);
    return new VersionConfiguration(type, projectVersion);
  }

  public DependencySetConfiguration.Integrity integrityConfiguration(Dependency artifact) {
    return configurationFor(artifact, d -> d.integrity, DependencySetConfiguration.Integrity.check);
  }

  public boolean allowSuperfluous(Dependency artifact) {
    return configurationFor(artifact, d -> d.allowSuperfluous, Boolean.FALSE);
  }

  public boolean allowMissing(Dependency artifact) {
    return configurationFor(artifact, d -> d.allowMissing, Boolean.FALSE);
  }

  public static final class VersionConfiguration {
    public final DependencySetConfiguration.Version type;
    public final String projectVersion;

    private VersionConfiguration(DependencySetConfiguration.Version type, String projectVersion) {
      this.type = type;
      this.projectVersion = projectVersion;
    }
  }
}
