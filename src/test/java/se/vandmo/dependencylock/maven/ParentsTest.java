package se.vandmo.dependencylock.maven;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.testing.MojoRule;
import org.apache.maven.plugin.testing.WithoutMojo;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.artifact.DefaultArtifactType;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.VersionRangeRequest;
import org.eclipse.aether.resolution.VersionRangeResult;
import org.eclipse.aether.util.version.GenericVersionScheme;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestName;
import org.mockito.Mockito;

public class ParentsTest {

  @Rule public final MojoRule mojoRule = new MojoRule();

  @Rule public final TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Rule public final TestName testName = new TestName();

  private RepositorySystem repositorySystem;

  @Before
  public void before() {
    repositorySystem = Mockito.mock(RepositorySystem.class);
    mojoRule
        .getContainer()
        .addComponent(repositorySystem, org.eclipse.aether.RepositorySystem.class, null);
  }

  private MavenProject mavenProject() throws Exception {
    return mavenProject(temporaryFolder.newFolder());
  }

  private MavenProject mavenProject(File directory) throws Exception {
    URL url =
        getClass()
            .getResource(getClass().getSimpleName() + "/" + this.testName.getMethodName() + ".xml");
    Assert.assertNotNull(url);
    final Path directoryPath = directory.toPath();
    Files.createDirectories(directoryPath);
    Files.write(directoryPath.resolve("pom.xml"), IOUtils.toByteArray(url));
    return mojoRule.readMavenProject(directory);
  }

  @Test
  @WithoutMojo
  public void from_returnsNullIfNoParentArtifact() throws Exception {
    MavenProject project = mavenProject();
    Parents parents = Parents.from(project);
    Assert.assertArrayEquals(new Parent[0], parents.stream().toArray());
  }

  @Test
  @WithoutMojo
  public void from_returnsArtifactFileBasedInfoIfRelativePathEmpty() throws Exception {
    File tempFolder = temporaryFolder.newFolder();
    final Path parentPomFilePath = tempFolder.toPath().resolve("pom.xml");
    Files.copy(
        getClass()
            .getResourceAsStream(
                getClass().getSimpleName() + "/" + this.testName.getMethodName() + "_parent.xml"),
        parentPomFilePath);
    Mockito.doAnswer(
            invocationOnMock -> {
              final VersionRangeRequest request = invocationOnMock.getArgument(1);
              return new VersionRangeResult(request)
                  .addVersion(new GenericVersionScheme().parseVersion("0-SNAPSHOT"));
            })
        .when(repositorySystem)
        .resolveVersionRange(
            Mockito.any(RepositorySystemSession.class), Mockito.any(VersionRangeRequest.class));
    Mockito.doAnswer(
            invocationOnMock -> {
              final ArtifactRequest request = invocationOnMock.getArgument(1);
              return new ArtifactResult(request)
                  .setArtifact(
                      new DefaultArtifact(
                              "se.vandmo.tests",
                              "parent-pom",
                              null,
                              "xml",
                              "0-SNAPSHOT",
                              new DefaultArtifactType("pom"))
                          .setFile(parentPomFilePath.toFile()));
            })
        .when(repositorySystem)
        .resolveArtifact(Mockito.any(RepositorySystemSession.class), Mockito.any());
    MavenProject project = mavenProject();
    Parents parents = Parents.from(project);
    Assert.assertEquals(1, parents.size());
    Assert.assertArrayEquals(
        new Parent[] {
          Parent.builder()
              .artifactIdentifier(
                  ArtifactIdentifier.builder()
                      .groupId("se.vandmo.tests")
                      .artifactId("parent-pom")
                      .type("pom")
                      .build())
              .version("0-SNAPSHOT")
              .integrity(
                  "sha512:B/RGlTb03YLK0wl6k3WKuY7nPWoNJAUQKT632q4WCpS6JHkKsiTdJM5q//KhUoWVt0/mYDCVDNDwJKN8S5+juA==")
              .build()
        },
        parents.stream().toArray());
  }

  @Test
  @WithoutMojo
  public void from_returnsDirBasedInfoIfRelativePathSetToADirectoryPath() throws Exception {
    final File temporaryDir = temporaryFolder.newFolder();
    final Path parentProjectDirectory = temporaryDir.toPath().resolve("dir-path");
    Files.createDirectories(parentProjectDirectory);
    Files.copy(
        getClass()
            .getResourceAsStream(
                getClass().getSimpleName() + "/" + this.testName.getMethodName() + "_parent.xml"),
        parentProjectDirectory.resolve("pom.xml"));
    final MavenProject mavenProject = mavenProject(temporaryDir);
    final Parents parents = Parents.from(mavenProject);
    Assert.assertArrayEquals(
        new Parent[] {
          Parent.builder()
              .artifactIdentifier(
                  ArtifactIdentifier.builder()
                      .groupId("se.vandmo.tests")
                      .artifactId("parent-pom")
                      .type("pom")
                      .build())
              .version("1-SNAPSHOT")
              .integrity(
                  "sha512:F10c+XI2f2EliNG4g4L1RmTFUYKx5Z2kSMRLJhGKHTWRo8fyxEAU7hUUQKfE31WVfJT6G3Wkk0QV7U9EUbGgQw==")
              .build()
        },
        parents.stream().toArray());
  }

  @Test
  @WithoutMojo
  public void from_returnsDirectIfRelativePathSetToAFilePath() throws Exception {
    final File temporaryDir = temporaryFolder.newFolder();
    final Path parentPomDirectory = temporaryDir.toPath().resolve("dir-path");
    Files.createDirectories(parentPomDirectory);
    Files.copy(
        getClass()
            .getResourceAsStream(
                getClass().getSimpleName() + "/" + this.testName.getMethodName() + "_parent.xml"),
        parentPomDirectory.resolve("random-file-name.xml"));
    final MavenProject mavenProject = mavenProject(temporaryDir);
    final Parents parents = Parents.from(mavenProject);
    Assert.assertArrayEquals(
        new Parent[] {
          Parent.builder()
              .artifactIdentifier(
                  ArtifactIdentifier.builder()
                      .groupId("se.vandmo.tests")
                      .artifactId("parent-pom")
                      .type("pom")
                      .build())
              .version("2-SNAPSHOT")
              .integrity(
                  "sha512:PablsG6vcNsEV4PQGGzJfZU0+Sr4ZyVoNqiKuSmW52voq1/kRPrArrZ3CRcoKkI4vTtIv+HurfYAgkRunmtIlA==")
              .build()
        },
        parents.stream().toArray());
  }
}
