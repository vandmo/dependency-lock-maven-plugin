package se.vandmo.dependencylock.maven;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.apache.maven.project.MavenProject;

import static java.util.Collections.emptyList;

public final class Parents extends LockableEntitiesWithArtifacts<Artifact> implements Iterable<Artifact> {

  private final List<Artifact> hierarchy;

  private Parents(List<Artifact> artifacts) {
    super();
    this.hierarchy = Collections.unmodifiableList(new ArrayList<>(artifacts));
  }

  public static Parents fromMavenProject(MavenProject mavenProject) {
    MavenProject currentProject = mavenProject;
    List<Artifact> parentHierarchy = new ArrayList<>();
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
          Artifact.builder()
              .artifactIdentifier(ArtifactIdentifier.from(parentArtifact))
              .version(parentArtifact.getVersion())
              .integrity(integrity)
              .build());
      currentProject = parentProject;
    }
    return new Parents(parentHierarchy);
  }

  public static Parents from(MavenProject project) {
    List<Artifact> parents = new ArrayList<>();
    project = project.getParent();
    while (project != null) {
      final org.apache.maven.artifact.Artifact parentArtifact = project.getParentArtifact();
      String integrity = Checksum.calculateFor(parentArtifact.getFile());
      parents.add(Artifact.builder()
              .artifactIdentifier(ArtifactIdentifier.from(parentArtifact))
              .version(parentArtifact.getVersion())
              .integrity(integrity)
              .build());
      project = project.getParent();
    }
    return new Parents(parents);
  }

  @Override
  public Iterator<Artifact> iterator() {
    return this.hierarchy.iterator();
  }
}
