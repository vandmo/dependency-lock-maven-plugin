package se.vandmo.dependencylock.maven;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.maven.plugin.logging.Log;

public final class LockedParents extends AbstractLockedEntries<Artifact> {
  private final Parents parents;

  private LockedParents(Parents parents, Log log) {
    super(log);
    this.parents = parents;
  }

  public static LockedParents from(Parents artifacts, Log log) {
    return new LockedParents(requireNonNull(artifacts), log);
  }

  public DiffReport compareWith(Parents actualEntities, Filters filters) {
    List<String> missing = new ArrayList<>();
    List<String> different = new ArrayList<>();
    List<String> extraneous = new ArrayList<>();
    Iterator<Artifact> lockedEntitiesIterator =
        this.parents.stream().map(parent -> parent.artifact).iterator();
    Iterator<Artifact> actualEntitiesIterator =
        actualEntities.stream().map(parent -> parent.artifact).iterator();
    if (!lockedEntitiesIterator.hasNext()) {
      actualEntitiesIterator.forEachRemaining(
          artifact -> extraneous.add(artifact.toString_withoutIntegrity()));
    }
    if (!actualEntitiesIterator.hasNext()) {
      lockedEntitiesIterator.forEachRemaining(
          artifact -> missing.add(artifact.toString_withoutIntegrity()));
      return new DiffReport(missing, different, extraneous);
    }
    while (lockedEntitiesIterator.hasNext() && actualEntitiesIterator.hasNext()) {
      final Artifact currentLocked = lockedEntitiesIterator.next();
      final Artifact actual = actualEntitiesIterator.next();
      AtomicReference<Artifact> lockedEntityRef = new AtomicReference<>(currentLocked);
      final List<String> wrongs = findDiffs(lockedEntityRef, actual, filters);
      if (!wrongs.isEmpty()) {
        different.add(formatDifference(currentLocked, actual, wrongs));
      }
    }
    while (lockedEntitiesIterator.hasNext()) {
      missing.add(lockedEntitiesIterator.next().toString_withoutIntegrity());
    }
    while (actualEntitiesIterator.hasNext()) {
      extraneous.add(actualEntitiesIterator.next().toString_withoutIntegrity());
    }
    return new DiffReport(different, missing, extraneous);
  }

  List<String> findDiffs(
      AtomicReference<Artifact> lockedArtifactRef, Artifact actualArtifact, Filters filters) {
    List<String> wrongs = super.findDiffs(lockedArtifactRef, actualArtifact, filters);
    final ArtifactIdentifier expectedArtifactIdentifier =
        lockedArtifactRef.get().getArtifactIdentifier();
    final ArtifactIdentifier actualArtifactArtifactIdentifier =
        actualArtifact.getArtifactIdentifier();
    if (!expectedArtifactIdentifier.groupId.equals(actualArtifactArtifactIdentifier.groupId)) {
      wrongs.add("groupId");
    }
    if (!expectedArtifactIdentifier.artifactId.equals(
        actualArtifactArtifactIdentifier.artifactId)) {
      wrongs.add("artifactId");
    }
    wrongs.addAll(diffVersion(lockedArtifactRef, actualArtifact, Artifact::withVersion, filters));
    return wrongs;
  }
}
