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
import se.vandmo.dependencylock.maven.versions.VersionConstraint;
import se.vandmo.dependencylock.maven.versions.VersionConstraintContext;
import se.vandmo.dependencylock.maven.versions.VersionConstraintVisitor;
import se.vandmo.dependencylock.maven.versions.VersionConstraints;

final class DiffHelper {
  private final Log log;
  private final VersionConstraintWithSnapshotConstraintRelaxed withSnapshotConstraintRelaxed;

  DiffHelper(Log log) {
    this.log = log;
    this.withSnapshotConstraintRelaxed = new VersionConstraintWithSnapshotConstraintRelaxed();
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
      BiFunction<EntityType, VersionConstraint, EntityType> versionUpdater,
      Filters filters) {
    EntityType lockedDependency = lockedEntityRef.get();
    Filters.VersionConfiguration versionConfiguration =
        filters.versionConfiguration(lockedDependency);
    switch (versionConfiguration.type) {
      case check:
        if (lockedDependency.getVersion().compliantWith(actualEntity.getVersion(), filters)) {
          return emptyList();
        } else {
          return singletonList("version");
        }
      case useProjectVersion:
        log.info(format(ROOT, "Using project version for %s", lockedDependency));
        lockedEntityRef.set(
            versionUpdater.apply(lockedDependency, VersionConstraints.version(versionConfiguration.projectVersion)));
        if (VersionConstraints.useProjectVersion()
            .compliantWith(actualEntity.getVersion(), filters)) {
          return emptyList();
        } else {
          return singletonList("version (expected project version)");
        }
      case snapshot:
        log.info(format(ROOT, "Allowing snapshot version for %s", lockedDependency));
        if (lockedDependency
            .getVersion()
            .accept(withSnapshotConstraintRelaxed, filters)
            .compliantWith(
                actualEntity.getVersion().accept(withSnapshotConstraintRelaxed, filters),
                filters)) {
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

  /**
   * Instances of this class shall be used to "downgrade" constraints on a given version to ignore
   * -SNAPSHOT suffixes.
   */
  private static final class VersionConstraintWithSnapshotConstraintRelaxed
      implements VersionConstraintVisitor<VersionConstraint, VersionConstraintContext> {
    @Override
    public VersionConstraint onVersion(String version, VersionConstraintContext context) {
      return VersionConstraints.version(VersionUtils.stripSnapshot(version));
    }

    @Override
    public VersionConstraint onProjectVersion(VersionConstraintContext context) {
      return onVersion(context.getProjectVersion(), context);
    }

    @Override
    public VersionConstraint onIgnoreVersion(VersionConstraintContext context) {
      return VersionConstraints.ignoreVersion();
    }
  }
}
