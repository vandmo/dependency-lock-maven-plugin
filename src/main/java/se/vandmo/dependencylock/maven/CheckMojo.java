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

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    DependenciesLockFile lockFile = DependenciesLockFile.fromBasedir(basedir);
    if (!lockFile.exists()) {
      getLog().error("No lock file found, create one by running 'mvn se.vandmo:dependency-lock-maven-plugin:lock'");
      return;
    }
    Artifacts lockedDependencies = lockFile.read();
    Artifacts actualDependencies = Artifacts.from(project.getArtifacts());
    Artifacts.Diff diff = lockedDependencies.compareWith(actualDependencies);
    if (diff.equals()) {
      getLog().info("Dependencies are the same");
    } else {
      diff.logTo(getLog());
      throw new MojoExecutionException("Dependencies differ");
    }
  }

}
