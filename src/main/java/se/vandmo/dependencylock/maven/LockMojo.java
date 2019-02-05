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

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    DependenciesLockFile.fromBasedir(basedir).write(Artifacts.from(project.getArtifacts()));
  }

}
