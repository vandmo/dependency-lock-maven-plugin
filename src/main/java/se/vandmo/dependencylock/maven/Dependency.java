package se.vandmo.dependencylock.maven;

import static java.util.Objects.requireNonNull;

import java.util.Objects;

public final class Dependency implements Comparable<Dependency> {
  public final Artifact artifact;
  public final String scope;
  public final boolean optional;

  public static ArtifactIdentifierBuilderStage builder() {
    return new ArtifactIdentifierBuilderStage();
  }

  public static ScopeBuilderStage forArtifact(Artifact artifact) {
    return new ScopeBuilderStage(requireNonNull(artifact));
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

  public static final class ScopeBuilderStage {
    private final Artifact artifact;

    private ScopeBuilderStage(Artifact artifact) {
      this.artifact = requireNonNull(artifact);
    }

    public OptionalBuilderStage scope(String scope) {
      return new OptionalBuilderStage(this.artifact, requireNonNull(scope));
    }
  }

  public static final class OptionalBuilderStage {
    private final Artifact artifact;
    private final String scope;

    private OptionalBuilderStage(Artifact artifact, String scope) {
      this.artifact = requireNonNull(artifact);
      this.scope = scope;
    }

    public Dependency build() {
      return optional(false).build();
    }

    public FinalBuilderStage optional(boolean optional) {
      return new FinalBuilderStage(artifact, scope, optional);
    }
  }

  public static final class IntegrityBuilderStage {
    private final Artifact.IntegrityBuilderStage artifactBuilder;

    private IntegrityBuilderStage(Artifact.IntegrityBuilderStage artifactBuilder) {
      this.artifactBuilder = artifactBuilder;
    }

    public ScopeBuilderStage integrity(String integrity) {
      return new ScopeBuilderStage(artifactBuilder.integrity(integrity).build());
    }
  }

  public static final class FinalBuilderStage {
    private final Artifact artifact;
    private final String scope;
    private final boolean optional;

    private FinalBuilderStage(Artifact artifact, String scope, boolean optional) {
      this.artifact = artifact;
      this.scope = scope;
      this.optional = optional;
    }

    public Dependency build() {
      return new Dependency(artifact, scope, optional);
    }
  }

  public static Dependency from(org.apache.maven.artifact.Artifact artifact) {
    Integrity integrity =
        artifact.getFile().isDirectory()
            ? Integrity.Folder()
            : Integrity.Calculated(Checksum.calculateFor(artifact.getFile()));
    return new Dependency(
        Artifact.from(artifact).withIntegrity(integrity),
        artifact.getScope(),
        artifact.isOptional());
  }

  public org.apache.maven.artifact.Artifact toMavenArtifact() {
    return new MavenArtifact(this);
  }

  private Dependency(Artifact artifact, String scope, boolean optional) {
    super();
    this.artifact = requireNonNull(artifact);
    this.scope = requireNonNull(scope);
    this.optional = optional;
  }

  public Dependency withVersion(String version) {
    return new Dependency(this.artifact.withVersion(version), scope, optional);
  }

  public Dependency withIntegrity(Integrity integrity) {
    return new Dependency(this.artifact.withIntegrity(integrity), scope, optional);
  }

  @Override
  public int compareTo(Dependency other) {
    return toString().compareTo(other.toString());
  }

  @Override
  public String toString() {
    return toStringBuilder_withoutIntegrity()
        .append('@')
        .append(
            artifact
                .integrity
                .<String>matching()
                .Calculated((calculcated) -> calculcated.checksum)
                .Folder((folder) -> "<Folder>")
                .Ignored((ignored) -> "<Ignored>")
                .get())
        .toString();
  }

  public String toString_withoutIntegrity() {
    return toStringBuilder_withoutIntegrity().toString();
  }

  private StringBuilder toStringBuilder_withoutIntegrity() {
    return artifact
        .toStringBuilder_withoutIntegrity()
        .append(':')
        .append(scope)
        .append(":optional=")
        .append(optional);
  }

  @Override
  public int hashCode() {
    int hash = 7;
    hash = 17 * hash + Objects.hashCode(this.artifact);
    hash = 17 * hash + Objects.hashCode(this.scope);
    hash = 17 * hash + Objects.hashCode(this.optional);
    return hash;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final Dependency other = (Dependency) obj;
    if (!Objects.equals(this.artifact, other.artifact)) {
      return false;
    }
    if (!Objects.equals(this.scope, other.scope)) {
      return false;
    }
    if (!Objects.equals(this.optional, other.optional)) {
      return false;
    }
    return true;
  }

  public boolean equals_ignoreVersion(Dependency other) {
    if (this == other) {
      return true;
    }
    if (other == null) {
      return false;
    }
    if (getClass() != other.getClass()) {
      return false;
    }
    if (!this.artifact.equals_ignoreVersion(other.artifact)) {
      return false;
    }
    if (!Objects.equals(this.scope, other.scope)) {
      return false;
    }
    if (!Objects.equals(this.optional, other.optional)) {
      return false;
    }
    return true;
  }
}
