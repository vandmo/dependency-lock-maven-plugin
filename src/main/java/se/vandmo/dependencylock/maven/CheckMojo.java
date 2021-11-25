package se.vandmo.dependencylock.maven;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static org.apache.maven.plugins.annotations.LifecyclePhase.VALIDATE;
import static org.apache.maven.plugins.annotations.ResolutionScope.TEST;

import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.shared.artifact.filter.StrictPatternIncludesArtifactFilter;


@Mojo(
  name = "check",
  defaultPhase = VALIDATE,
  requiresDependencyResolution = TEST)
public final class CheckMojo extends AbstractDependencyLockMojo {

  @Parameter
  private String[] useMyVersionFor = new String[0];

  @Override
  public void execute() throws MojoExecutionException {
    DependenciesLockFile lockFile = lockFile();
    if (!lockFile.exists()) {
      throw new MojoExecutionException(
          "No lock file found, create one by running 'mvn se.vandmo:dependency-lock-maven-plugin:create-lock-file'");
    }
    ArtifactFilter useMyVersionForFilter = new StrictPatternIncludesArtifactFilter(asList(useMyVersionFor));
    LockedDependencies lockedDependencies = getLockedDependencies(lockFile);
    LockedDependencies.Diff diff = lockedDependencies.compareWith(projectDependencies(), projectVersion(), useMyVersionForFilter);
    if (diff.equals()) {
      getLog().info("Actual dependencies matches locked dependencies");
    } else {
      diff.logTo(getLog());
      throw new MojoExecutionException("Dependencies differ");
    }
  }

  private LockedDependencies getLockedDependencies(DependenciesLockFile lockFile) {
    switch (format()) {
      case json:
        DependenciesLockFileJson lockFileJson = DependenciesLockFileJson.from(lockFile);
        return lockFileJson.read(getLog());
      case pom:
        return PomIO.readPom(lockFile, getLog());
      default:
        throw new RuntimeException("This should not happen!");
    }
  }

}
