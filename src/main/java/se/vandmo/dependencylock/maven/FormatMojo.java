package se.vandmo.dependencylock.maven;

import static org.apache.maven.plugins.annotations.ResolutionScope.TEST;

import java.io.File;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;


@Mojo(
  name = "format",
  requiresDependencyResolution = TEST)
public final class FormatMojo extends AbstractMojo {

  @Parameter(
    defaultValue = "${basedir}",
    required = true,
    readonly = true)
  private File basedir;

  @Parameter(defaultValue = DependenciesLockFile.DEFAULT_FILENAME)
  private String filename;

  @Override
  public void execute() {
    getLog().warn(""
        + "The 'format' goal is deprecated. "
        + "Use 'create-lock-file' instead and avoid editing the lock file.");
    DependenciesLockFile.fromBasedir(basedir, filename).format(getLog());
  }

}
