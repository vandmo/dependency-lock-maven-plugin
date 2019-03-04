package se.vandmo.dependencylock.maven;

import java.io.File;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import static org.apache.maven.plugins.annotations.ResolutionScope.TEST;


@Mojo(
  name = "format",
  requiresDependencyResolution = TEST)
public final class FormatMojo extends AbstractMojo {

  @Parameter(
    defaultValue = "${basedir}",
    required = true,
    readonly = true)
  private File basedir;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    DependenciesLockFile.fromBasedir(basedir).format();
  }

}
