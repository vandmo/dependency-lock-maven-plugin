package se.vandmo.dependencylock.maven;

import static java.util.Collections.singletonMap;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;


@Mojo(
  name = "lock",
  requiresDependencyResolution = ResolutionScope.TEST)
public final class LockMojo extends AbstractMojo {

  @Parameter(
    defaultValue = "${basedir}",
    required = true,
    readonly = true)
  private File baseFolder;

  @Parameter(
    defaultValue="${project}",
    required = true,
    readonly = true)
  private MavenProject project;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    try {
      new ObjectMapper()
          .writerWithDefaultPrettyPrinter()
          .writeValue(new File(baseFolder, "dependencies-lock.json"), asJson(project.getArtifacts()));
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  private Map<String, Object> asJson(Set<Artifact> artifacts) {
    List<Map<String, String>> list = new ArrayList<>();
    artifacts.stream().sorted().forEach(artifact -> list.add(asJson(artifact)));
    return singletonMap("dependencies", list);
  }

  private Map<String, String> asJson(Artifact artifact) {
    Map<String, String> m = new LinkedHashMap<>();
    putIfNotNull(m, "groupId", artifact.getGroupId());
    putIfNotNull(m, "artifactId", artifact.getArtifactId());
    putIfNotNull(m, "version", artifact.getVersion());
    putIfNotNull(m, "scope", artifact.getScope());
    putIfNotNull(m, "type", artifact.getType());
    putIfNotNull(m, "classifier", artifact.getClassifier());
    return m;
  }

  private void putIfNotNull(Map<String, String> m, String name, String value) {
    if (value != null) {
      m.put(name, value);
    }
  }

}
