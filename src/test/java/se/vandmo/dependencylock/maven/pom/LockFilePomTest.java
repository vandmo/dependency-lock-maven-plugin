package se.vandmo.dependencylock.maven.pom;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import javax.xml.stream.XMLStreamException;

import com.ctc.wstx.stax.WstxInputFactory;
import com.ctc.wstx.stax.WstxOutputFactory;
import org.apache.maven.monitor.logging.DefaultLog;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.codehaus.stax2.XMLStreamReader2;
import org.codehaus.stax2.XMLStreamWriter2;
import org.codehaus.stax2.validation.XMLValidationSchema;
import org.codehaus.stax2.validation.XMLValidationSchemaFactory;
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
import se.vandmo.dependencylock.maven.Build;
import se.vandmo.dependencylock.maven.Dependencies;
import se.vandmo.dependencylock.maven.Dependency;
import se.vandmo.dependencylock.maven.Extension;
import se.vandmo.dependencylock.maven.Extensions;
import se.vandmo.dependencylock.maven.LockFileAccessor;
import se.vandmo.dependencylock.maven.LockedProject;
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
  public void write_generates_a_xsd_compliant_resource() throws IOException, SAXException, XMLStreamException {
    final MavenProject mavenProject = new MavenProject();
    final Log log = new DefaultLog(new ConsoleLogger());
    final LockFileAccessor lockfileAccessor =
        LockFileAccessor.fromBasedir(lockfiles, "whatever.xml");
    final LockFilePom lockFilePom =
        LockFilePom.from(lockfileAccessor, PomMinimums.from(mavenProject), log);
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
    final Build build = Build.from(plugins, extensions);
    lockFilePom.write(LockedProject.from(dependencies, build, log));
    final XMLValidationSchema schema = loadXsdSchema();
    Throwable thrownError = null;
    try {
      WstxInputFactory wstxInputFactory = new WstxInputFactory();
      XMLStreamReader2 reader = wstxInputFactory.createXMLStreamReader(lockfileAccessor.file);
      reader.validateAgainst(schema);

      WstxOutputFactory wstxOutputFactory = new WstxOutputFactory();
      XMLStreamWriter2 nullWriter = wstxOutputFactory.createXMLStreamWriter(new StringWriter(), StandardCharsets.UTF_8.name());
      nullWriter.copyEventFromReader(reader, false);

      while (reader.hasNext()) {
        reader.next();
        nullWriter.copyEventFromReader(reader, false);
      }
    } catch (XMLStreamException e) {
      thrownError = e;
    }
    Assert.assertNull("No validation error should have been thrown", thrownError);
  }

  private XMLValidationSchema loadXsdSchema() throws XMLStreamException {
    XMLValidationSchemaFactory xmlValidationSchemaFactory = XMLValidationSchemaFactory.newInstance(XMLValidationSchema.SCHEMA_ID_W3C_SCHEMA);
    final URL schemaResource = getClass().getResource("/maven-4_0_0_ext.xsd");
    Assert.assertNotNull(schemaResource);
    return xmlValidationSchemaFactory.createSchema(schemaResource);
  }
}
