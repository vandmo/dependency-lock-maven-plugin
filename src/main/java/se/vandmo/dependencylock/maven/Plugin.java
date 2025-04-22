package se.vandmo.dependencylock.maven;

import static java.util.Objects.requireNonNull;

import java.util.stream.Collectors;
import org.apache.maven.plugin.descriptor.PluginDescriptor;

public final class Plugin extends LockableEntityWithArtifact {
  public final Artifacts dependencies;

  public static PluginIdentifierBuilderStage builder() {
    return new PluginIdentifierBuilderStage();
  }

  public static ArtifactsBuilderStage forArtifact(Artifact dependency) {
    return new ArtifactsBuilderStage(dependency);
  }

  public static Plugin fromPluginDescriptor(PluginDescriptor descriptor) {
    final org.apache.maven.artifact.Artifact pluginArtifact = descriptor.getPluginArtifact();
    return new Plugin(
        Artifact.from(pluginArtifact),
        Artifacts.fromMavenArtifacts(
            descriptor.getArtifacts().stream()
                .filter(a -> !a.equals(pluginArtifact))
                .collect(Collectors.toList())));
  }

  public Plugin withIntegrity(Integrity integrity) {
    return new Plugin(artifact.withIntegrity(integrity), dependencies);
  }

  public Plugin withDependencies(Artifacts artifacts) {
    return new Plugin(artifact, artifacts);
  }

  public static final class PluginIdentifierBuilderStage {
    private PluginIdentifierBuilderStage() {}

    public VersionBuilderStage artifactIdentifier(ArtifactIdentifier artifactIdentifier) {
      return new VersionBuilderStage(Artifact.builder().artifactIdentifier(artifactIdentifier));
    }
  }

  public static final class VersionBuilderStage {
    private final Artifact.VersionBuilderStage versionBuilder;

    private VersionBuilderStage(Artifact.VersionBuilderStage versionBuilder) {
      this.versionBuilder = versionBuilder;
    }

    public IntegrityBuilderStage version(String version) {
      return new IntegrityBuilderStage(versionBuilder.version(version));
    }
  }

  public static final class IntegrityBuilderStage {
    private final Artifact.IntegrityBuilderStage artifactIntegrity;

    private IntegrityBuilderStage(Artifact.IntegrityBuilderStage artifactIntegrity) {
      this.artifactIntegrity = artifactIntegrity;
    }

    public ArtifactsBuilderStage integrity(String integrity) {
      return new ArtifactsBuilderStage(artifactIntegrity.integrity(integrity).build());
    }
  }

  public static final class ArtifactsBuilderStage {
    private final Artifact artifact;

    private ArtifactsBuilderStage(Artifact artifact) {
      this.artifact = artifact;
    }

    public FinalBuilderStage artifacts(Artifacts artifacts) {
      return new FinalBuilderStage(this.artifact, requireNonNull(artifacts));
    }
  }

  public static final class FinalBuilderStage {
    private final Artifact artifact;
    private final Artifacts dependencies;

    private FinalBuilderStage(Artifact artifact, Artifacts dependencies) {
      this.artifact = artifact;
      this.dependencies = dependencies;
    }

    public Plugin build() {
      return new Plugin(artifact, dependencies);
    }
  }

  private Plugin(Artifact artifact, Artifacts dependencies) {
    super(artifact);
    this.dependencies = requireNonNull(dependencies);
  }

  public Plugin withVersion(String version) {
    return new Plugin(artifact.withVersion(version), dependencies);
  }
}
