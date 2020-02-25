package se.vandmo.dependencylock.maven;

import static org.apache.maven.plugins.annotations.LifecyclePhase.VALIDATE;
import static org.apache.maven.plugins.annotations.ResolutionScope.TEST;

import java.io.File;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;


@Mojo(
  name = "check",
  defaultPhase = VALIDATE,
  requiresDependencyResolution = TEST)
public final class CheckMojo extends AbstractMojo {

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
    if (!lockFile.exists()) {
      getLog().error("No lock file found, create one by running 'mvn se.vandmo:dependency-lock-maven-plugin:lock'");
      return;
    }
    LockedDependencies lockedDependencies = lockFile.read();
    Artifacts actualDependencies = Artifacts.from(project.getArtifacts());
    LockedDependencies.Diff diff = lockedDependencies.compareWith(actualDependencies, project.getVersion());
    if (diff.equals()) {
      getLog().info("Actual dependencies matches locked dependencies");
    } else {
      diff.logTo(getLog());
      throw new MojoExecutionException("Dependencies differ");
    }
  }

}
