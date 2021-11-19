package se.vandmo.dependencylock.maven;

import java.io.File;
import java.util.Collection;
import java.util.List;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.VersionRange;

public final class MavenArtifact implements org.apache.maven.artifact.Artifact {

  private final se.vandmo.dependencylock.maven.Artifact delegate;
  private final String baseVersion;

  public MavenArtifact(se.vandmo.dependencylock.maven.Artifact delegate) {
    this.delegate = delegate;
    this.baseVersion = ArtifactUtils.toSnapshotVersion(delegate.version);
  }

  @Override
  public String getGroupId() {
    return delegate.identifier.groupId;
  }

  @Override
  public String getArtifactId() {
    return delegate.identifier.artifactId;
  }

  @Override
  public String getVersion() {
    return delegate.version;
  }

  @Override
  public void setVersion(String version) {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getScope() {
    return delegate.scope;
  }

  @Override
  public String getType() {
    return delegate.type;
  }

  @Override
  public String getClassifier() {
    return delegate.identifier.classifier.orElse(null);
  }

  @Override
  public boolean hasClassifier() {
    return delegate.identifier.classifier.isPresent();
  }

  @Override
  public File getFile() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setFile(File destination) {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getBaseVersion() {
    return baseVersion;
  }

  @Override
  public void setBaseVersion(String baseVersion) {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getId() {
    return delegate.identifier.toString();
  }

  @Override
  public String getDependencyConflictId() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void addMetadata(ArtifactMetadata metadata) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Collection<ArtifactMetadata> getMetadataList() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setRepository(ArtifactRepository remoteRepository) {
    throw new UnsupportedOperationException();
  }

  @Override
  public ArtifactRepository getRepository() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void updateVersion(String version, ArtifactRepository localRepository) {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getDownloadUrl() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setDownloadUrl(String downloadUrl) {
    throw new UnsupportedOperationException();
  }

  @Override
  public ArtifactFilter getDependencyFilter() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setDependencyFilter(ArtifactFilter artifactFilter) {
    throw new UnsupportedOperationException();
  }

  @Override
  public ArtifactHandler getArtifactHandler() {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<String> getDependencyTrail() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setDependencyTrail(List<String> dependencyTrail) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setScope(String scope) {
    throw new UnsupportedOperationException();
  }

  @Override
  public VersionRange getVersionRange() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setVersionRange(VersionRange newRange) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void selectVersion(String version) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setGroupId(String groupId) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setArtifactId(String artifactId) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isSnapshot() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setResolved(boolean resolved) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isResolved() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setResolvedVersion(String version) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setArtifactHandler(ArtifactHandler handler) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isRelease() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setRelease(boolean release) {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<ArtifactVersion> getAvailableVersions() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setAvailableVersions(List<ArtifactVersion> versions) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isOptional() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setOptional(boolean optional) {
    throw new UnsupportedOperationException();
  }

  @Override
  public ArtifactVersion getSelectedVersion() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isSelectedVersionKnown() {
    throw new UnsupportedOperationException();
  }

  @Override
  public int compareTo(Artifact artifact) {
    throw new UnsupportedOperationException();
  }
}
