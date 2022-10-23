package se.vandmo.dependencylock.maven;

import static java.util.Collections.unmodifiableList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

public final class Filters {

  private final List<DependencySetConfiguration> dependencySetConfigurations;

  public Filters(List<DependencySetConfiguration> dependencySets) {
    List<DependencySetConfiguration> dependencySetConfigurations = new ArrayList<>(dependencySets);
    Collections.reverse(dependencySetConfigurations);
    this.dependencySetConfigurations = unmodifiableList(dependencySetConfigurations);
  }

  private <T> T configurationFor(
      Artifact artifact, Function<DependencySetConfiguration, T> extractor, T defaultValue) {
    return dependencySetConfigurations.stream()
        .filter(d -> d.matches(artifact))
        .map(extractor)
        .filter(v -> v != null)
        .findFirst()
        .orElse(defaultValue);
  }

  public DependencySetConfiguration.Version versionConfiguration(Artifact artifact) {
    return configurationFor(artifact, d -> d.version, DependencySetConfiguration.Version.check);
  }

  public DependencySetConfiguration.Integrity integrityConfiguration(Artifact artifact) {
    return configurationFor(artifact, d -> d.integrity, DependencySetConfiguration.Integrity.check);
  }

  public boolean allowSuperfluous(Artifact artifact) {
    return configurationFor(artifact, d -> d.allowSuperfluous, Boolean.FALSE);
  }

  public boolean allowMissing(Artifact artifact) {
    return configurationFor(artifact, d -> d.allowMissing, Boolean.FALSE);
  }
}
