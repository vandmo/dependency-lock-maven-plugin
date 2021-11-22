package se.vandmo.dependencylock.maven;

import static org.apache.maven.plugins.annotations.ResolutionScope.TEST;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;


@Mojo(
  name = "format",
  requiresDependencyResolution = TEST)
public final class FormatMojo extends AbstractDependencyLockMojo {

  @Override
  public void execute() throws MojoFailureException {
    getLog().warn(""
        + "The 'format' goal is deprecated. "
        + "Use 'create-lock-file' instead and avoid editing the lock file.");
    switch (format()) {
      case json:
        DependenciesLockFileJson
            .from(lockFile())
            .format(getLog());
        break;
      default:
        throw new MojoFailureException("Can only format json lock files");
    }
  }

}
