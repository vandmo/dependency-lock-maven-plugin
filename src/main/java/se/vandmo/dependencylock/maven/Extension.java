package se.vandmo.dependencylock.maven;

import static java.util.Objects.requireNonNull;

import org.apache.maven.plugin.ExtensionRealmCache;

public final class Extension extends LockableEntityWithArtifact {
  public static ArtifactIdentifierBuilderStage builder() {
    return new ArtifactIdentifierBuilderStage();
  }

  public static Extension of(Artifact dependency) {
    return new Extension(dependency);
  }

  public static Extension fromMavenExtensionRealm(ExtensionRealmCache.CacheRecord extensionRealm) {
    return new Extension(Artifact.from(extensionRealm.getArtifacts().get(0)));
  }

  public Extension withIntegrity(Integrity integrity) {
    return new Extension(artifact.withIntegrity(integrity));
  }

  public static final class ArtifactIdentifierBuilderStage {
    private ArtifactIdentifierBuilderStage() {}

    public VersionBuilderStage artifactIdentifier(ArtifactIdentifier artifactIdentifier) {
      return new VersionBuilderStage(Artifact.builder().artifactIdentifier(artifactIdentifier));
    }
  }

  public static final class VersionBuilderStage {
    private final Artifact.VersionBuilderStage artifactBuilder;

    private VersionBuilderStage(Artifact.VersionBuilderStage artifactBuilder) {
      this.artifactBuilder = requireNonNull(artifactBuilder);
    }

    public IntegrityBuilderStage version(String version) {
      return new IntegrityBuilderStage(artifactBuilder.version(version));
    }
  }

  public static final class IntegrityBuilderStage {
    private final Artifact.IntegrityBuilderStage artifactBuilder;

    private IntegrityBuilderStage(Artifact.IntegrityBuilderStage artifactBuilder) {
      this.artifactBuilder = artifactBuilder;
    }

    public FinalBuilderStage integrity(String integrity) {
      return new FinalBuilderStage(artifactBuilder.integrity(integrity).build());
    }
  }

  public static final class FinalBuilderStage {
    private final Artifact artifact;

    private FinalBuilderStage(Artifact artifact) {
      this.artifact = artifact;
    }

    public Extension build() {
      return new Extension(artifact);
    }
  }

  private Extension(Artifact artifact) {
    super(artifact);
  }

  public Extension withVersion(String version) {
    return new Extension(artifact.withVersion(version));
  }
}
