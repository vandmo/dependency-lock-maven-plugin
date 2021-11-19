package se.vandmo.dependencylock.maven;

import static org.apache.maven.plugins.annotations.ResolutionScope.TEST;

import java.io.File;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;


@Mojo(
  name = "create-lock-file",
  requiresDependencyResolution = TEST)
public final class CreateLockFileMojo extends AbstractMojo {

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
  public void execute() {
    DependenciesLockFile lockFile = DependenciesLockFile.fromBasedir(basedir, filename);
    LockedDependencies lockedDependencies = LockedDependencies.from(Artifacts.from(project.getArtifacts()), getLog());
    lockFile.write(lockedDependencies);
  }

}
