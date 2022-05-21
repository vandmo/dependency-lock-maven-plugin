package se.vandmo.dependencylock.maven;

import static java.util.Objects.requireNonNull;

import org.apache.maven.artifact.resolver.filter.ArtifactFilter;

public final class Filters {
  public final ArtifactFilter useMyVersionForFilter;
  public final ArtifactFilter ignoreFilter;

  private Filters(ArtifactFilter useMyVersionForFilter, ArtifactFilter ignoreFilter) {
    this.useMyVersionForFilter = useMyVersionForFilter;
    this.ignoreFilter = ignoreFilter;
  }

  public static UseMyVersionForFilterBuilderStage builder() {
    return new UseMyVersionForFilterBuilderStage();
  }

  public static final class UseMyVersionForFilterBuilderStage {
    private UseMyVersionForFilterBuilderStage() {}
    public IgnoreFilterBuilderStage useMyVersionForFilter(ArtifactFilter useMyVersionForFilter) {
      return new IgnoreFilterBuilderStage(requireNonNull(useMyVersionForFilter));
    }
  }

  public static final class IgnoreFilterBuilderStage {
    private final ArtifactFilter useMyVersionForFilter;
    private IgnoreFilterBuilderStage(ArtifactFilter useMyVersionForFilter) {
      this.useMyVersionForFilter = useMyVersionForFilter;
    }
    public FinalBuilderStage ignoreFilter(ArtifactFilter ignoreFilter) {
      return new FinalBuilderStage(useMyVersionForFilter, requireNonNull(ignoreFilter));
    }
  }

  public static final class FinalBuilderStage {
    private final ArtifactFilter useMyVersionForFilter;
    private final ArtifactFilter ignoreFilter;
    private FinalBuilderStage(ArtifactFilter useMyVersionForFilter, ArtifactFilter ignoreFilter) {
      this.useMyVersionForFilter = useMyVersionForFilter;
      this.ignoreFilter = ignoreFilter;
    }
    public Filters build() {
      return new Filters(useMyVersionForFilter, ignoreFilter);
    }
  }

}
