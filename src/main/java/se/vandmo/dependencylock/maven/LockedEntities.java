package se.vandmo.dependencylock.maven;

import static java.lang.String.format;
import static java.util.Locale.ROOT;
import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.maven.plugin.logging.Log;

public class LockedEntities<EntityType extends LockableEntity<EntityType>>
    extends AbstractLockedEntries<EntityType> {
  public final LockableEntities<EntityType> lockedEntities;

  LockedEntities(LockableEntities<EntityType> lockedEntities, Log log) {
    super(log);
    this.lockedEntities = requireNonNull(lockedEntities);
  }

  final DiffReport compareWith(LockableEntities<EntityType> actualEntities, Filters filters) {
    List<String> missing = new ArrayList<>();
    List<String> different = new ArrayList<>();
    collectDiff(actualEntities, filters, different, missing);
    List<String> extraneous = diffHelper.findExtraneous(actualEntities, lockedEntities, filters);
    return new DiffReport(different, missing, extraneous);
  }

  private void collectDiff(
      LockableEntities<EntityType> actualEntities,
      Filters filters,
      List<String> different,
      List<String> missing) {
    for (EntityType lockedEntity : lockedEntities) {
      final ArtifactIdentifier identifier = lockedEntity.getArtifactIdentifier();
      final Optional<EntityType> possiblyOtherEntity = actualEntities.by(identifier);
      if (!possiblyOtherEntity.isPresent()) {
        if (filters.allowMissing(lockedEntity)) {
          log.info(format(ROOT, "Ignoring missing %s", identifier));
        } else {
          missing.add(identifier.toString());
        }
      } else {
        EntityType actualEntity = possiblyOtherEntity.get();
        AtomicReference<EntityType> lockedEntityRef = new AtomicReference<>(lockedEntity);
        List<String> wrongs = findDiffs(lockedEntityRef, actualEntity, filters);
        if (!wrongs.isEmpty()) {
          different.add(formatDifference(lockedEntityRef.get(), actualEntity, wrongs));
        }
      }
    }
  }
}
