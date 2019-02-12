package se.vandmo.dependencylock.maven;

import static java.util.Collections.sort;
import static java.util.Collections.unmodifiableList;
import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class Artifacts {

  public final List<Artifact> artifacts;

  Artifacts(List<Artifact> artifacts) {
    ArrayList<Artifact> copy = new ArrayList<>(artifacts);
    sort(copy);
    this.artifacts = unmodifiableList(copy);
  }

  public static Artifacts from(Set<org.apache.maven.artifact.Artifact> artifacts) {
    return new Artifacts(artifacts.stream().map(Artifact::from).collect(toList()));
  }

  public Optional<Artifact> by(ArtifactIdentifier identifier) {
    for (Artifact artifact : artifacts) {
      if (identifier.equals(artifact.identifier)) {
        return Optional.of(artifact);
      }
    }
    return Optional.empty();
  }

}
