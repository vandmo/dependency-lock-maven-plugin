package se.vandmo.dependencylock.maven;

import static org.apache.maven.plugins.annotations.ResolutionScope.TEST;

import java.io.File;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;


@Mojo(
  name = "lock",
  requiresDependencyResolution = TEST)
public final class LockMojo extends AbstractMojo {

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

  @Parameter(defaultValue = DependenciesLockFile.DEFAULT_FILENAME)
  private String filename;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    DependenciesLockFile lockFile = DependenciesLockFile.fromBasedir(basedir, filename);
    LockedDependencies existingLockedDependencies = getExistingLockedDependencies(lockFile);
    LockedDependencies lockedDependencies = existingLockedDependencies.updateWith(Artifacts.from(project.getArtifacts()));
    lockFile.write(lockedDependencies);
  }

  private LockedDependencies getExistingLockedDependencies(DependenciesLockFile lockFile) {
    if (lockFile.exists()) {
      return lockFile.read();
    } else {
      return LockedDependencies.empty();
    }
  }

}
