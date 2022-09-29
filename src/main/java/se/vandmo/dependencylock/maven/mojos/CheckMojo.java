package se.vandmo.dependencylock.maven.mojos;

import static java.util.Arrays.asList;
import static org.apache.maven.plugins.annotations.LifecyclePhase.VALIDATE;
import static org.apache.maven.plugins.annotations.ResolutionScope.TEST;

import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.shared.artifact.filter.StrictPatternIncludesArtifactFilter;
import se.vandmo.dependencylock.maven.DependenciesLockFileAccessor;
import se.vandmo.dependencylock.maven.Filters;
import se.vandmo.dependencylock.maven.LockedDependencies;


@Mojo(
  name = "check",
  defaultPhase = VALIDATE,
  requiresDependencyResolution = TEST)
public final class CheckMojo extends AbstractDependencyLockMojo {

  @Parameter
  private String[] useMyVersionFor = new String[0];
  @Parameter
  private String[] ignore = new String[0];

  @Override
  public void execute() throws MojoExecutionException {
    updateDependencyIntegrityCheckingValue();
    DependenciesLockFileAccessor lockFile = lockFile();
    if (!lockFile.exists()) {
      throw new MojoExecutionException(
          "No lock file found, create one by running 'mvn se.vandmo:dependency-lock-maven-plugin:create-lock-file'");
    }
    ArtifactFilter useMyVersionForFilter = new StrictPatternIncludesArtifactFilter(asList(useMyVersionFor));
    ArtifactFilter ignoreFilter = new StrictPatternIncludesArtifactFilter(asList(ignore));
    LockedDependencies lockedDependencies = format()
        .dependenciesLockFile_from(lockFile, pomMinimums(), getLog())
        .read(dependencyIntegrityChecking());
    Filters filters = Filters.builder().useMyVersionForFilter(useMyVersionForFilter).ignoreFilter(ignoreFilter).build();
    LockedDependencies.Diff diff = lockedDependencies.compareWith(projectDependencies(), projectVersion(), filters);
    if (diff.equals()) {
      getLog().info("Actual dependencies matches locked dependencies");
    } else {
      diff.logTo(getLog());
      throw new MojoExecutionException("Dependencies differ");
    }
  }

}
