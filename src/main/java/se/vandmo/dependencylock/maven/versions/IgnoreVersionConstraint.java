package se.vandmo.dependencylock.maven.versions;

final class IgnoreVersionConstraint extends VersionConstraint {
  IgnoreVersionConstraint() {
    super();
  }

  @Override
  public <T, C> T accept(VersionConstraintVisitor<T, C> visitor, C context) {
    return visitor.onIgnoreVersion(context);
  }

  @Override
  public boolean compliantWith(VersionConstraint other, VersionConstraintContext context) {
    return true;
  }

  @Override
  public String toString() {
    return "ignored";
  }
}
