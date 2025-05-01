package se.vandmo.dependencylock.maven.mojos;

import java.io.File;
import java.nio.file.Files;
import java.util.Collections;
import org.apache.commons.io.IOUtils;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.InvalidPluginDescriptorException;
import org.apache.maven.plugin.MavenPluginManager;
import org.apache.maven.plugin.PluginDescriptorParsingException;
import org.apache.maven.plugin.PluginResolutionException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.testing.MojoRule;
import org.apache.maven.plugin.testing.WithoutMojo;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.artifact.PluginArtifact;
import org.eclipse.aether.RepositorySystemSession;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestName;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class LockProjectHelperTest {

  @Rule public final MojoRule mojoRule = new MojoRule();

  @Rule public final TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Rule public final TestName testName = new TestName();
  
  @Before
  public void before() {
    mojoRule.getContainer().addComponent(Mockito.mock(org.eclipse.aether.RepositorySystem.class), org.eclipse.aether.RepositorySystem.class, null);
  }

  private MavenProject mavenProject() throws Exception {
    final File tempDir = temporaryFolder.newFolder();
    Files.write(
        tempDir.toPath().resolve("pom.xml"),
        IOUtils.toByteArray(
            getClass()
                .getResource(
                    getClass().getSimpleName() + "/" + this.testName.getMethodName() + ".xml")));
    return mojoRule.readMavenProject(tempDir);
  }

  @Test
  @WithoutMojo
  public void loadPlugins_logsAWarningIfAppropriatelyManagedProfilePluginMissing()
      throws Exception {
    final MavenProject mavenProject = mavenProject();
    final Log log = Mockito.mock(Log.class);
    final MavenPluginManager mavenPluginManager = mockMavenPluginManager();
    final LockProjectHelper projectHelper =
        new LockProjectHelper(log, mavenPluginManager, mojoRule.newMavenSession(mavenProject));
    projectHelper.loadPlugins(mavenProject);
    final ArgumentCaptor<String> warningCaptor = ArgumentCaptor.forClass(String.class);
    Mockito.verify(log, Mockito.times(2)).warn(warningCaptor.capture());
    Assert.assertArrayEquals(
        "Unexpected warnings emitted",
        new String[] {
          "1 plugin(s) were found in profiles but not declared in the current build. This is not"
              + " recommended and may cause lock files to be inconsistent.",
          "   * org.apache.maven.plugins:maven-invoker-plugin:3.9.0"
        },
        warningCaptor.getAllValues().toArray());
    Mockito.verifyNoMoreInteractions(log);
  }

  private MavenPluginManager mockMavenPluginManager()
      throws PluginResolutionException,
          PluginDescriptorParsingException,
          InvalidPluginDescriptorException {
    final MavenPluginManager mavenPluginManager = Mockito.mock(MavenPluginManager.class);
    Mockito.when(
            mavenPluginManager.getPluginDescriptor(
                Mockito.any(Plugin.class),
                Mockito.anyList(),
                Mockito.any(RepositorySystemSession.class)))
        .thenAnswer(
            new Answer<PluginDescriptor>() {

              @Override
              public PluginDescriptor answer(InvocationOnMock invocation) throws Throwable {
                final Plugin plugin = invocation.getArgument(0, Plugin.class);
                final PluginDescriptor pluginDescriptor = new PluginDescriptor();
                pluginDescriptor.setGroupId(plugin.getGroupId());
                pluginDescriptor.setArtifactId(plugin.getArtifactId());
                pluginDescriptor.setVersion(plugin.getVersion());
                final DefaultArtifact pluginArtifact =
                    new DefaultArtifact(
                        plugin.getGroupId(),
                        plugin.getArtifactId(),
                        plugin.getVersion(),
                        null,
                        "maven-plugin",
                        null,
                        new DefaultArtifactHandler());
                pluginArtifact.setFile(temporaryFolder.newFile());
                pluginDescriptor.setPluginArtifact(new PluginArtifact(plugin, pluginArtifact));
                pluginDescriptor.setArtifacts(
                    Collections.emptyList()); // this is a mock value we don't care here
                return pluginDescriptor;
              }
            });
    return mavenPluginManager;
  }

  @Test
  @WithoutMojo
  public void loadPlugins_logsAWarningIfProfilePluginMissing() throws Exception {
    final MavenProject mavenProject = mavenProject();
    final Log log = Mockito.mock(Log.class);
    final MavenPluginManager mavenPluginManager = mockMavenPluginManager();
    final LockProjectHelper projectHelper =
        new LockProjectHelper(log, mavenPluginManager, mojoRule.newMavenSession(mavenProject));
    projectHelper.loadPlugins(mavenProject);
    final ArgumentCaptor<String> warningCaptor = ArgumentCaptor.forClass(String.class);
    Mockito.verify(log, Mockito.times(2)).warn(warningCaptor.capture());
    Assert.assertArrayEquals(
        "Unexpected warnings emitted",
        new String[] {
          "1 plugin(s) were found in profiles but not declared in the current build. This is not"
              + " recommended and may cause lock files to be inconsistent.",
          "   * org.apache.maven.plugins:maven-invoker-plugin:3.9.0"
        },
        warningCaptor.getAllValues().toArray());
    Mockito.verifyNoMoreInteractions(log);
  }

  @Test
  @WithoutMojo
  public void loadPlugins_logsAWarningIfProfilePluginVersionManagedInProfile() throws Exception {
    final MavenProject mavenProject = mavenProject();
    final Log log = Mockito.mock(Log.class);
    final MavenPluginManager mavenPluginManager = mockMavenPluginManager();
    final LockProjectHelper projectHelper =
        new LockProjectHelper(log, mavenPluginManager, mojoRule.newMavenSession(mavenProject));
    projectHelper.loadPlugins(mavenProject);
    final ArgumentCaptor<String> warningCaptor = ArgumentCaptor.forClass(String.class);
    Mockito.verify(log, Mockito.times(3)).warn(warningCaptor.capture());
    Assert.assertArrayEquals(
        "Unexpected warnings emitted",
        new String[] {
          "Detected version managed at profile level for plugin"
              + " org.apache.maven.plugins:maven-invoker-plugin. This is not recommended as it may"
              + " yield inconsistent behaviours.",
          "1 plugin(s) were found in profiles but not declared in the current build. This is not"
              + " recommended and may cause lock files to be inconsistent.",
          "   * org.apache.maven.plugins:maven-invoker-plugin:3.9.0"
        },
        warningCaptor.getAllValues().toArray());
    Mockito.verifyNoMoreInteractions(log);
  }

  @Test
  @WithoutMojo
  public void loadPlugins_logsNothingIfProfilePluginAppropriatelyDeclared() throws Exception {
    final MavenProject mavenProject = mavenProject();
    final Log log = Mockito.mock(Log.class);
    final MavenPluginManager mavenPluginManager = mockMavenPluginManager();
    final LockProjectHelper projectHelper =
        new LockProjectHelper(log, mavenPluginManager, mojoRule.newMavenSession(mavenProject));
    projectHelper.loadPlugins(mavenProject);
    Mockito.verifyNoInteractions(log);
  }
}
