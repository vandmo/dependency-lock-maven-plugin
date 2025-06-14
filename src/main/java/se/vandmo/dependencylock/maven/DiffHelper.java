package se.vandmo.dependencylock.maven;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Locale.ROOT;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import org.apache.maven.plugin.logging.Log;

final class DiffHelper {
  private final Log log;

  DiffHelper(Log log) {
    this.log = log;
  }

  <T extends LockableEntity<T>> List<String> diffIntegrity(
      T lockedEntity, T actualEntity, Filters filters) {
    if (lockedEntity.getIntegrity().equals(actualEntity.getIntegrity())) {
      return emptyList();
    } else {
      DependencySetConfiguration.Integrity integrityConfiguration =
          filters.integrityConfiguration(lockedEntity);
      switch (integrityConfiguration) {
        case check:
          return asList("integrity");
        case ignore:
          log.info(format(ROOT, "Ignoring integrity for %s", lockedEntity));
          return emptyList();
        default:
          throw new RuntimeException("Unsupported enum value");
      }
    }
  }

  <T extends LockableEntity<T>> List<String> findExtraneous(
      LockableEntities<T> entities, LockableEntities<T> lockedEntities, Filters filters) {
    List<String> extraneous = new ArrayList<>();
    for (T entity : entities) {
      if (!lockedEntities.by(entity.getArtifactIdentifier()).isPresent()) {
        if (filters.allowSuperfluous(entity)) {
          log.info(format(ROOT, "Ignoring extraneous %s", entity.getArtifactIdentifier()));
        } else {
          extraneous.add(entity.toString_withoutIntegrity());
        }
      }
    }
    return extraneous;
  }

  <EntityType extends LockableEntity<EntityType>> List<String> diffVersion(
      AtomicReference<EntityType> lockedEntityRef,
      EntityType actualEntity,
      BiFunction<EntityType, String, EntityType> versionUpdater,
      Filters filters) {
    EntityType lockedDependency = lockedEntityRef.get();
    Filters.VersionConfiguration versionConfiguration =
        filters.versionConfiguration(lockedDependency);
    switch (versionConfiguration.type) {
      case check:
        if (lockedDependency.getVersion().equals(actualEntity.getVersion())) {
          return emptyList();
        } else {
          return singletonList("version");
        }
      case useProjectVersion:
        log.info(format(ROOT, "Using project version for %s", lockedDependency));
        lockedEntityRef.set(
            versionUpdater.apply(lockedDependency, versionConfiguration.projectVersion));
        if (versionConfiguration.projectVersion.equals(actualEntity.getVersion())) {
          return emptyList();
        } else {
          return singletonList("version (expected project version)");
        }
      case snapshot:
        log.info(format(ROOT, "Allowing snapshot version for %s", lockedDependency));
        if (VersionUtils.snapshotMatch(lockedDependency.getVersion(), actualEntity.getVersion())) {
          return emptyList();
        } else {
          return singletonList("version (allowing snapshot version)");
        }
      case ignore:
        log.info(format(ROOT, "Ignoring version for %s", lockedDependency));
        return emptyList();
      default:
        throw new RuntimeException("Unsupported enum value");
    }
  }
}
