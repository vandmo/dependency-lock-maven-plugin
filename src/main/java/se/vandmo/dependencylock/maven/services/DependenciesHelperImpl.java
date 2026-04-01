package se.vandmo.dependencylock.maven.services;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.apache.maven.RepositoryUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.DefaultDependencyResolutionRequest;
import org.apache.maven.project.DependencyResolutionException;
import org.apache.maven.project.DependencyResolutionResult;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectDependenciesResolver;
import org.eclipse.aether.RepositorySystemSession;
import se.vandmo.dependencylock.maven.Dependency;
import se.vandmo.dependencylock.maven.Filters;

/** Implementation of {@link DependenciesHelper} injected via Guice framework. */
@Named
@Singleton
public class DependenciesHelperImpl implements DependenciesHelper {
  private final ProjectDependenciesResolver projectDependenciesResolver;

  @Inject
  public DependenciesHelperImpl(ProjectDependenciesResolver projectDependenciesResolver) {
    this.projectDependenciesResolver = projectDependenciesResolver;
  }

  @Override
  public Collection<Dependency> resolveDependencies(
      RepositorySystemSession repositorySystemSession, MavenProject mavenProject, Filters filters)
      throws MojoExecutionException {
    final DefaultDependencyResolutionRequest request = new DefaultDependencyResolutionRequest();
    request.setMavenProject(mavenProject);
    request.setRepositorySession(repositorySystemSession);
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

  private static Set<Dependency> extractDependenciesFromResult(
      se.vandmo.dependencylock.maven.services.IgnoreArtifactsWithIgnoredIntegrity resolutionFilter,
      DependencyResolutionResult resolutionResult) {
    return Stream.concat(
            resolutionFilter.collectIgnoredDependencies(),
            resolutionResult.getDependencies().stream()
                .map(DependenciesHelperImpl::mapDependency)
                .map(se.vandmo.dependencylock.maven.Dependency::from))
        .collect(Collectors.toSet());
  }

  private static Artifact mapDependency(org.eclipse.aether.graph.Dependency dependency) {
    final Artifact resultingArtifact = RepositoryUtils.toArtifact(dependency.getArtifact());
    resultingArtifact.setScope(dependency.getScope());
    resultingArtifact.setOptional(dependency.isOptional());
    return resultingArtifact;
  }
}
