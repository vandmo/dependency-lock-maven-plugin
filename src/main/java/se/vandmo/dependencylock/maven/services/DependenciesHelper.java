package se.vandmo.dependencylock.maven.services;

import java.util.Collection;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystemSession;
import se.vandmo.dependencylock.maven.Dependency;
import se.vandmo.dependencylock.maven.Filters;

/** */
public interface DependenciesHelper {

  Collection<Dependency> resolveDependencies(
      RepositorySystemSession repositorySystemSession, MavenProject mavenProject, Filters filters)
      throws MojoExecutionException;
}
