package se.vandmo.dependencylock.maven.mojos;

import static java.util.Locale.ROOT;
import static java.util.stream.Collectors.toList;
import static org.apache.maven.plugins.annotations.ResolutionScope.TEST;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import se.vandmo.dependencylock.maven.*;
import se.vandmo.dependencylock.maven.json.DependenciesLockFileJson;
import se.vandmo.dependencylock.maven.json.LockfileJson;
import se.vandmo.dependencylock.maven.pom.DependenciesLockFilePom;
import se.vandmo.dependencylock.maven.pom.LockFilePom;

@Mojo(name = "lock", requiresDependencyResolution = TEST, threadSafe = true)
public final class LockMojo extends AbstractDependencyLockMojo {

  @Parameter(property = "dependencyLock.markIgnoredAsIgnored")
  private boolean markIgnoredAsIgnored = false;

  @Parameter(property = "dependencyLock.skipLock")
  private Boolean skip = false;

  @Parameter(property = "dependencyLock.lockBuild")
  private boolean lockBuild;

  @Override
  public void execute() throws MojoExecutionException {
    if (skip) {
      getLog().info("Skipping lock");
      return;
    }
    LockFileAccessor lockFile = lockFile();
    getLog().info(String.format(ROOT, "Creating %s", lockFile.filename()));
    switch (format()) {
      case json:
        dumpJsonLockfile(lockFile);
        break;
      case pom:
        dumpPomLockfile(lockFile);
        break;
      default:
        throw new RuntimeException("This should not happen!");
    }
  }

  private void dumpPomLockfile(LockFileAccessor lockFile) throws MojoExecutionException {
    if (isLockBuild()) {
      LockFilePom lockFiles = LockFilePom.from(lockFile, pomMinimums(), getLog());
      LockedProject lockedProject = LockedProject.from(project(), getLog());
      lockFiles.write(lockedProject);
    } else {
      DependenciesLockFilePom lockFilePom =
          DependenciesLockFilePom.from(lockFile, pomMinimums(), getLog());
      lockFilePom.write(filteredProjectDependencies());
    }
  }

  boolean isLockBuild() {
    return lockBuild;
  }

  Project project() throws MojoExecutionException {
    if (isLockBuild()) {
      return Project.from(
          filteredProjectDependencies(),
          Parents.from(mavenProject()),
          filteredProjectPlugins(),
          filteredProjectExtensions());
    }
    return Project.from(filteredProjectDependencies());
  }

  private void dumpJsonLockfile(LockFileAccessor lockFile) throws MojoExecutionException {
    if (isLockBuild()) {
      LockfileJson lockFileJson = LockfileJson.from(lockFile, getLog());
      LockedProject lockedProject = LockedProject.from(project(), getLog());
      lockFileJson.write(lockedProject);
    } else {
      DependenciesLockFileJson lockFileJson = DependenciesLockFileJson.from(lockFile, getLog());
      LockedDependencies lockedDependencies =
          LockedDependencies.from(filteredProjectDependencies(), getLog());
      lockFileJson.write(lockedDependencies);
    }
  }

  private Dependencies filteredProjectDependencies() {
    Dependencies projectDependencies = projectDependencies();
    if (!markIgnoredAsIgnored) {
      return projectDependencies;
    }
    getLog().info("Marking ignored version and integrity as ignored in lock file");
    Filters filters = filters();
    return Dependencies.fromDependencies(
        projectDependencies().stream()
            .map(artifact -> modify(artifact, filters))
            .collect(toList()));
  }

  private Plugins filteredProjectPlugins() throws MojoExecutionException {
    Plugins projectPlugins = projectPlugins();
    if (!markIgnoredAsIgnored) {
      return projectPlugins;
    }
    getLog().info("Marking ignored version and integrity as ignored in lock file");
    Filters filters = filters();
    return Plugins.from(
        projectPlugins.stream().map(plugin -> modify(plugin, filters)).collect(toList()));
  }

  private Extensions filteredProjectExtensions() throws MojoExecutionException {
    Extensions projectExtensions = projectExtensions();
    if (!markIgnoredAsIgnored) {
      return projectExtensions;
    }
    getLog().info("Marking ignored version and integrity as ignored in lock file");
    Filters filters = filters();
    return Extensions.from(
        projectExtensions.stream().map(plugin -> modify(plugin, filters)).collect(toList()));
  }

  private static <T extends LockableEntity<T>> T modify(T lockableEntity, Filters filters) {
    T result = lockableEntity;
    result = ignoreVersionIfRelevant(result, filters);
    result = ignoreIntegrityIfRelevant(result, filters);
    return result;
  }

  private static <T extends LockableEntity<T>> T ignoreIntegrityIfRelevant(
      T lockableEntity, Filters filters) {
    if (filters
        .integrityConfiguration(lockableEntity)
        .equals(DependencySetConfiguration.Integrity.ignore)) {
      return lockableEntity.withIntegrity(Integrity.Ignored());
    }
    return lockableEntity;
  }

  private static <T extends LockableEntity<T>> T ignoreVersionIfRelevant(
      T lockableEntity, Filters filters) {
    if (filters
        .versionConfiguration(lockableEntity)
        .type
        .equals(DependencySetConfiguration.Version.ignore)) {
      return lockableEntity.withVersion("ignored");
    }
    return lockableEntity;
  }

  private Plugin modify(Plugin plugin, Filters filters) {
    Plugin result = plugin;
    result = ignoreVersionIfRelevant(result, filters);
    result = ignoreIntegrityIfRelevant(result, filters);
    result =
        result.withDependencies(
            Artifacts.fromArtifacts(
                result.dependencies.stream()
                    .map(artifact -> modify(artifact, filters))
                    .collect(toList())));
    return result;
  }
}
