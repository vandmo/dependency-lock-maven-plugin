package se.vandmo.dependencylock.maven.versions;

final class UseProjectVersionConstraint extends VersionConstraint {

  UseProjectVersionConstraint() {
    super();
  }

  @Override
  public <T, C> T accept(VersionConstraintVisitor<T, C> visitor, C context) {
    return visitor.onProjectVersion(context);
  }

  /**
   * Returns <code>true</code> if the given constraint ignores versions, accepts project versions or
   * is strict about versions and accepts the given context project version.
   *
   * @param other the constraint against which this constraint is to be compared.
   * @param context the context in which the comparison is to be performed.
   * @return true if, and only if, the given constraint accepts project versions, all versions, or
   *     the given context project version.
   */
  @Override
  public boolean compliantWith(VersionConstraint other, VersionConstraintContext context) {
    return other.accept(ProjectVersionCompliancyChecker.INSTANCE, context).booleanValue();
  }

  @Override
  public String toString() {
    return "project-version";
  }

  private static final class ProjectVersionCompliancyChecker
      implements VersionConstraintVisitor<Boolean, VersionConstraintContext> {
    static final ProjectVersionCompliancyChecker INSTANCE = new ProjectVersionCompliancyChecker();

    private ProjectVersionCompliancyChecker() {
      super();
    }

    @Override
    public Boolean onVersion(String version, VersionConstraintContext context) {
      return version.equals(context.getProjectVersion());
    }

    @Override
    public Boolean onProjectVersion(VersionConstraintContext context) {
      return Boolean.TRUE;
    }

    @Override
    public Boolean onIgnoreVersion(VersionConstraintContext context) {
      return Boolean.TRUE;
    }
  }
}
