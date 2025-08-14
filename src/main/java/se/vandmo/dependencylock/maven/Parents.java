package se.vandmo.dependencylock.maven;

import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.project.MavenProject;

public final class Parents extends LockableEntitiesWithArtifact<Parent>
    implements Iterable<Parent> {
  private static final class ParentCursor {
    private final MavenProject mavenProject;
    private final Artifact artifact;

    private ParentCursor(MavenProject mavenProject, Artifact artifact) {
      this.mavenProject = Objects.requireNonNull(mavenProject);
      this.artifact = Objects.requireNonNull(artifact);
    }

    static ParentCursor initializeIfPresent(MavenProject mavenProject) {
      return createIfPresent(mavenProject);
    }

    private static ParentCursor createIfPresent(MavenProject mavenProject) {
      final MavenProject parentMavenProject = mavenProject.getParent();
      if (null == parentMavenProject) {
        return null;
      }
      return new ParentCursor(parentMavenProject, mavenProject.getParentArtifact());
    }

    private MavenProject getMavenProject() {
      return mavenProject;
    }

    File getFile() {
      /*
       * Two cases here:
       * 1. The current parent was loaded from a relative path location (in which case currentParent.getFile is not null)
       * 2. The current parent was loaded from a remote artifact location (in which case the currentParentArtifact.getFile is not null)
       */
      File result = getMavenProject().getFile();
      if (null == result) {
        result = getArtifact().getFile();
      }
      return result;
    }

    private Artifact getArtifact() {
      return artifact;
    }

    ArtifactIdentifier getArtifactIdentifier() {
      return ArtifactIdentifier.from(getArtifact());
    }

    String getVersion() {
      return getMavenProject().getVersion();
    }

    String getId() {
      return getMavenProject().getId();
    }

    ParentCursor getParent() {
      return createIfPresent(getMavenProject());
    }
  }

  public Parents(List<Parent> parents) {
    super(unmodifiableList(new ArrayList<>(requireNonNull(parents))), false);
  }

  public static Parents from(MavenProject project) {
    List<Parent> parents = new ArrayList<>();
    ParentCursor cursor = ParentCursor.initializeIfPresent(project);
    while (cursor != null) {
      final File file = cursor.getFile();
      if (null == file) {
        throw new MojoExecutionRuntimeException(
            "Invalid parent reference found: " + cursor.getId(), new FileNotFoundException());
      }
      final String integrity = Checksum.calculateFor(file);
      parents.add(
          Parent.builder()
              .artifactIdentifier(cursor.getArtifactIdentifier())
              .version(cursor.getVersion())
              .integrity(integrity)
              .build());
      cursor = cursor.getParent();
    }
    return new Parents(parents);
  }
}
