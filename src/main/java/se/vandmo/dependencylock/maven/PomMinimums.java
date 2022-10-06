package se.vandmo.dependencylock.maven;

import static java.util.Objects.requireNonNull;

import org.apache.maven.project.MavenProject;

public final class PomMinimums {

  public final String groupId;
  public final String artifactId;
  public final String version;

  public static PomMinimums from(MavenProject mavenProject) {
    return new PomMinimums(
        mavenProject.getGroupId(), mavenProject.getArtifactId(), mavenProject.getVersion());
  }

  PomMinimums(String groupId, String artifactId, String version) {
    this.groupId = requireNonNull(groupId);
    this.artifactId = requireNonNull(artifactId);
    this.version = requireNonNull(version);
  }
}
