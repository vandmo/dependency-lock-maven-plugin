import static org.junit.Assert.assertEquals;

import org.junit.Test;
import io.mvnpm.maven.locker.Artifact;
import io.mvnpm.maven.locker.ArtifactIdentifier;

public final class ArtifactTests {

  @Test(expected = NullPointerException.class)
  public void from_null() {
    Artifact.from(null);
  }

  @Test(expected = NullPointerException.class)
  public void builder_artifactIdentifier_null() {
    Artifact.builder().artifactIdentifier(null);
  }

  @Test(expected = NullPointerException.class)
  public void builder_version_null() {
    Artifact.builder().artifactIdentifier(anArtifactIdentifier()).version(null);
  }

  @Test(expected = NullPointerException.class)
  public void builder_scope_null() {
    Artifact.builder().artifactIdentifier(anArtifactIdentifier()).version("1").scope(null);
  }

  @Test(expected = NullPointerException.class)
  public void builder_integrity_null() {
    Artifact.builder()
        .artifactIdentifier(anArtifactIdentifier())
        .version("1")
        .scope("compile")
        .integrity(null);
  }

  @Test
  public void toString_withoutIntegrity() {
    Artifact artifact =
        Artifact.builder()
            .artifactIdentifier(
                ArtifactIdentifier.builder()
                    .groupId("the_groupId")
                    .artifactId("the_artifactId")
                    .classifier("the_classifier")
                    .type("war")
                    .build())
            .version("1.2.3")
            .scope("compile")
            .integrity("sha512:123abc")
            .build();
    assertEquals(
        "the_groupId:the_artifactId:the_classifier:war:1.2.3:compile:optional=false",
        artifact.toString_withoutIntegrity());
  }

  private static ArtifactIdentifier anArtifactIdentifier() {
    return ArtifactIdentifier.builder().groupId("the_groupId").artifactId("the_artifactId").build();
  }
}
