package se.vandmo.dependencylock.maven;

import static java.util.Objects.requireNonNull;

import java.util.Objects;
import java.util.function.Predicate;

public final class LockedDependency implements Comparable<LockedDependency>, Predicate<Artifact> {

  public final ArtifactIdentifier identifier;
  public final String version;
  public final String scope;
  public final boolean optional;
  public final String integrity;

  private LockedDependency(
      ArtifactIdentifier identifier,
      String version,
      String scope,
      boolean optional,
      String integrity) {
    this.identifier = requireNonNull(identifier);
    this.version = requireNonNull(version);
    this.scope = requireNonNull(scope);
    this.optional = optional;
    this.integrity = checkIntegrityArgument(integrity);
  }

  private String checkIntegrityArgument(String integrity) {
    requireNonNull(integrity);
    Checksum.checkAlgorithmHeader(
        integrity,
        "Encountered unsupported checksum format, consider using a later version of this plugin");
    return integrity;
  }

  public static LockedDependency from(Artifact artifact) {
    return new LockedDependency(
        artifact.identifier,
        artifact.version,
        artifact.scope,
        artifact.optional,
        artifact.integrity);
  }

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

    public FinalBuilderStage integrity(String integrity) {
      return new FinalBuilderStage(
          artifactIdentifier, version, scope, optional, requireNonNull(integrity));
    }
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

    public LockedDependency build() {
      return new LockedDependency(artifactIdentifier, version, scope, optional, integrity);
    }
  }

  public Artifact toArtifact() {
    return Artifact.builder()
        .artifactIdentifier(identifier)
        .version(version)
        .scope(scope)
        .optional(optional)
        .integrity(integrity)
        .build();
  }

  @Override
  public boolean test(Artifact artifact) {
    return identifier.equals(artifact.identifier)
        && version.matches(artifact.version)
        && scope.equals(artifact.scope)
        && optional == artifact.optional
        && integrity.equals(artifact.integrity);
  }

  @Override
  public int compareTo(LockedDependency other) {
    return toString().compareTo(other.toString());
  }

  @Override
  public String toString() {
    return toStringWithVersion(version);
  }
  private String toStringWithVersion(String version) {
    return new StringBuilder()
        .append(identifier)
        .append(':')
        .append(version)
        .append(':')
        .append(scope)
        .append(":optional=")
        .append(optional)
        .append('@')
        .append(integrity)
        .toString();
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
    final LockedDependency other = (LockedDependency) obj;
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

  public WithMyVersion withMyVersion(String myVersion) {
    return new WithMyVersion(myVersion);
  }

  public final class WithMyVersion implements Predicate<Artifact> {

    private final String myVersion;

    private WithMyVersion(String myVersion) {
      this.myVersion = myVersion;
    }

    public String toString() {
      return toStringWithVersion(myVersion);
    }

    @Override
    public boolean test(Artifact artifact) {
      return identifier.equals(artifact.identifier)
          && version.matches(myVersion)
          && scope.equals(artifact.scope)
          && optional == artifact.optional
          && integrity.equals(artifact.integrity);
    }
  }

}
