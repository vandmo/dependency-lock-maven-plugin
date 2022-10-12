package se.vandmo.dependencylock.maven;

import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;

import java.util.Objects;
import java.util.Optional;
import org.apache.maven.model.Dependency;

public final class Artifact implements Comparable<Artifact> {

  public final ArtifactIdentifier identifier;
  public final String version;
  public final String scope;
  public final boolean optional;
  public final Optional<String> checksum;

  public static ArtifactIdentifierBuilderStage builder() {
    return new ArtifactIdentifierBuilderStage();
  }

  public static final class ArtifactIdentifierBuilderStage {
    private ArtifactIdentifierBuilderStage() {}

    public VersionBuilderStage artifactIdentifier(ArtifactIdentifier artifactIdentifier) {
      return new VersionBuilderStage(requireNonNull(artifactIdentifier));
    }
  }

  public static final class VersionBuilderStage {
    private final ArtifactIdentifier artifactIdentifier;

    private VersionBuilderStage(ArtifactIdentifier artifactIdentifier) {
      this.artifactIdentifier = artifactIdentifier;
    }

    public ScopeBuilderStage version(String version) {
      return new ScopeBuilderStage(artifactIdentifier, requireNonNull(version));
    }
  }

  public static final class ScopeBuilderStage {
    private final ArtifactIdentifier artifactIdentifier;
    private final String version;

    private ScopeBuilderStage(ArtifactIdentifier artifactIdentifier, String version) {
      this.artifactIdentifier = artifactIdentifier;
      this.version = version;
    }

    public OptionalBuilderStage scope(String scope) {
      return new OptionalBuilderStage(artifactIdentifier, version, requireNonNull(scope));
    }
  }

  public static final class OptionalBuilderStage {
    private final ArtifactIdentifier artifactIdentifier;
    private final String version;
    private final String scope;

    private OptionalBuilderStage(ArtifactIdentifier artifactIdentifier, String version, String scope) {
      this.artifactIdentifier = artifactIdentifier;
      this.version = version;
      this.scope = scope;
    }

    public IntegrityBuilderStage optional(boolean optional) {
      return new IntegrityBuilderStage(artifactIdentifier, version, scope, optional);
    }
    public FinalBuilderStage integrity(String integrity) {
      return new FinalBuilderStage(
          artifactIdentifier, version, scope, false, Optional.of(requireNonNull(integrity)));
    }
    public FinalBuilderStage integrity(Optional<String> integrity) {
      return new FinalBuilderStage(
          artifactIdentifier, version, scope, false, requireNonNull(integrity));
    }
  }

  public static final class IntegrityBuilderStage {
    private final ArtifactIdentifier artifactIdentifier;
    private final String version;
    private final String scope;
    private final boolean optional;

    private IntegrityBuilderStage(
        ArtifactIdentifier artifactIdentifier, String version, String scope, boolean optional) {
      this.artifactIdentifier = artifactIdentifier;
      this.version = version;
      this.scope = scope;
      this.optional = optional;
    }

    public FinalBuilderStage integrity(Optional<String> integrity) {
      return new FinalBuilderStage(
          artifactIdentifier, version, scope, optional, requireNonNull(integrity));
    }
  }

  public static final class FinalBuilderStage {
    private final ArtifactIdentifier artifactIdentifier;
    private final String version;
    private final String scope;
    private final boolean optional;
    private final Optional<String> checksum;

    private FinalBuilderStage(
        ArtifactIdentifier artifactIdentifier,
        String version,
        String scope,
        boolean optional,
        Optional<String> checksum) {
      this.artifactIdentifier = artifactIdentifier;
      this.version = version;
      this.scope = scope;
      this.optional = optional;
      this.checksum = checksum;
    }

    public Artifact build() {
      return new Artifact(artifactIdentifier, version, scope, optional, checksum);
    }
  }

  public static Artifact from(
      org.apache.maven.artifact.Artifact artifact, boolean enableIntegrityChecking) {
    return new Artifact(
        ArtifactIdentifier
            .builder()
            .groupId(artifact.getGroupId())
            .artifactId(artifact.getArtifactId())
            .classifier(ofNullable(artifact.getClassifier()))
            .type(ofNullable(artifact.getType()))
            .build(),
        artifact.getVersion(),
        artifact.getScope(),
        artifact.isOptional(),
        enableIntegrityChecking
            ? Optional.of(Checksum.calculateFor(artifact.getFile()))
            : Optional.empty());
  }

  public static Artifact from(Dependency dependency) {
    return new Artifact(
        ArtifactIdentifier
            .builder()
            .groupId(dependency.getGroupId())
            .artifactId(dependency.getArtifactId())
            .classifier(ofNullable(dependency.getClassifier()))
            .type(ofNullable(dependency.getType()))
            .build(),
        dependency.getVersion(),
        dependency.getScope(),
        dependency.isOptional(),
        Optional.empty());
  }

  public org.apache.maven.artifact.Artifact toMavenArtifact() {
    return new MavenArtifact(this);
  }

  Artifact(
      ArtifactIdentifier identifier,
      String version,
      String scope,
      boolean optional,
      Optional<String> checksum) {
    this.identifier = requireNonNull(identifier);
    this.version = requireNonNull(version);
    this.scope = requireNonNull(scope);
    this.optional = optional;
    this.checksum = checksum;
  }

  @Override
  public int compareTo(Artifact other) {
    return toString().compareTo(other.toString());
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(identifier.toString())
        .append(':')
        .append(version)
        .append(':')
        .append(scope)
        .append(":optional=")
        .append(optional)
        .append('@')
        .append(checksum.orElse("NO_CHECKSUM"));
    return sb.toString();
  }

  @Override
  public int hashCode() {
    int hash = 7;
    hash = 17 * hash + Objects.hashCode(this.identifier);
    hash = 17 * hash + Objects.hashCode(this.version);
    hash = 17 * hash + Objects.hashCode(this.scope);
    hash = 17 * hash + Objects.hashCode(this.checksum);
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
    final Artifact other = (Artifact) obj;
    if (!Objects.equals(this.identifier, other.identifier)) {
      return false;
    }
    if (!Objects.equals(this.version, other.version)) {
      return false;
    }
    if (!Objects.equals(this.scope, other.scope)) {
      return false;
    }
    if (!Objects.equals(this.checksum, other.checksum)) {
      return false;
    }
    return true;
  }
}
