package se.vandmo.dependencylock.maven;

import static java.util.Collections.emptyList;
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

  public static Parents fromMavenProject(MavenProject mavenProject) {
    MavenProject currentProject = mavenProject;
    List<Parent> parentHierarchy = new ArrayList<>();
    while (currentProject != null) {
      final org.apache.maven.model.Parent parentModel = currentProject.getModel().getParent();
      if (parentModel == null) {
        return new Parents(emptyList());
      }
      final MavenProject parentProject = currentProject.getParent();
      if (parentProject == null) {
        throw new IllegalArgumentException(
            "Parent project is not expected to be null for project " + currentProject.getId());
      }
      final String integrity = Checksum.calculateFor(parentProject.getFile());

      final org.apache.maven.artifact.Artifact parentArtifact = currentProject.getParentArtifact();
      parentHierarchy.add(
          Parent.builder()
              .artifactIdentifier(ArtifactIdentifier.from(parentArtifact))
              .version(parentArtifact.getVersion())
              .integrity(integrity)
              .build());
      currentProject = parentProject;
    }
    return new Parents(parentHierarchy);
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
