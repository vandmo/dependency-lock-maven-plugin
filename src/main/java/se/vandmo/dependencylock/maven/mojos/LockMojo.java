package se.vandmo.dependencylock.maven.mojos;

import static java.util.Locale.ROOT;
import static java.util.stream.Collectors.toList;
import static org.apache.maven.plugins.annotations.ResolutionScope.TEST;

import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import se.vandmo.dependencylock.maven.Artifact;
import se.vandmo.dependencylock.maven.Artifacts;
import se.vandmo.dependencylock.maven.DependencySetConfiguration;
import se.vandmo.dependencylock.maven.Filters;
import se.vandmo.dependencylock.maven.Integrity;
import se.vandmo.dependencylock.maven.LockFileAccessor;
import se.vandmo.dependencylock.maven.LockedDependencies;
import se.vandmo.dependencylock.maven.json.DependenciesLockFileJson;
import se.vandmo.dependencylock.maven.pom.DependenciesLockFilePom;

@Mojo(name = "lock", requiresDependencyResolution = TEST, threadSafe = true)
public final class LockMojo extends AbstractDependencyLockMojo {

  @Parameter(property = "dependencyLock.markIgnoredAsIgnored")
  private boolean markIgnoredAsIgnored = false;

  @Parameter(property = "dependencyLock.skipLock")
  private Boolean skip = false;

  @Override
  public void execute() {
    if (skip) {
      getLog().info("Skipping lock");
      return;
    }
    LockFileAccessor lockFile = lockFile();
    getLog().info(String.format(ROOT, "Creating %s", lockFile.filename()));
    switch (format()) {
      case json:
        DependenciesLockFileJson lockFileJson = DependenciesLockFileJson.from(lockFile, getLog());
        LockedDependencies lockedDependencies =
            LockedDependencies.from(filteredProjectDependencies(), getLog());
        lockFileJson.write(lockedDependencies);
        break;
      case pom:
        DependenciesLockFilePom lockFilePom =
            DependenciesLockFilePom.from(lockFile, pomMinimums(), getLog());
        lockFilePom.write(filteredProjectDependencies());
        break;
      default:
        throw new RuntimeException("This should not happen!");
    }
  }

  private Artifacts filteredProjectDependencies() {
    Artifacts projectDependencies = projectDependencies();
    if (!markIgnoredAsIgnored) {
      return projectDependencies;
    }
    getLog().info("Marking ignored version and integrity as ignored in lock file");
    Filters filters = filters();
    return Artifacts.fromArtifacts(
        projectDependencies().artifacts.stream()
            .map(artifact -> modify(artifact, filters))
            .collect(toList()));
  }

  private static Artifact modify(Artifact artifact, Filters filters) {
    if (filters
        .versionConfiguration(artifact)
        .type
        .equals(DependencySetConfiguration.Version.ignore)) {
      artifact = artifact.withVersion("ignored");
    }
    if (filters
        .integrityConfiguration(artifact)
        .equals(DependencySetConfiguration.Integrity.ignore)) {
      artifact = artifact.withIntegrity(Integrity.Ignored());
    }
    return artifact;
  }
}
