package se.vandmo.dependencylock.maven.mojos;

import static org.apache.maven.plugins.annotations.LifecyclePhase.VALIDATE;
import static org.apache.maven.plugins.annotations.ResolutionScope.TEST;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import se.vandmo.dependencylock.maven.*;
import se.vandmo.dependencylock.maven.mojos.model.Profile;
import se.vandmo.dependencylock.maven.services.ProfileHandler;

@Mojo(
    name = "check",
    defaultPhase = VALIDATE,
    requiresDependencyResolution = TEST,
    threadSafe = true)
public final class CheckMojo extends AbstractDependencyLockMojo {

  @Parameter(property = "dependencyLock.skipCheck")
  private Boolean skip = false;

  private final ProfileHandler profileHandler;

  @Inject
  public CheckMojo(ProfileHandler profileHandler) {
    this.profileHandler = profileHandler;
  }

  @Override
  public void execute() throws MojoExecutionException {
    try {
      doExecute();
    } catch (MojoExecutionRuntimeException e) {
      throw new MojoExecutionException(e.getMessage(), e);
    }
  }

  Dependencies projectDependencies() {
    return Dependencies.fromMavenArtifacts(mavenProject().getArtifacts());
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
    Set<String> activeProfiles =
        profileHandler
            .filterActiveProfiles(
                getDependenciesProfiles(), mavenProject(), mavenSession().getRequest())
            .map(Profile::getId)
            .collect(Collectors.toSet());
    Filters filters = filters();
    Log log = getLog();
    log.info("Checking dependencies");
    final DiffReport dependenciesDiff =
        LockedDependencies.from(
                Dependencies.merge(
                    Stream.concat(
                        Stream.of(lockedProject.dependencies.getDefaultEntities()),
                        lockedProject
                            .dependencies
                            .profileEntries()
                            .filter(p -> activeProfiles.contains(p.getKey()))
                            .map(Map.Entry::getValue))),
                getLog())
            .compareWith(projectDependencies(), filters)
            .getReport();
    Optional<DiffReport> parentsDiff =
        lockedProject.parents.map(
            lockedParents -> {
              log.info("Checking parents");
              return LockedParents.from(Parents.from(mavenProject()), getLog())
                  .compareWith(lockedParents, filters);
            });
    Optional<DiffReport> pluginsDiff =
        lockedProject.plugins.map(
            lockedPlugins -> {
              log.info("Checking plugins");
              return LockedPlugins.from(projectPlugins(), getLog())
                  .compareWith(lockedPlugins, filters);
            });
    Optional<DiffReport> extensionsDiff =
        lockedProject.extensions.map(
            lockedExtensions -> {
              log.info("Checking extensions");
              return LockedExtensions.from(projectExtensions(), getLog())
                  .compareWith(lockedExtensions, filters);
            });
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
