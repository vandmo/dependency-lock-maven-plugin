package se.vandmo.textchecker.maven;

import java.io.File;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;


@Mojo(
  name = "lock",
  requiresDependencyResolution = ResolutionScope.COMPILE)
public final class LockMojo extends AbstractMojo {

  @Parameter(
    defaultValue = "${basedir}",
    required = true,
    readonly = true)
  private File baseFolder;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    try {
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

}
