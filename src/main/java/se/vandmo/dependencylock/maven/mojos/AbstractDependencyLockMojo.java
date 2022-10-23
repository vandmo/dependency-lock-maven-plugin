package se.vandmo.dependencylock.maven.mojos;

import java.io.File;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import se.vandmo.dependencylock.maven.Artifacts;
import se.vandmo.dependencylock.maven.DependenciesLockFileAccessor;
import se.vandmo.dependencylock.maven.LockFileFormat;
import se.vandmo.dependencylock.maven.PomMinimums;

public abstract class AbstractDependencyLockMojo extends AbstractMojo {

  @Parameter(defaultValue = "${basedir}", required = true, readonly = true)
  private File basedir;

  @Parameter(defaultValue = "${project}", required = true, readonly = true)
  private MavenProject project;

  @Parameter(property = "dependencyLock.filename")
  private String filename;

  @Parameter(property = "dependencyLock.format")
  private LockFileFormat format = LockFileFormat.json;

  DependenciesLockFileAccessor lockFile() {
    return format.dependenciesLockFileAccessor_fromBasedirAndFilename(basedir, filename);
  }

  Artifacts projectDependencies() {
    return Artifacts.fromMavenArtifacts(project.getArtifacts());
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
