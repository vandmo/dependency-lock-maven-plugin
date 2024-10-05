package se.vandmo.dependencylock.maven;

import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;
import static se.vandmo.dependencylock.maven.Checksum.ALGORITHM_HEADER;

import java.util.Objects;
import se.vandmo.dependencylock.maven.lang.Strings;

public final class Artifact implements Comparable<Artifact> {

  public final ArtifactIdentifier identifier;
  public final String version;
  public final String scope;
  public final boolean optional;
  public final String integrity;

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

    private OptionalBuilderStage(
        ArtifactIdentifier artifactIdentifier, String version, String scope) {
      this.artifactIdentifier = artifactIdentifier;
      this.version = version;
      this.scope = scope;
    }

    public IntegrityBuilderStage optional(boolean optional) {
      return integrityBuilderStage(optional);
    }

    public FinalBuilderStage integrity(String integrity) {
      return integrityBuilderStage(false).integrity(integrity);
    }

    private IntegrityBuilderStage integrityBuilderStage(boolean optional) {
      return new IntegrityBuilderStage(artifactIdentifier, version, scope, optional);
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

    public FinalBuilderStage integrity(String integrity) {
      return new FinalBuilderStage(
          artifactIdentifier, version, scope, optional, checkIntegrityArgument(integrity));
    }
  }

  private static String checkIntegrityArgument(String integrity) {
    requireNonNull(integrity);
    if (!Strings.startsWith(integrity, ALGORITHM_HEADER) && !"ignored".equals(integrity)) {
      throw new IllegalArgumentException(
          "Encountered unsupported checksum format, consider using a later version of this plugin");
    }
    return integrity;
  }

  public static final class FinalBuilderStage {
    private final ArtifactIdentifier artifactIdentifier;
    private final String version;
    private final String scope;
    private final boolean optional;
    private final String integrity;

    private FinalBuilderStage(
        ArtifactIdentifier artifactIdentifier,
        String version,
        String scope,
        boolean optional,
        String integrity) {
      this.artifactIdentifier = artifactIdentifier;
      this.version = version;
      this.scope = scope;
      this.optional = optional;
      this.integrity = integrity;
    }

    public Artifact build() {
      return new Artifact(artifactIdentifier, version, scope, optional, integrity);
    }
  }

  public static Artifact from(org.apache.maven.artifact.Artifact artifact) {
    return new Artifact(
        ArtifactIdentifier.builder()
            .groupId(artifact.getGroupId())
            .artifactId(artifact.getArtifactId())
            .classifier(ofNullable(artifact.getClassifier()))
            .type(ofNullable(artifact.getType()))
            .build(),
        artifact.getVersion(),
        artifact.getScope(),
        artifact.isOptional(),
        Checksum.calculateFor(artifact.getFile()));
  }

  public org.apache.maven.artifact.Artifact toMavenArtifact() {
    return new MavenArtifact(this);
  }

  private Artifact(
      ArtifactIdentifier identifier,
      String version,
      String scope,
      boolean optional,
      String integrity) {
    this.identifier = requireNonNull(identifier);
    this.version = requireNonNull(version);
    this.scope = requireNonNull(scope);
    this.optional = optional;
    this.integrity = integrity;
  }

  public Artifact withVersion(String version) {
    return new Artifact(identifier, version, scope, optional, integrity);
  }

  public Artifact withIntegrity(String integrity) {
    return new Artifact(identifier, version, scope, optional, integrity);
  }

  @Override
  public int compareTo(Artifact other) {
    return toString().compareTo(other.toString());
  }

  @Override
  public String toString() {
    return toStringBuilder_withoutIntegrity().append('@').append(integrity).toString();
  }

  public String toString_withoutIntegrity() {
    return toStringBuilder_withoutIntegrity().toString();
  }

  private StringBuilder toStringBuilder_withoutIntegrity() {
    return new StringBuilder()
        .append(identifier.toString())
        .append(':')
        .append(version)
        .append(':')
        .append(scope)
        .append(":optional=")
        .append(optional);
  }

  @Override
  public int hashCode() {
    int hash = 7;
    hash = 17 * hash + Objects.hashCode(this.identifier);
    hash = 17 * hash + Objects.hashCode(this.version);
    hash = 17 * hash + Objects.hashCode(this.scope);
    hash = 17 * hash + Objects.hashCode(this.optional);
    hash = 17 * hash + Objects.hashCode(this.integrity);
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
    if (!Objects.equals(this.optional, other.optional)) {
      return false;
    }
    if (!Objects.equals(this.integrity, other.integrity)) {
      return false;
    }
    return true;
  }

  public boolean equals_ignoreVersion(Artifact other) {
    if (this == other) {
      return true;
    }
    if (other == null) {
      return false;
    }
    if (getClass() != other.getClass()) {
      return false;
    }
    if (!Objects.equals(this.identifier, other.identifier)) {
      return false;
    }
    if (!Objects.equals(this.scope, other.scope)) {
      return false;
    }
    if (!Objects.equals(this.optional, other.optional)) {
      return false;
    }
    if (!Objects.equals(this.integrity, other.integrity)) {
      return false;
    }
    return true;
  }
}
