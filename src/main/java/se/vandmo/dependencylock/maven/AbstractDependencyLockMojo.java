package se.vandmo.dependencylock.maven;

import java.io.File;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;


public abstract class AbstractDependencyLockMojo extends AbstractMojo {

  @Parameter(
    defaultValue = "${basedir}",
    required = true,
    readonly = true)
  private File basedir;

  @Parameter(
    defaultValue="${project}",
    required = true,
    readonly = true)
  private MavenProject project;

  @Parameter
  private String filename;

  @Parameter
  private LockFileFormat format = LockFileFormat.json;

  DependenciesLockFileAccessor lockFile() {
    return format.dependenciesLockFileAccessor_fromBasedirAndFilename(basedir, filename);
  }

  Artifacts projectDependencies() {
    return Artifacts.from(project.getArtifacts());
  }

  PomMinimums pomMinimums() {
    return PomMinimums.from(project);
  }

  String projectVersion() {
    return project.getVersion();
  }

  LockFileFormat format() {
    return format;
  }
}
