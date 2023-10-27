package io.mvnpm.maven.locker.mojos;

import io.mvnpm.maven.locker.Artifacts;
import io.mvnpm.maven.locker.DependenciesLockFileAccessor;
import io.mvnpm.maven.locker.PomMinimums;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;

public abstract class AbstractDependencyLockMojo extends AbstractMojo {

  @Parameter(defaultValue = "${basedir}", required = true, readonly = true)
  private File basedir;

  @Parameter(defaultValue = "${project}", required = true, readonly = true)
  private MavenProject project;

  @Parameter(property = "dependencyLock.filename", defaultValue = ".maven-locker/pom.xml")
  private String filename;

  protected DependenciesLockFileAccessor lockFile() {
    return DependenciesLockFileAccessor.fromBasedir(basedir, filename);
  }

  protected Artifacts projectDependencies() {
    return Artifacts.fromMavenArtifacts(project.getArtifacts());
  }

  protected PomMinimums pomMinimums() {
    return PomMinimums.from(project);
  }

  protected String projectVersion() {
    return project.getVersion();
  }
}
