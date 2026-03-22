package se.vandmo.dependencylock.maven.mojos;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;
import static java.util.Locale.ROOT;
import static java.util.stream.Collectors.toList;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.inject.Inject;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystemSession;
import se.vandmo.dependencylock.maven.*;
import se.vandmo.dependencylock.maven.json.DependenciesLockFileJson;
import se.vandmo.dependencylock.maven.json.LockfileJson;
import se.vandmo.dependencylock.maven.mojos.model.Profile;
import se.vandmo.dependencylock.maven.pom.DependenciesLockFilePom;
import se.vandmo.dependencylock.maven.pom.LockFilePom;
import se.vandmo.dependencylock.maven.services.DependenciesHelper;
import se.vandmo.dependencylock.maven.services.ProfileHandler;

@Mojo(name = "lock", threadSafe = true)
public final class LockMojo extends AbstractDependencyLockMojo {

  @Parameter(property = "dependencyLock.markIgnoredAsIgnored")
  private boolean markIgnoredAsIgnored = false;

  private final DependenciesHelper dependenciesHelper;

  private final ProfileHandler profileHandler;

  @Parameter(property = "dependencyLock.skipLock")
  private Boolean skip = false;

  @Parameter(property = "dependencyLock.lockBuild")
  private boolean lockBuild;

  @Parameter private Profile[] profiles;

  @Inject
  public LockMojo(DependenciesHelper dependenciesHelper, ProfileHandler profileHandler) {
    this.dependenciesHelper = dependenciesHelper;
    this.profileHandler = profileHandler;
  }

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

  private Collection<Profile> getDependenciesProfiles() throws MojoExecutionException {
    final Profile[] configuredProfiles = this.profiles;
    if (null == configuredProfiles) {
      return null;
    }
    final List<Profile> result = unmodifiableList(asList(configuredProfiles));
    if (result.stream().map(Profile::getId).distinct().count() != result.size()) {
      throw new MojoExecutionException(
          "Duplicate profile IDs found: "
              + result.stream().map(Profile::getId).collect(Collectors.joining(", ")));
    }
    return result;
  }

  private void dumpPomLockfile(LockFileAccessor lockFile) throws MojoExecutionException {
    if (isLockBuild()) {
      LockFilePom lockFiles = LockFilePom.from(lockFile, pomMinimums());
      LockedProject lockedProject = LockedProject.from(project());
      lockFiles.write(lockedProject);
    } else {
      DependenciesLockFilePom lockFilePom = DependenciesLockFilePom.from(lockFile, pomMinimums());
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
      LockfileJson lockFileJson = LockfileJson.from(lockFile);
      LockedProject lockedProject = LockedProject.from(project());
      lockFileJson.write(lockedProject);
    } else {
      Collection<Profile> profiles = getDependenciesProfiles();
      DependenciesLockFileJson lockFileJson = DependenciesLockFileJson.from(lockFile);
      if (null == profiles || profiles.isEmpty()) {
        lockFileJson.write(filteredProjectDependencies().getSharedDependencies());
      } else {
        lockFileJson.write(filteredProjectDependencies());
      }
    }
  }

  private ProfiledDependencies filteredProjectDependencies() throws MojoExecutionException {
    ProfiledDependencies projectDependencies = projectDependencies();
    if (!markIgnoredAsIgnored) {
      return projectDependencies;
    }
    getLog().info("Marking ignored version and integrity as ignored in lock file");
    Filters filters = filters();
    return new ProfiledDependencies(
        applyFiltersToDependencies(projectDependencies.getSharedDependencies(), filters),
        projectDependencies
            .profileEntries()
            .map(
                entry ->
                    new ProfileEntry(
                        entry.getProfile(),
                        applyFiltersToDependencies(entry.getDependencies(), filters)))
            .collect(Collectors.toList()));
  }

  private static Dependencies applyFiltersToDependencies(
      Dependencies projectDependencies, Filters filters) {
    return Dependencies.fromDependencies(
        projectDependencies.stream()
            .map(dependency -> modify(dependency, filters))
            .collect(toList()));
  }

  private ProfiledDependencies projectDependencies() throws MojoExecutionException {
    final Filters filters = filters();
    final MavenProject project = mavenProject();
    final RepositorySystemSession repositorySession = mavenSession().getRepositorySession();
    final Collection<se.vandmo.dependencylock.maven.Dependency> currentProjectDependencies =
        this.dependenciesHelper.resolveDependencies(repositorySession, project, filters);
    Map<Profile, Function<RepositorySystemSession, RepositorySystemSession>> profilingSessions =
        new HashMap<>();
    final Collection<Profile> actuallyEnabledProfiles =
        profileHandler.computeEnabledProfiles(
            mavenSession(), getDependenciesProfiles(), profilingSessions);
    if (actuallyEnabledProfiles.size() > 1) {
      getLog()
          .warn(
              "More than one profile is currently enabled. Locked artifacts may actually be"
                  + " inconsistent: "
                  + actuallyEnabledProfiles.stream().map(Profile::getId).collect(toList()));
    } else if (getLog().isDebugEnabled()) {
      getLog()
          .debug(
              "Enabled profiles: "
                  + actuallyEnabledProfiles.stream().map(Profile::getId).collect(toList()));
    }
    if (getLog().isDebugEnabled()) {
      getLog().debug("Current dependencies: " + currentProjectDependencies);
    }
    Map<Profile, Collection<se.vandmo.dependencylock.maven.Dependency>> byProfile = new HashMap<>();
    for (Map.Entry<Profile, Function<RepositorySystemSession, RepositorySystemSession>> entry :
        profilingSessions.entrySet()) {
      final Collection<se.vandmo.dependencylock.maven.Dependency> profileDependencies =
          this.dependenciesHelper.resolveDependencies(
              entry.getValue().apply(repositorySession), project, filters);
      final Profile profile = entry.getKey();
      if (getLog().isDebugEnabled()) {
        getLog().debug("Profile " + profile.getId() + " dependencies: " + profileDependencies);
      }
      byProfile.put(profile, profileDependencies);
    }
    final HashSet<se.vandmo.dependencylock.maven.Dependency> sharedDependencies =
        new HashSet<>(currentProjectDependencies);
    byProfile.values().forEach(sharedDependencies::retainAll);
    if (getLog().isDebugEnabled()) {
      getLog().debug("Shared dependencies: " + sharedDependencies);
    }
    // clean up profile dependencies to remove shared dependencies
    for (Profile profile : profilingSessions.keySet()) {
      Collection<se.vandmo.dependencylock.maven.Dependency> profiledDependencies =
          byProfile.get(profile);
      profiledDependencies.removeAll(sharedDependencies);
    }

    // ensure "current project dependencies" no longer include shared dependencies
    currentProjectDependencies.removeAll(sharedDependencies);
    for (Profile enabledProfile : actuallyEnabledProfiles) {
      byProfile.put(enabledProfile, currentProjectDependencies);
    }

    return new ProfiledDependencies(
        Dependencies.fromDependencies(sharedDependencies),
        byProfile.entrySet().stream()
            .sorted(Comparator.comparing(entry -> entry.getKey().getId()))
            .map(
                entry ->
                    new ProfileEntry(
                        entry.getKey(), Dependencies.fromDependencies(entry.getValue())))
            .collect(toList()));
  }

  private Plugins filteredProjectPlugins() {
    Plugins projectPlugins = projectPlugins();
    if (!markIgnoredAsIgnored) {
      return projectPlugins;
    }
    getLog().info("Marking ignored version and integrity as ignored in lock file");
    Filters filters = filters();
    return Plugins.from(
        projectPlugins.stream().map(plugin -> modify(plugin, filters)).collect(toList()));
  }

  private Extensions filteredProjectExtensions() {
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
