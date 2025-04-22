package se.vandmo.dependencylock.maven.mojos;

import static java.util.Locale.ROOT;
import static java.util.stream.Collectors.toList;
import static org.apache.maven.plugins.annotations.ResolutionScope.TEST;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import se.vandmo.dependencylock.maven.Dependencies;
import se.vandmo.dependencylock.maven.Dependency;
import se.vandmo.dependencylock.maven.DependencySetConfiguration;
import se.vandmo.dependencylock.maven.Extension;
import se.vandmo.dependencylock.maven.Extensions;
import se.vandmo.dependencylock.maven.Filters;
import se.vandmo.dependencylock.maven.Integrity;
import se.vandmo.dependencylock.maven.LockFileAccessor;
import se.vandmo.dependencylock.maven.LockedDependencies;
import se.vandmo.dependencylock.maven.LockedProject;
import se.vandmo.dependencylock.maven.Plugin;
import se.vandmo.dependencylock.maven.Plugins;
import se.vandmo.dependencylock.maven.Project;
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
    if (alsoLockBuild()) {
      LockFilePom lockFileJson = LockFilePom.from(lockFile, pomMinimums(), getLog());
      LockedProject lockedProject = LockedProject.from(project(), getLog());
      lockFileJson.write(lockedProject);
    } else {
      DependenciesLockFilePom lockFilePom =
          DependenciesLockFilePom.from(lockFile, pomMinimums(), getLog());
      lockFilePom.write(filteredProjectDependencies());
    }
  }

  @Override
  Project project() throws MojoExecutionException {
    return Project.from(
        filteredProjectPlugins(), filteredProjectDependencies(), filteredProjectExtensions());
  }

  private void dumpJsonLockfile(LockFileAccessor lockFile) throws MojoExecutionException {
    if (alsoLockBuild()) {
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

  private static Dependency modify(Dependency dependency, Filters filters) {
    if (filters
        .versionConfiguration(dependency)
        .type
        .equals(DependencySetConfiguration.Version.ignore)) {
      dependency = dependency.withVersion("ignored");
    }
    if (filters
        .integrityConfiguration(dependency)
        .equals(DependencySetConfiguration.Integrity.ignore)) {
      dependency = dependency.withIntegrity(Integrity.Ignored());
    }
    return dependency;
  }

  private Plugin modify(Plugin plugin, Filters filters) {
    Plugin result = plugin;
    if (filters
        .versionConfiguration(plugin)
        .type
        .equals(DependencySetConfiguration.Version.ignore)) {
      result = result.withVersion("ignored");
    }
    if (filters
        .integrityConfiguration(plugin)
        .equals(DependencySetConfiguration.Integrity.ignore)) {
      result = result.withIntegrity(Integrity.Ignored());
    }
    // TODO modify artifacts
    return result;
  }

  private static Extension modify(Extension plugin, Filters filters) {
    Extension result = plugin;
    if (filters
        .versionConfiguration(plugin)
        .type
        .equals(DependencySetConfiguration.Version.ignore)) {
      result = result.withVersion("ignored");
    }
    if (filters
        .integrityConfiguration(result)
        .equals(DependencySetConfiguration.Integrity.ignore)) {
      result = result.withIntegrity(Integrity.Ignored());
    }
    return result;
  }
}
