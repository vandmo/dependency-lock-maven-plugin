package se.vandmo.dependencylock.maven;

import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;
import static se.vandmo.dependencylock.maven.Checksum.ALGORITHM_HEADER;

import java.util.Objects;
import se.vandmo.dependencylock.maven.lang.Strings;

public final class Artifact extends LockableEntity implements Comparable<Artifact> {

  public final ArtifactIdentifier identifier;
  public final String version;
  public final Integrity integrity;
  private org.apache.maven.artifact.Artifact mavenArtifact;

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

    public IntegrityBuilderStage version(String version) {
      return new IntegrityBuilderStage(artifactIdentifier, requireNonNull(version));
    }
  }

  public static final class IntegrityBuilderStage {
    private final ArtifactIdentifier artifactIdentifier;
    private final String version;

    private IntegrityBuilderStage(ArtifactIdentifier artifactIdentifier, String version) {
      this.artifactIdentifier = artifactIdentifier;
      this.version = version;
    }

    public FinalBuilderStage integrity(String integrity) {
      return new FinalBuilderStage(
          artifactIdentifier, version, Integrity.Calculated(checkIntegrityArgument(integrity)));
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
    private final Integrity integrity;

    private FinalBuilderStage(
        ArtifactIdentifier artifactIdentifier, String version, Integrity integrity) {
      this.artifactIdentifier = artifactIdentifier;
      this.version = version;
      this.integrity = integrity;
    }

    public Artifact build() {
      return new Artifact(artifactIdentifier, version, integrity);
    }
  }

  public static Artifact from(org.apache.maven.artifact.Artifact artifact) {
    Integrity integrity =
        artifact.getFile().isDirectory()
            ? Integrity.Folder()
            : Integrity.Calculated(Checksum.calculateFor(artifact.getFile()));
    return new Artifact(
        ArtifactIdentifier.builder()
            .groupId(artifact.getGroupId())
            .artifactId(artifact.getArtifactId())
            .classifier(ofNullable(artifact.getClassifier()))
            .type(ofNullable(artifact.getType()))
            .build(),
        artifact.getVersion(),
        integrity);
  }

  public org.apache.maven.artifact.Artifact getMavenArtifact() {
    org.apache.maven.artifact.Artifact result = mavenArtifact;
    if (result == null) {
      result = MavenArtifact.unscoped(this);
      mavenArtifact = result;
    }
    return result;
  }

  private Artifact(ArtifactIdentifier identifier, String version, Integrity integrity) {
    this.identifier = requireNonNull(identifier);
    this.version = requireNonNull(version);
    this.integrity = integrity;
  }

  public Artifact withVersion(String version) {
    return new Artifact(identifier, version, integrity);
  }

  public Artifact withIntegrity(Integrity integrity) {
    return new Artifact(identifier, version, integrity);
  }

  public String getIntegrityForLockFile() {
    return integrity
        .<String>matching()
        .Calculated((calculated) -> calculated.checksum)
        .Folder(
            (folder) -> {
              throw new IllegalStateException(
                  "Can not calculate dependencies for "
                      + toString_withoutIntegrity()
                      + " since it is a folder.");
            })
        .Ignored((ignored) -> "ignored")
        .get();
  }

  @Override
  public int compareTo(Artifact other) {
    return toString().compareTo(other.toString());
  }

  @Override
  public String toString() {
    return toStringBuilder_withoutIntegrity()
        .append('@')
        .append(
            integrity
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

  StringBuilder toStringBuilder_withoutIntegrity() {
    return new StringBuilder().append(identifier.toString()).append(':').append(version);
  }

  @Override
  public int hashCode() {
    int hash = 7;
    hash = 17 * hash + Objects.hashCode(this.identifier);
    hash = 17 * hash + Objects.hashCode(this.version);
    hash = 17 * hash + Objects.hashCode(this.integrity);
    return hash;
  }

  @Override
  public ArtifactIdentifier getArtifactIdentifier() {
    return identifier;
  }

  @Override
  public Integrity getIntegrity() {
    return integrity;
  }

  @Override
  public String getArtifactKey() {
    return toString_withoutIntegrity();
  }

  @Override
  public String getVersion() {
    return version;
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
    if (!Objects.equals(this.integrity, other.integrity)) {
      return false;
    }
    return true;
  }
}
