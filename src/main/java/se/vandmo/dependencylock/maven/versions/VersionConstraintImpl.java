package se.vandmo.dependencylock.maven.versions;

import java.util.Objects;

final class VersionConstraintImpl extends VersionConstraint
    implements VersionConstraintVisitor<Boolean, VersionConstraintContext> {
  private final String version;

  VersionConstraintImpl(String version) {
    this.version = version;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof VersionConstraintImpl)) return false;
    VersionConstraintImpl that = (VersionConstraintImpl) o;
    return Objects.equals(version, that.version);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(version);
  }

  @Override
  public <T, C> T accept(VersionConstraintVisitor<T, C> visitor, C context) {
    return visitor.onVersion(version, context);
  }

  @Override
  public Boolean onVersion(String version, VersionConstraintContext context) {
    return this.version.equals(version);
  }

  @Override
  public Boolean onProjectVersion(VersionConstraintContext context) {
    return this.version.equals(context.getProjectVersion());
  }

  @Override
  public Boolean onIgnoreVersion(VersionConstraintContext context) {
    return Boolean.TRUE;
  }

  @Override
  public boolean compliantWith(VersionConstraint other, VersionConstraintContext context) {
    return other.accept(this, context).booleanValue();
  }

  @Override
  public String toString() {
    return this.version;
  }
}
