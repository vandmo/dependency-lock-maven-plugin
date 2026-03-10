import static org.junit.Assert.assertEquals;

import org.junit.Test;
import se.vandmo.dependencylock.maven.ArtifactIdentifier;
import se.vandmo.dependencylock.maven.Dependency;
import se.vandmo.dependencylock.maven.Integrity;

public final class DependencyTests {

  @Test(expected = NullPointerException.class)
  public void from_null() {
    Dependency.from(null);
  }

  @Test(expected = NullPointerException.class)
  public void builder_artifactIdentifier_null() {
    Dependency.builder().artifactIdentifier(null);
  }

  @Test(expected = NullPointerException.class)
  public void builder_version_null() {
    Dependency.builder().artifactIdentifier(anArtifactIdentifier()).version(null);
  }

  @Test(expected = NullPointerException.class)
  public void builder_scope_null() {
    Dependency.builder()
        .artifactIdentifier(anArtifactIdentifier())
        .version("1")
        .integrity("sha512:123abc")
        .scope(null);
  }

  @Test(expected = NullPointerException.class)
  public void builder_integrity_string_null() {
    Dependency.builder()
        .artifactIdentifier(anArtifactIdentifier())
        .version("1")
        .integrity((String) null);
  }

  @Test(expected = NullPointerException.class)
  public void builder_integrity_Integrity_null() {
    Dependency.builder()
        .artifactIdentifier(anArtifactIdentifier())
        .version("1")
        .integrity((Integrity) null);
  }

  @Test
  public void toString_withoutIntegrity() {
    Dependency dependency =
        Dependency.builder()
            .artifactIdentifier(
                ArtifactIdentifier.builder()
                    .groupId("the_groupId")
                    .artifactId("the_artifactId")
                    .classifier("the_classifier")
                    .type("war")
                    .build())
            .version("1.2.3")
            .integrity("sha512:123abc")
            .scope("compile")
            .build();
    assertEquals(
        "the_groupId:the_artifactId:the_classifier:war:1.2.3:compile:optional=false",
        dependency.toString_withoutIntegrity());
  }

  private static ArtifactIdentifier anArtifactIdentifier() {
    return ArtifactIdentifier.builder().groupId("the_groupId").artifactId("the_artifactId").build();
  }
}
