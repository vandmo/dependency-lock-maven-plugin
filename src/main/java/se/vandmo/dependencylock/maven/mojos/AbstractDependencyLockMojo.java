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

  @Parameter(
      property = "dependencyLock.filename"
  )
  private String filename;

  @Parameter(
      property = "dependencyLock.format"
  )
  private LockFileFormat format = LockFileFormat.json;

  @Parameter(
     property = "dependencyLock.integrityChecking"
  )
  private boolean dependencyIntegrityChecking = false;

  /**
   * Update {@link #dependencyIntegrityChecking} to false if {@link #format} is set to {@link LockFileFormat#pom} since
   * we can't store integrity information in a pom.
   */
  protected final void updateDependencyIntegrityCheckingValue() {
    if (format == LockFileFormat.pom) {
        if (dependencyIntegrityChecking) {
            getLog().warn("Dependency integrity checking disabled because format is POM. Use JSON format if you want integrity verification.");
        }
        dependencyIntegrityChecking = false;
    }
  }

  DependenciesLockFileAccessor lockFile() {
    return format.dependenciesLockFileAccessor_fromBasedirAndFilename(basedir, filename);
  }

  Artifacts projectDependencies() {
    return Artifacts.from(project.getArtifacts(), dependencyIntegrityChecking());
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

  boolean dependencyIntegrityChecking() {
      return dependencyIntegrityChecking;
  }
}
