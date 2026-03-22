package se.vandmo.dependencylock.maven.services;

import java.util.Collection;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystemSession;
import se.vandmo.dependencylock.maven.Dependency;
import se.vandmo.dependencylock.maven.Filters;

/**
 * Implementations of this interface shall be used to resolve the locked dependencies entry for a
 * given project.
 */
public interface DependenciesHelper {

  /**
   * Resolves all the dependencies of the given project using the specified session with the
   * provided filters.
   *
   * @param repositorySystemSession the session to use for resolving dependencies
   * @param mavenProject the project whose dependencies should be returned
   * @param filters what should be used to configure filtering of dependencies resolution
   * @return the corresponding resolved dependencies
   * @throws MojoExecutionException if there was any error while trying to resolve the dependencies
   */
  Collection<Dependency> resolveDependencies(
      RepositorySystemSession repositorySystemSession, MavenProject mavenProject, Filters filters)
      throws MojoExecutionException;
}
