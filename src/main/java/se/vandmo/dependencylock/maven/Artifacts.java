package se.vandmo.dependencylock.maven;

import static java.lang.String.format;
import static java.util.Collections.sort;
import static java.util.Collections.unmodifiableList;
import static java.util.Locale.ROOT;
import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.apache.maven.plugin.logging.Log;

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

  public Diff compareWith(Artifacts other) {
    List<String> missing = new ArrayList<>();
    List<String> different = new ArrayList<>();
    List<String> added = new ArrayList<>();
    for (Artifact artifact : artifacts) {
      Optional<Artifact> otherArtifact = other.byGroupIdAndArtifactId(artifact.groupId, artifact.artifactId);
      if (!otherArtifact.isPresent()) {
        missing.add(artifact.toString());
      } else if (!artifact.equals(otherArtifact.get())) {
        different.add(format(ROOT, "Expected %s but found %s", artifact.toString(), otherArtifact.get().toString()));
      }
    }
    for (Artifact otherArtifact : other.artifacts) {
      if (!byGroupIdAndArtifactId(otherArtifact.groupId, otherArtifact.artifactId).isPresent()) {
        added.add(otherArtifact.toString());
      }
    }
    return new Diff(missing, different, added);
  }

  public Optional<Artifact> byGroupIdAndArtifactId(String groupId, String artifactId) {
    for (Artifact artifact : artifacts) {
      if (artifact.groupId.equals(groupId) && artifact.artifactId.equals(artifactId)) {
        return Optional.of(artifact);
      }
    }
    return Optional.empty();
  }

  public static final class Diff {
    private final List<String> missing;
    private final List<String> different;
    private final List<String> added;
    private Diff(List<String> missing, List<String> different, List<String> added) {
      this.missing = missing;
      this.different = different;
      this.added = added;
    }
    public boolean equals() {
      return missing.isEmpty() && different.isEmpty() && added.isEmpty();
    }
    public void logTo(Log log) {
      if (!missing.isEmpty()) {
        log.error("Missing dependencies:");
        missing.forEach(line -> log.error("  "+line));
      }
      if (!different.isEmpty()) {
        log.error("The following dependencies differ:");
        different.forEach(line -> log.error("  "+line));
      }
      if (!added.isEmpty()) {
        log.error("Extraneous dependencies:");
        added.forEach(line -> log.error("  "+line));
      }
    }
  }
}
