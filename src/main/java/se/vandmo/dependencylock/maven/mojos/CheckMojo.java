package se.vandmo.dependencylock.maven.mojos;

import static org.apache.maven.plugins.annotations.LifecyclePhase.VALIDATE;
import static org.apache.maven.plugins.annotations.ResolutionScope.TEST;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import se.vandmo.dependencylock.maven.LockFileAccessor;
import se.vandmo.dependencylock.maven.LockedDependencies;
import se.vandmo.dependencylock.maven.LockedProject;

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

    LockFileAccessor lockFile = lockFile();
    if (!lockFile.exists()) {
      throw new MojoExecutionException(
          "No lock file found, create one by running 'mvn"
              + " se.vandmo:dependency-lock-maven-plugin:lock'");
    }
    if (isLockBuild()) {
      LockedProject lockedProjectDependencies =
          format().lockFile_from(lockFile, pomMinimums(), getLog()).read();
      LockedProject.Diff diff = lockedProjectDependencies.compareWith(project(), filters());
      if (diff.equals()) {
        getLog()
            .info(
                "Actual dependencies, plugins and extensions matches locked dependencies, plugins"
                    + " and extensions");
      } else {
        diff.logTo(getLog());
        throw new MojoExecutionException("Dependencies / Plugins / Extensions differ");
      }
    } else {
      LockedDependencies lockedDependencies =
          format().dependenciesLockFile_from(lockFile, pomMinimums(), getLog()).read();
      LockedDependencies.Diff diff =
          lockedDependencies.compareWith(projectDependencies(), filters());
      if (diff.equals()) {
        getLog().info("Actual dependencies matches locked dependencies");
      } else {
        diff.logTo(getLog());
        throw new MojoExecutionException("Dependencies differ");
      }
    }
  }
}
