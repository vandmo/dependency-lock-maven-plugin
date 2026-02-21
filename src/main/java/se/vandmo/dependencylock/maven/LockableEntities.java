package se.vandmo.dependencylock.maven;

import static java.util.Collections.unmodifiableList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Instances of this class shall be used to gather a sorted list of {@link LockableEntity}
 * instances.
 *
 * @param <Type> the type of {@link LockableEntity} this implementation would be handling
 */
public abstract class LockableEntities<Type extends LockableEntity<Type>>
    implements Iterable<Type> {

  private final List<Type> entities;

  LockableEntities(Collection<Type> contents, boolean sort) {
    ArrayList<Type> copy = new ArrayList<>(contents);
    if (sort) {
      copy.sort(
          new Comparator<Type>() {
            @Override
            public int compare(Type o1, Type o2) {
              return o1.getArtifactIdentifier().compareTo(o2.getArtifactIdentifier());
            }
          });
    }
    this.entities = unmodifiableList(copy);
  }

  public final Optional<Type> by(ArtifactIdentifier identifier) {
    for (Type entity : entities) {
      if (identifier.equals(entity.getArtifactIdentifier())) {
        return Optional.of(entity);
      }
    }
    return Optional.empty();
  }

  @Override
  public final Iterator<Type> iterator() {
    return entities.iterator();
  }

  public final Stream<Type> stream() {
    return entities.stream();
  }

  public abstract Stream<Artifact> artifacts();

  public final int size() {
    return entities.size();
  }
}
