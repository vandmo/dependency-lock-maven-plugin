package se.vandmo.dependencylock.maven;

import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;

import com.google.common.io.BaseEncoding;
import com.google.common.io.Files;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;
import java.util.Optional;
import org.apache.maven.model.Dependency;

public final class Artifact implements Comparable<Artifact> {

  public final ArtifactIdentifier identifier;
  public final String version;
  public final String scope;
  public final String type;
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

    public TypeBuilderStage scope(String scope) {
      return new TypeBuilderStage(artifactIdentifier, version, requireNonNull(scope));
    }
  }

  public static final class TypeBuilderStage {
    private final ArtifactIdentifier artifactIdentifier;
    private final String version;
    private final String scope;

    private TypeBuilderStage(ArtifactIdentifier artifactIdentifier, String version, String scope) {
      this.artifactIdentifier = artifactIdentifier;
      this.version = version;
      this.scope = scope;
    }

    public ChecksumBuilderStage type(String type) {
      return new ChecksumBuilderStage(artifactIdentifier, version, scope, requireNonNull(type));
    }
  }

  public static final class ChecksumBuilderStage {
    private final ArtifactIdentifier artifactIdentifier;
    private final String version;
    private final String scope;
    private final String type;

    private ChecksumBuilderStage(
        ArtifactIdentifier artifactIdentifier, String version, String scope, String type) {
      this.artifactIdentifier = artifactIdentifier;
      this.version = version;
      this.scope = scope;
      this.type = type;
    }

    public FinalBuilderStage checksum(Optional<String> checksum) {
      return new FinalBuilderStage(
          artifactIdentifier, version, scope, type, requireNonNull(checksum));
    }
  }

  public static final class FinalBuilderStage {
    private final ArtifactIdentifier artifactIdentifier;
    private final String version;
    private final String scope;
    private final String type;
    private final Optional<String> checksum;

    private FinalBuilderStage(
        ArtifactIdentifier artifactIdentifier,
        String version,
        String scope,
        String type,
        Optional<String> checksum) {
      this.artifactIdentifier = artifactIdentifier;
      this.version = version;
      this.scope = scope;
      this.type = type;
      this.checksum = checksum;
    }

    public Artifact build() {
      return new Artifact(artifactIdentifier, version, scope, type, false, checksum);
    }
  }

  public static Artifact from(
      org.apache.maven.artifact.Artifact artifact, boolean enableIntegrityChecking) {
    return new Artifact(
        new ArtifactIdentifier(
            artifact.getGroupId(),
            artifact.getArtifactId(),
            ofNullable(artifact.getClassifier()),
            ofNullable(artifact.getType())),
        artifact.getVersion(),
        artifact.getScope(),
        artifact.getType(),
        artifact.isOptional(),
        enableIntegrityChecking
            ? Optional.of(Checksum.calculateFor(artifact.getFile()))
            : Optional.empty());
  }

  public static Artifact from(Dependency dependency) {
    return new Artifact(
        new ArtifactIdentifier(
            dependency.getGroupId(),
            dependency.getArtifactId(),
            ofNullable(dependency.getClassifier()),
            ofNullable(dependency.getType())),
        dependency.getVersion(),
        dependency.getScope(),
        dependency.getType(),
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
      String type,
      boolean optional,
      Optional<String> checksum) {
    this.identifier = requireNonNull(identifier);
    this.version = requireNonNull(version);
    this.scope = requireNonNull(scope);
    this.type = requireNonNull(type);
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
        .append(':')
        .append(type)
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
    hash = 17 * hash + Objects.hashCode(this.type);
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
    if (!Objects.equals(this.type, other.type)) {
      return false;
    }
    if (!Objects.equals(this.checksum, other.checksum)) {
      return false;
    }
    return true;
  }
}
