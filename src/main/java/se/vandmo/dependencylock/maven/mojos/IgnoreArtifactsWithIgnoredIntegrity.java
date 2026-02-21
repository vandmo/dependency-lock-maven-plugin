package se.vandmo.dependencylock.maven.mojos;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import org.apache.maven.RepositoryUtils;
import org.apache.maven.artifact.Artifact;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.graph.DependencyNode;
import se.vandmo.dependencylock.maven.ArtifactIdentifier;
import se.vandmo.dependencylock.maven.Dependency;
import se.vandmo.dependencylock.maven.DependencySetConfiguration;
import se.vandmo.dependencylock.maven.Filters;
import se.vandmo.dependencylock.maven.Integrity;

/**
 * Instances of this class shall ensure dependencies which integrity check is not required are not
 * being resolved.
 */
final class IgnoreArtifactsWithIgnoredIntegrity implements DependencyFilter {
  private final Filters filters;
  private final Collection<Dependency> ignoredDependencies;

  public IgnoreArtifactsWithIgnoredIntegrity(Filters filters) {
    this.filters = Objects.requireNonNull(filters, "filters == null");
    this.ignoredDependencies = new ArrayList<>();
  }

  Stream<Dependency> collectIgnoredDependencies() {
    return this.ignoredDependencies.stream();
  }

  void reset() {
    this.ignoredDependencies.clear();
  }

  @Override
  public boolean accept(DependencyNode dependencyNode, List<DependencyNode> list) {
    final org.eclipse.aether.graph.Dependency dependency = dependencyNode.getDependency();
    if (null == dependency) { // if it's the root node, go for it
      return true;
    }
    final Artifact artifact = RepositoryUtils.toArtifact(dependency.getArtifact());
    if (DependencySetConfiguration.Integrity.ignore.equals(
        filters.integrityConfiguration(artifact))) {
      ignoredDependencies.add(
          Dependency.builder()
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
}
