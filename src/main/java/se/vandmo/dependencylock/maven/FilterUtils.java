package se.vandmo.dependencylock.maven;

import static java.util.stream.Collectors.toList;

/** Helper methods for applying filters. */
public final class FilterUtils {
  private FilterUtils() {}

  public static Dependencies apply(Filters filters, Dependencies src) {
    return Dependencies.fromDependencies(
        src.stream().map(artifact -> modify(artifact, filters)).collect(toList()));
  }

  private static Parents apply(Filters filters, Parents src) {
    return new Parents(src.stream().map(parent -> modify(parent, filters)).collect(toList()));
  }

  public static Project apply(Filters filters, Project src) {
    return Project.from(
        apply(filters, src.dependencies),
        src.parents.map(parents -> apply(filters, parents)),
        src.plugins.map(plugins -> apply(filters, plugins)),
        src.extensions.map(extensions -> apply(filters, extensions)));
  }

  private static Plugins apply(Filters filters, Plugins src) {
    return Plugins.from(src.stream().map(plugin -> modify(plugin, filters)).collect(toList()));
  }

  private static Extensions apply(Filters filters, Extensions src) {
    return Extensions.from(src.stream().map(plugin -> modify(plugin, filters)).collect(toList()));
  }

  private static <T extends LockableEntity<T>> T modify(T lockableEntity, Filters filters) {
    T result = lockableEntity;
    result = ignoreVersionIfRelevant(result, filters);
    result = ignoreIntegrityIfRelevant(result, filters);
    return result;
  }

  private static <T extends LockableEntity<T>> T ignoreIntegrityIfRelevant(
      T lockableEntity, Filters filters) {
    if (filters
        .integrityConfiguration(lockableEntity)
        .equals(DependencySetConfiguration.Integrity.ignore)) {
      return lockableEntity.withIntegrity(Integrity.Ignored());
    }
    return lockableEntity;
  }

  private static Plugin modify(Plugin plugin, Filters filters) {
    Plugin result = plugin;
    result = ignoreVersionIfRelevant(result, filters);
    result = ignoreIntegrityIfRelevant(result, filters);
    result =
        result.withDependencies(
            Artifacts.fromArtifacts(
                result.dependencies.stream()
                    .map(artifact -> modify(artifact, filters))
                    .collect(toList())));
    return result;
  }

  private static <T extends LockableEntity<T>> T ignoreVersionIfRelevant(
      T lockableEntity, Filters filters) {
    if (filters
        .versionConfiguration(lockableEntity)
        .type
        .equals(DependencySetConfiguration.Version.ignore)) {
      return lockableEntity.withVersion("ignored");
    }
    return lockableEntity;
  }
}
