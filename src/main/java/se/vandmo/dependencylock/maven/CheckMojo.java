package se.vandmo.dependencylock.maven;

import java.io.File;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;


@Mojo(
  name = "check",
  defaultPhase = LifecyclePhase.GENERATE_SOURCES,
  requiresDependencyResolution = ResolutionScope.COMPILE)
public final class CheckMojo extends AbstractMojo {

  @Parameter(
    defaultValue = "${basedir}",
    required = true,
    readonly = true)
  private File baseFolder;

  @Parameter(
    defaultValue="${project}",
    required = true,
    readonly = true)
  private MavenProject project;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
  }

}
