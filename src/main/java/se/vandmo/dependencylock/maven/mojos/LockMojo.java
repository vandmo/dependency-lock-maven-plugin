package se.vandmo.dependencylock.maven.mojos;

import static java.util.Locale.ROOT;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
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
import org.apache.maven.project.ProjectDependenciesResolver;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.graph.DependencyNode;
import se.vandmo.dependencylock.maven.*;
import se.vandmo.dependencylock.maven.json.DependenciesLockFileJson;
import se.vandmo.dependencylock.maven.json.LockfileJson;
import se.vandmo.dependencylock.maven.pom.DependenciesLockFilePom;
import se.vandmo.dependencylock.maven.pom.LockFilePom;

@Mojo(name = "lock", threadSafe = true)
public final class LockMojo extends AbstractDependencyLockMojo {

  /**
   * If true, entries which are ignored according to the filters parameter will have their integrity
   * flagged as ignored.
   */
  @Parameter(property = "dependencyLock.markIgnoredAsIgnored")
  private boolean markIgnoredAsIgnored = false;

  private final ProjectDependenciesResolver projectDependenciesResolver;

  @Parameter(property = "dependencyLock.skipLock")
  private Boolean skip = false;

  @Parameter(property = "dependencyLock.lockBuild")
  private boolean lockBuild;

  @Inject
  public LockMojo(ProjectDependenciesResolver projectDependenciesResolver) {
    this.projectDependenciesResolver = projectDependenciesResolver;
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
    Project result;
    if (isLockBuild()) {
      result =
          Project.from(
              projectDependencies(),
              Parents.from(mavenProject()),
              projectPlugins(),
              projectExtensions());
    } else {
      result = Project.from(projectDependencies());
    }
    if (!markIgnoredAsIgnored) {
      return result;
    }
    getLog().info("Marking ignored version and integrity as ignored in lock file");
    return FilterUtils.apply(filters(), result);
  }

  private void dumpJsonLockfile(LockFileAccessor lockFile) throws MojoExecutionException {
    if (isLockBuild()) {
      LockfileJson lockFileJson = LockfileJson.from(lockFile);
      LockedProject lockedProject = LockedProject.from(project());
      lockFileJson.write(lockedProject);
    } else {
      LockedDependencies lockedDependencies =
          LockedDependencies.from(filteredProjectDependencies(), getLog());
      DependenciesLockFileJson lockFileJson = DependenciesLockFileJson.from(lockFile);
      lockFileJson.write(lockedDependencies);
    }
  }

  private Dependencies filteredProjectDependencies() throws MojoExecutionException {
    Dependencies projectDependencies = projectDependencies();
    if (!markIgnoredAsIgnored) {
      return projectDependencies;
    }
    getLog().info("Marking ignored version and integrity as ignored in lock file");
    Filters filters = filters();
    return FilterUtils.apply(filters, projectDependencies);
  }

  Dependencies projectDependencies() throws MojoExecutionException {
    Filters filters = filters();
    DefaultDependencyResolutionRequest request = new DefaultDependencyResolutionRequest();
    request.setMavenProject(mavenProject());
    request.setRepositorySession(mavenSession().getRepositorySession());
    Collection<se.vandmo.dependencylock.maven.Dependency> ignoredDependencies = new ArrayList<>();
    request.setResolutionFilter(
        new DependencyFilter() {
          @Override
          public boolean accept(DependencyNode dependencyNode, List<DependencyNode> list) {
            final Dependency dependency = dependencyNode.getDependency();
            if (null == dependency) { // if it's the root node, go for it
              return true;
            }
            final Artifact artifact = RepositoryUtils.toArtifact(dependency.getArtifact());
            if (DependencySetConfiguration.Integrity.ignore.equals(
                filters.integrityConfiguration(artifact))) {
              ignoredDependencies.add(
                  se.vandmo.dependencylock.maven.Dependency.builder()
                      .artifactIdentifier(ArtifactIdentifier.from(artifact))
                      .version(artifact.getVersion())
                      .integrity(Integrity.Ignored())
                      .scope(dependency.getScope())
                      .optional(dependency.isOptional())
                      .build());
              return false;
            }
            return true;
          }
        });
    final DependencyResolutionResult resolutionResult;
    try {
      resolutionResult = projectDependenciesResolver.resolve(request);
    } catch (DependencyResolutionException e) {
      throw new MojoExecutionException(
          "Failed resolving dependencies due to an unexpected internal error: " + e, e);
    }

    return Dependencies.fromDependencies(
        Stream.concat(
                ignoredDependencies.stream(),
                resolutionResult.getDependencies().stream()
                    .map(LockMojo::mapDependency)
                    .map(se.vandmo.dependencylock.maven.Dependency::from))
            .collect(Collectors.toList()));
  }

  private static Artifact mapDependency(Dependency dependency) {
    final Artifact resultingArtifact = RepositoryUtils.toArtifact(dependency.getArtifact());
    resultingArtifact.setScope(dependency.getScope());
    resultingArtifact.setOptional(dependency.isOptional());
    return resultingArtifact;
  }

}
