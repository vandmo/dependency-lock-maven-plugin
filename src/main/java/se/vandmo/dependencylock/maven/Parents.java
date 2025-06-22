package se.vandmo.dependencylock.maven;

import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.List;
import org.apache.maven.project.MavenProject;

public final class Parents extends LockableEntitiesWithArtifact<Parent>
    implements Iterable<Parent> {

  public Parents(List<Parent> parents) {
    super(unmodifiableList(new ArrayList<>(requireNonNull(parents))));
  }

  public static Parents from(MavenProject project) {
    List<Parent> parents = new ArrayList<>();
    project = project.getParent();
    while (project != null) {
      final org.apache.maven.artifact.Artifact parentArtifact = project.getParentArtifact();
      String integrity = Checksum.calculateFor(parentArtifact.getFile());
      parents.add(
          Parent.builder()
              .artifactIdentifier(ArtifactIdentifier.from(parentArtifact))
              .version(parentArtifact.getVersion())
              .integrity(integrity)
              .build());
      project = project.getParent();
    }
    return new Parents(parents);
  }
}
