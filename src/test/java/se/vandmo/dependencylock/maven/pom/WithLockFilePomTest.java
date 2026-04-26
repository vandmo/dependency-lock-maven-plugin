package se.vandmo.dependencylock.maven.pom;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.apache.maven.project.MavenProject;
import org.junit.Assert;
import org.junit.Test;
import se.vandmo.dependencylock.maven.ArtifactIdentifier;
import se.vandmo.dependencylock.maven.Dependencies;
import se.vandmo.dependencylock.maven.Dependency;
import se.vandmo.dependencylock.maven.Integrity;
import se.vandmo.dependencylock.maven.PomMinimums;
import se.vandmo.dependencylock.maven.ProfileEntry;
import se.vandmo.dependencylock.maven.ProfiledDependencies;
import se.vandmo.dependencylock.maven.mojos.model.Profile;

public class WithLockFilePomTest {

  @Test
  public void makeDataModel_ignores_empty_entries() {
    Map<String, Object> dataModel =
        new WithLockFilePom()
            .makeDataModel(
                getPomMinimums(),
                new ProfiledDependencies(
                    Dependencies.fromDependencies(Collections.emptyList()),
                    Arrays.asList(
                        new ProfileEntry(
                            new Profile(),
                            Dependencies.fromDependencies(Collections.emptyList())))));
    Assert.assertEquals(Collections.emptyList(), dataModel.get("profiles"));
  }

  private static PomMinimums getPomMinimums() {
    return PomMinimums.from(new MavenProject());
  }

  @Test
  public void makeDataModel_ensures_sorting_is_performed() {
    final Profile firstProfile = new Profile();
    firstProfile.setId("b");
    final Profile secondProfile = new Profile();
    secondProfile.setId("a");
    final Dependency firstProfileDependency =
        Dependency.builder()
            .artifactIdentifier(
                ArtifactIdentifier.builder()
                    .groupId("se.vandmo.dependency")
                    .artifactId("first-profile-artifact-id")
                    .build())
            .version("1.0.0")
            .integrity(Integrity.Ignored())
            .scope("runtime")
            .build();
    final Dependency secondProfileDependency =
        Dependency.builder()
            .artifactIdentifier(
                ArtifactIdentifier.builder()
                    .groupId("se.vandmo.dependency")
                    .artifactId("second-profile-artifact-id")
                    .build())
            .version("1.0.0")
            .integrity(Integrity.Ignored())
            .scope("runtime")
            .build();
    Map<String, Object> dataModel =
        new WithLockFilePom()
            .makeDataModel(
                getPomMinimums(),
                new ProfiledDependencies(
                    Dependencies.fromDependencies(Collections.emptyList()),
                    Arrays.asList(
                        new ProfileEntry(
                            firstProfile,
                            Dependencies.fromDependencies(
                                Collections.singletonList(firstProfileDependency))),
                        new ProfileEntry(
                            secondProfile,
                            Dependencies.fromDependencies(
                                Collections.singletonList(secondProfileDependency))))));
    final Object profiles = dataModel.get("profiles");
    Assert.assertTrue(profiles instanceof List);
    final List<?> profilesList = (List<?>) profiles;
    Assert.assertEquals("Unexpected number of profiles entries generated.", 2, profilesList.size());
    Assert.assertTrue(profilesList.get(0) instanceof Map);
    Assert.assertEquals(
        "Unexpected ordering of profiles entries found",
        secondProfile.getId(),
        ((Map) profilesList.get(0)).get("id"));
    Assert.assertTrue(profilesList.get(1) instanceof Map);
    Assert.assertEquals(
        "Unexpected ordering of profiles entries found",
        firstProfile.getId(),
        ((Map) profilesList.get(1)).get("id"));
  }
}
