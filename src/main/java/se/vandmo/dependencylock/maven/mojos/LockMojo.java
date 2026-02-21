package se.vandmo.dependencylock.maven.mojos;

import static java.util.Locale.ROOT;
import static java.util.stream.Collectors.toList;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;
import org.apache.maven.RepositoryUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.DefaultDependencyResolutionRequest;
import org.apache.maven.project.DependencyResolutionException;
import org.apache.maven.project.DependencyResolutionResult;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectDependenciesResolver;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.graph.Dependency;
import se.vandmo.dependencylock.maven.*;
import se.vandmo.dependencylock.maven.json.DependenciesLockFileJson;
import se.vandmo.dependencylock.maven.json.LockfileJson;
import se.vandmo.dependencylock.maven.mojos.model.Profile;
import se.vandmo.dependencylock.maven.pom.DependenciesLockFilePom;
import se.vandmo.dependencylock.maven.pom.LockFilePom;
import se.vandmo.dependencylock.maven.services.ProfileHandler;

@Mojo(name = "lock", threadSafe = true)
public final class LockMojo extends AbstractDependencyLockMojo {

  @Parameter(property = "dependencyLock.markIgnoredAsIgnored")
  private boolean markIgnoredAsIgnored = false;

  private final ProjectDependenciesResolver projectDependenciesResolver;

  private final ProfileHandler profileHandler;

  @Parameter(property = "dependencyLock.skipLock")
  private Boolean skip = false;

  @Parameter(property = "dependencyLock.lockBuild")
  private boolean lockBuild;

  @Inject
  public LockMojo(
      ProjectDependenciesResolver projectDependenciesResolver, ProfileHandler profileHandler) {
    this.projectDependenciesResolver = projectDependenciesResolver;
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
        lockFileJson.write(filteredProjectDependencies().getDefaultEntities());
      } else {
        lockFileJson.write(filteredProjectDependencies());
      }
    }
  }

  private Profiled<se.vandmo.dependencylock.maven.Dependency, Dependencies>
      filteredProjectDependencies() throws MojoExecutionException {
    Profiled<se.vandmo.dependencylock.maven.Dependency, Dependencies> projectDependencies =
        projectDependencies();
    if (!markIgnoredAsIgnored) {
      return projectDependencies;
    }
    getLog().info("Marking ignored version and integrity as ignored in lock file");
    Filters filters = filters();
    return new Profiled<>(
        applyFiltersToDependencies(projectDependencies.getDefaultEntities(), filters),
        projectDependencies
            .profileEntries()
            .collect(
                Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> applyFiltersToDependencies(entry.getValue(), filters))));
  }

  private static Dependencies applyFiltersToDependencies(
      Dependencies projectDependencies, Filters filters) {
    return Dependencies.fromDependencies(
        projectDependencies.stream()
            .map(dependency -> modify(dependency, filters))
            .collect(toList()));
  }

  private Profiled<se.vandmo.dependencylock.maven.Dependency, Dependencies> projectDependencies()
      throws MojoExecutionException {
    final Filters filters = filters();
    Map<String, Function<RepositorySystemSession, RepositorySystemSession>> profilingSessions =
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
    final MavenProject project = mavenProject();
    final RepositorySystemSession repositorySession = mavenSession().getRepositorySession();
    final Set<se.vandmo.dependencylock.maven.Dependency> currentProjectDependencies =
        computeDependencies(project, repositorySession, filters);
    if (getLog().isDebugEnabled()) {
      getLog().debug("Current dependencies: " + currentProjectDependencies);
    }
    Map<String, Set<se.vandmo.dependencylock.maven.Dependency>> byProfileId = new HashMap<>();
    for (Map.Entry<String, Function<RepositorySystemSession, RepositorySystemSession>> entry :
        profilingSessions.entrySet()) {
      final Set<se.vandmo.dependencylock.maven.Dependency> profileDependencies =
          computeDependencies(project, entry.getValue().apply(repositorySession), filters);
      final String profileId = entry.getKey();
      if (getLog().isDebugEnabled()) {
        getLog().debug("Profile " + profileId + " dependencies: " + profileDependencies);
      }
      byProfileId.put(profileId, profileDependencies);
    }
    final HashSet<se.vandmo.dependencylock.maven.Dependency> sharedDependencies =
        new HashSet<>(currentProjectDependencies);
    byProfileId.values().forEach(sharedDependencies::retainAll);
    if (getLog().isDebugEnabled()) {
      getLog().debug("Shared dependencies: " + sharedDependencies);
    }
    // clean up profile dependencies to remove shared dependencies
    for (String profileId : profilingSessions.keySet()) {
      Set<se.vandmo.dependencylock.maven.Dependency> profiledDependencies =
          byProfileId.get(profileId);
      profiledDependencies.removeAll(sharedDependencies);
    }

    // ensure "current project dependencies" no longer include shared dependencies
    currentProjectDependencies.removeAll(sharedDependencies);
    for (Profile enabledProfile : actuallyEnabledProfiles) {
      byProfileId.put(enabledProfile.getId(), currentProjectDependencies);
    }

    return new Profiled<>(
        Dependencies.fromDependencies(sharedDependencies),
        byProfileId.entrySet().stream()
            .collect(
                Collectors.toMap(
                    Map.Entry::getKey, entry -> Dependencies.fromDependencies(entry.getValue()))));
  }

  private Set<se.vandmo.dependencylock.maven.Dependency> computeDependencies(
      MavenProject project, RepositorySystemSession repositorySession, Filters filters)
      throws MojoExecutionException {
    final DefaultDependencyResolutionRequest request = new DefaultDependencyResolutionRequest();
    request.setMavenProject(project);
    request.setRepositorySession(repositorySession);
    final IgnoreArtifactsWithIgnoredIntegrity resolutionFilter =
        new IgnoreArtifactsWithIgnoredIntegrity(filters);
    request.setResolutionFilter(resolutionFilter);
    final DependencyResolutionResult resolutionResult;
    try {
      resolutionResult = projectDependenciesResolver.resolve(request);
    } catch (DependencyResolutionException e) {
      throw new MojoExecutionException(
          "Failed resolving dependencies due to an unexpected internal error: " + e, e);
    }
    return extractDependenciesFromResult(resolutionFilter, resolutionResult);
  }

  private static Set<se.vandmo.dependencylock.maven.Dependency> extractDependenciesFromResult(
      IgnoreArtifactsWithIgnoredIntegrity resolutionFilter,
      DependencyResolutionResult resolutionResult) {
    return Stream.concat(
            resolutionFilter.collectIgnoredDependencies(),
            resolutionResult.getDependencies().stream()
                .map(LockMojo::mapDependency)
                .map(se.vandmo.dependencylock.maven.Dependency::from))
        .collect(Collectors.toSet());
  }

  private static Artifact mapDependency(Dependency dependency) {
    final Artifact resultingArtifact = RepositoryUtils.toArtifact(dependency.getArtifact());
    resultingArtifact.setScope(dependency.getScope());
    resultingArtifact.setOptional(dependency.isOptional());
    return resultingArtifact;
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
