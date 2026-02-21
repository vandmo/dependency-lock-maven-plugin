package se.vandmo.dependencylock.maven.pom;

import static freemarker.template.TemplateExceptionHandler.RETHROW_HANDLER;
import static java.lang.String.format;
import static java.util.Locale.ROOT;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapperBuilder;
import freemarker.template.ObjectWrapper;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.Version;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.maven.plugin.MojoExecutionException;
import se.vandmo.dependencylock.maven.Dependencies;
import se.vandmo.dependencylock.maven.Extensions;
import se.vandmo.dependencylock.maven.LockFileAccessor;
import se.vandmo.dependencylock.maven.LockedProject;
import se.vandmo.dependencylock.maven.Lockfile;
import se.vandmo.dependencylock.maven.Parent;
import se.vandmo.dependencylock.maven.Parents;
import se.vandmo.dependencylock.maven.Plugins;
import se.vandmo.dependencylock.maven.PomMinimums;
import se.vandmo.dependencylock.maven.Profiled;

public final class LockFilePom implements Lockfile {

  private static final Version VERSION = Configuration.VERSION_2_3_31;

  private final LockFileAccessor dependenciesLockFile;
  private final PomMinimums pomMinimums;

  private LockFilePom(LockFileAccessor dependenciesLockFile, PomMinimums pomMinimums) {
    this.dependenciesLockFile = dependenciesLockFile;
    this.pomMinimums = pomMinimums;
  }

  public static LockFilePom from(LockFileAccessor dependenciesLockFile, PomMinimums pomMinimums) {
    return new LockFilePom(requireNonNull(dependenciesLockFile), requireNonNull(pomMinimums));
  }

  private void writeFromTemplate(Configuration cfg, Map<String, Object> dataModel, String name)
      throws IOException, TemplateException {
    Template template = cfg.getTemplate(format(ROOT, "%s-pom.ftlx", name));
    try (Writer writer = dependenciesLockFile.writer(name, "pom.xml")) {
      template.process(dataModel, writer);
    }
  }

  @Override
  public void write(LockedProject projectDependencies) {
    Configuration cfg = createConfiguration();
    try {
      Map<String, Object> dataModel = makeDataModel(pomMinimums, projectDependencies);
      Template template = cfg.getTemplate("pom.ftlx");
      try (Writer writer = dependenciesLockFile.writer()) {
        template.process(dataModel, writer);
      }
      writeFromTemplate(cfg, dataModel, "extensions");
      writeFromTemplate(cfg, dataModel, "parents");
      writeFromTemplate(cfg, dataModel, "plugins");
    } catch (IOException | TemplateException e) {
      throw new RuntimeException(e);
    }
  }

  private static Map<String, Object> makeDataModel(
      PomMinimums pomMinimums, LockedProject lockedProject) {
    Map<String, Object> dataModel = new HashMap<>();
    dataModel.put("pom", pomMinimums);
    dataModel.put("dependencies", lockedProject.dependencies.getDefaultEntities());
    lockedProject.parents.ifPresent(parents -> dataModel.put("parents", parents));
    lockedProject.plugins.ifPresent(plugins -> dataModel.put("plugins", plugins));
    lockedProject.extensions.ifPresent(extensions -> dataModel.put("extensions", extensions));
    dataModel.put(
        "profiles",
        lockedProject
            .dependencies
            .profileEntries()
            .map(
                entry -> {
                  Map<String, Object> result = new HashMap<>();
                  result.put("id", entry.getKey());
                  result.put("dependencies", entry.getValue());
                  return result;
                })
            .collect(toList()));
    return dataModel;
  }

  private static Configuration createConfiguration() {
    Configuration cfg = new Configuration(VERSION);
    cfg.setClassForTemplateLoading(LockFilePom.class, "");
    cfg.setDefaultEncoding("UTF-8");
    cfg.setTemplateExceptionHandler(RETHROW_HANDLER);
    cfg.setLogTemplateExceptions(false);
    cfg.setFallbackOnNullLoopVariable(false);
    cfg.setObjectWrapper(getObjectWrapper());
    return cfg;
  }

  private static ObjectWrapper getObjectWrapper() {
    DefaultObjectWrapperBuilder builder = new DefaultObjectWrapperBuilder(VERSION);
    builder.setExposeFields(true);
    builder.setIterableSupport(true);
    return builder.build();
  }

  @Override
  public LockedProject read() throws MojoExecutionException {
    final PomLockFile.Contents contents = readContents(dependenciesLockFile.file, true);
    Dependencies dependencies =
        Dependencies.fromDependencies(
            contents.dependencies.orElseThrow(
                () -> new InvalidPomLockFile("Missing 'dependencies' element")));
    Map<String, Dependencies> profiled =
        contents
            .profiles
            .map(
                deps ->
                    deps.entrySet().stream()
                        .collect(
                            Collectors.toMap(
                                entry -> entry.getKey(),
                                entry -> Dependencies.fromDependencies(entry.getValue()))))
            .orElse(Collections.emptyMap());
    Optional<Parents> parents =
        maybeReadContents(dependenciesLockFile.sibling("parents", "pom.xml"), false)
            .map(
                parentsContent ->
                    new Parents(
                        parentsContent
                            .dependencies
                            .orElseThrow(
                                () ->
                                    new InvalidPomLockFile(
                                        "Missing 'dependencies' element in parents lock file"))
                            .stream()
                            .map(
                                parent -> {
                                  if (parent.optional) {
                                    throw new InvalidPomLockFile("Parent should not be optional");
                                  }
                                  if (!parent.getArtifactIdentifier().type.equals("pom")) {
                                    throw new InvalidPomLockFile("Parent should be a pom");
                                  }
                                  return Parent.builder()
                                      .artifactIdentifier(parent.getArtifactIdentifier())
                                      .version(parent.getVersion())
                                      .integrity(parent.getIntegrity())
                                      .build();
                                })
                            .collect(toList())));
    Optional<Plugins> plugins =
        maybeReadContents(dependenciesLockFile.sibling("plugins", "pom.xml"), true)
            .map(
                pluginsContent ->
                    pluginsContent
                        .plugins
                        .map(pluginsInContent -> Plugins.from(pluginsInContent))
                        .orElseThrow(
                            () ->
                                new InvalidPomLockFile(
                                    "Missing plugins section in plugins lock file")));
    Optional<Extensions> extensions =
        maybeReadContents(dependenciesLockFile.sibling("extensions", "pom.xml"), true)
            .map(
                extensionsContent ->
                    extensionsContent
                        .extensions
                        .map(extensionsInContent -> Extensions.from(extensionsInContent))
                        .orElseThrow(
                            () ->
                                new InvalidPomLockFile(
                                    "Missing extensions section in extensions lock file")));
    return LockedProject.from(new Profiled<>(dependencies, profiled), parents, plugins, extensions);
  }

  private Optional<PomLockFile.Contents> maybeReadContents(File file, boolean requireScope)
      throws MojoExecutionException {
    if (file.isFile()) {
      return Optional.of(readContents(file, requireScope));
    }
    return Optional.empty();
  }

  private PomLockFile.Contents readContents(File file, boolean requireScope)
      throws MojoExecutionException {
    try {
      return PomLockFile.read(file, requireScope);
    } catch (InvalidPomLockFile e) {
      throw new MojoExecutionException(e.getMessage(), e);
    }
  }
}
