package se.vandmo.dependencylock.maven.mojos;

import static org.apache.maven.plugins.annotations.LifecyclePhase.VALIDATE;
import static org.apache.maven.plugins.annotations.ResolutionScope.TEST;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import se.vandmo.dependencylock.maven.DependenciesLockFileAccessor;
import se.vandmo.dependencylock.maven.LockedDependencies;

@Mojo(
    name = "check",
    defaultPhase = VALIDATE,
    requiresDependencyResolution = TEST,
    threadSafe = true)
public final class CheckMojo extends AbstractDependencyLockMojo {

  @Parameter(property = "dependencyLock.skipCheck")
  private Boolean skip = false;

  @Override
  public void execute() throws MojoExecutionException {
    if (skip) {
      getLog().info("Skipping check");
      return;
    }

    DependenciesLockFileAccessor lockFile = lockFile();
    if (!lockFile.exists()) {
      throw new MojoExecutionException(
          "No lock file found, create one by running 'mvn"
              + " se.vandmo:dependency-lock-maven-plugin:lock'");
    }
    LockedDependencies lockedDependencies =
        format().dependenciesLockFile_from(lockFile, pomMinimums(), getLog()).read();
    LockedDependencies.Diff diff = lockedDependencies.compareWith(projectDependencies(), filters());
    if (diff.equals()) {
      getLog().info("Actual dependencies matches locked dependencies");
    } else {
      diff.logTo(getLog());
      throw new MojoExecutionException("Dependencies differ");
    }
  }
}
