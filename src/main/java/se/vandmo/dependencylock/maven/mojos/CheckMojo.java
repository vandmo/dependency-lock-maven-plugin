package se.vandmo.dependencylock.maven.mojos;

import static org.apache.maven.plugins.annotations.LifecyclePhase.VALIDATE;
import static org.apache.maven.plugins.annotations.ResolutionScope.TEST;

import java.util.Optional;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import se.vandmo.dependencylock.maven.*;

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
    try {
      doExecute();
    } catch (MojoExecutionRuntimeException e) {
      throw new MojoExecutionException(e.getMessage(), e);
    }
  }

  public void doExecute() throws MojoExecutionException {
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
    LockedProject lockedProject = format().lockFile_from(lockFile, pomMinimums(), getLog()).read();
    Filters filters = filters();
    final DiffReport dependenciesDiff =
        LockedDependencies.from(lockedProject.dependencies, getLog())
            .compareWith(projectDependencies(), filters)
            .getReport();
    Optional<DiffReport> parentsDiff =
        lockedProject.parents.map(
            lockedParents ->
                LockedParents.from(Parents.from(mavenProject()), getLog())
                    .compareWith(lockedParents, filters));
    Optional<DiffReport> pluginsDiff =
        lockedProject.plugins.map(
            lockedPlugins ->
                LockedPlugins.from(projectPlugins(), getLog()).compareWith(lockedPlugins, filters));
    Optional<DiffReport> extensionsDiff =
        lockedProject.extensions.map(
            lockedExtensions ->
                LockedExtensions.from(projectExtensions(), getLog())
                    .compareWith(lockedExtensions, filters));
    LockedProject.Diff diff =
        new LockedProject.Diff(dependenciesDiff, parentsDiff, pluginsDiff, extensionsDiff);
    if (diff.equals()) {
      getLog().info("Actual project matches locked project");
    } else {
      diff.logTo(getLog());
      throw new MojoExecutionException("Actual project differ from locked project");
    }
  }
}
