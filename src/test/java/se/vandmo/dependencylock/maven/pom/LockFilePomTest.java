package se.vandmo.dependencylock.maven.pom;

import static java.util.Arrays.asList;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.Optional;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import org.apache.maven.monitor.logging.DefaultLog;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestName;
import org.xml.sax.SAXException;
import se.vandmo.dependencylock.maven.Artifact;
import se.vandmo.dependencylock.maven.ArtifactIdentifier;
import se.vandmo.dependencylock.maven.Artifacts;
import se.vandmo.dependencylock.maven.Dependencies;
import se.vandmo.dependencylock.maven.Dependency;
import se.vandmo.dependencylock.maven.Extension;
import se.vandmo.dependencylock.maven.Extensions;
import se.vandmo.dependencylock.maven.LockFileAccessor;
import se.vandmo.dependencylock.maven.LockedProject;
import se.vandmo.dependencylock.maven.Parent;
import se.vandmo.dependencylock.maven.Parents;
import se.vandmo.dependencylock.maven.Plugin;
import se.vandmo.dependencylock.maven.Plugins;
import se.vandmo.dependencylock.maven.PomMinimums;

public class LockFilePomTest {

  @Rule public final TemporaryFolder folder = new TemporaryFolder();

  @Rule public final TestName testName = new TestName();
  private File lockfiles;

  @Before
  public void setUp() throws Exception {
    lockfiles = folder.newFolder("lockfiles", testName.getMethodName());
  }

  @Test
  public void write_generates_a_xsd_compliant_resource_according_to_xerces()
      throws IOException, SAXException, XMLStreamException {
    final LockFileAccessor lockfileAccessor = generateLockFile();
    final Validator schema = loadXsdValidator();
    Throwable thrownError = null;
    try {
      schema.validate(new StreamSource(lockfileAccessor.file));
    } catch (Exception e) {
      thrownError = e;
      e.printStackTrace();
    }
    Assert.assertNull("No validation error should have been thrown", thrownError);
  }

  private LockFileAccessor generateLockFile() {
    final MavenProject mavenProject = new MavenProject();
    final Log log = new DefaultLog(new ConsoleLogger());
    final LockFileAccessor lockfileAccessor =
        LockFileAccessor.fromBasedir(lockfiles, "whatever.xml");
    final LockFilePom lockFilePom =
        LockFilePom.from(lockfileAccessor, PomMinimums.from(mavenProject));
    final Dependency leafTestArtifact =
        Dependency.builder()
            .artifactIdentifier(
                ArtifactIdentifier.builder()
                    .groupId("se.vandmo.tests")
                    .artifactId("a")
                    .classifier("leaf")
                    .type("jar")
                    .build())
            .version("1.0")
            .integrity(
                "sha512:KDKGxTrNHDhrGZEGDSX2KVkn/2S0u1EBJ1B0jMfeZXZm4yeiNIvUnFdFb9W4X1w5HF7ce9ZqeQCnmHdGfTyk2w==")
            .scope("test")
            .optional(true)
            .build();
    final Dependencies dependencies =
        Dependencies.fromDependencies(Collections.singletonList(leafTestArtifact));
    final Extension mavenEnforcerExtension =
        Extension.builder()
            .artifactIdentifier(
                ArtifactIdentifier.builder()
                    .groupId("org.apache.maven.extensions")
                    .artifactId("maven-enforcer-extension")
                    .build())
            .version("3.5.0")
            .integrity(
                "sha512:wnRrq40de1k0a5eIA174WhJ5iWAd5f2rOcLwwMjaCBJ5327oDm5aN0rRtIfSkK+Z2HYJcOnmbjQNmIaZd3IoVA==")
            .build();
    final Extensions extensions =
        Extensions.from(Collections.singletonList(mavenEnforcerExtension));
    final Plugin compilerPlugin =
        Plugin.builder()
            .artifactIdentifier(
                ArtifactIdentifier.builder()
                    .groupId("org.apache.maven.plugins")
                    .artifactId("maven-compiler-plugin")
                    .type("maven-plugin")
                    .build())
            .version("3.13.0")
            .integrity(
                "sha512:Wf9vlOrQHnFRHoQiUEkp3IPSpbMNbYj5rqlHjFKry3BKdLRLZV0D1GcZ0NfdwGo2YsCTEj8L2Evv9veYPBHlGA==")
            .artifacts(
                Artifacts.fromArtifacts(
                    Collections.singletonList(
                        Artifact.builder()
                            .artifactIdentifier(
                                ArtifactIdentifier.builder()
                                    .groupId("com.thoughtworks.qdox")
                                    .artifactId("qdox")
                                    .type("jar")
                                    .build())
                            .version("2.0.3")
                            .integrity(
                                "sha512:8YRMGhdSxqt76kgyc28slGG8HTbi5lE48ok+SAVw8fyEy4Wl9ynPWPOmOi2Bp1PfcAf3Hr4rpFakjNxR2Fecyw==")
                            .build())))
            .build();
    final Plugins plugins = Plugins.from(Collections.singletonList(compilerPlugin));
    final Parents parents =
        new Parents(
            asList(
                Parent.builder()
                    .artifactIdentifier(
                        ArtifactIdentifier.builder()
                            .groupId("org.apache.maven.plugins")
                            .artifactId("maven-plugins")
                            .type("pom")
                            .build())
                    .version("43")
                    .integrity(
                        "sha512:0d6a975da79ab1fe489edc6df27057185d598b246ec4bce41694eb81cb571a53f4839c3bf96ead68580f314398b19d902ea07be129756d207cc0043803bf22d5")
                    .build(),
                Parent.builder()
                    .artifactIdentifier(
                        ArtifactIdentifier.builder()
                            .groupId("org.apache.maven")
                            .artifactId("maven-parent")
                            .build())
                    .version("44")
                    .integrity(
                        "sha512:806b8a36939ed7b6f81770ef48648a7bde6c4f87bd0cd73b8ffce0fec317ab3adf40617d91b840c40c58c2a2fd448f696032a60da845256661d28986e4fa055e")
                    .build(),
                Parent.builder()
                    .artifactIdentifier(
                        ArtifactIdentifier.builder()
                            .groupId("org.apache")
                            .artifactId("apache")
                            .type("pom")
                            .build())
                    .version("34")
                    .integrity(
                        "sha512:29b34e91977d3490bca8ab046d67f71e336599b2ef3af87d757784e05d6fd2091704d3b8879e11a53fdaccd3d949dfa4d66af2db5fec0d7158e958102a79461a")
                    .build()));
    lockFilePom.write(
        LockedProject.from(
            dependencies, Optional.of(parents), Optional.of(plugins), Optional.of(extensions)));
    return lockfileAccessor;
  }

  private Validator loadXsdValidator() throws SAXException {
    final URL schemaResource = getClass().getResource("/maven-4_0_0_ext.xsd");
    SchemaFactory schemaFactory = SchemaFactory.newInstance("http://www.w3.org/XML/XMLSchema/v1.1");
    Assert.assertNotNull(schemaResource);
    Schema schema = schemaFactory.newSchema(schemaResource);
    return schema.newValidator();
  }
}
