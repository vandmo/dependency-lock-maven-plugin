package se.vandmo.dependencylock.maven.pom;

import static freemarker.template.TemplateExceptionHandler.RETHROW_HANDLER;
import static java.util.Objects.requireNonNull;

import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapperBuilder;
import freemarker.template.ObjectWrapper;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.Version;
import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import se.vandmo.dependencylock.maven.Build;
import se.vandmo.dependencylock.maven.Dependencies;
import se.vandmo.dependencylock.maven.Extensions;
import se.vandmo.dependencylock.maven.LockFileAccessor;
import se.vandmo.dependencylock.maven.LockedProject;
import se.vandmo.dependencylock.maven.Lockfile;
import se.vandmo.dependencylock.maven.Plugins;
import se.vandmo.dependencylock.maven.PomMinimums;

public final class LockFilePom implements Lockfile {

  private static final Version VERSION = Configuration.VERSION_2_3_31;

  private final LockFileAccessor dependenciesLockFile;
  private final PomMinimums pomMinimums;
  private final Log log;

  private LockFilePom(LockFileAccessor dependenciesLockFile, PomMinimums pomMinimums, Log log) {
    this.dependenciesLockFile = dependenciesLockFile;
    this.pomMinimums = pomMinimums;
    this.log = log;
  }

  public static LockFilePom from(
      LockFileAccessor dependenciesLockFile, PomMinimums pomMinimums, Log log) {
    return new LockFilePom(
        requireNonNull(dependenciesLockFile), requireNonNull(pomMinimums), requireNonNull(log));
  }

  @Override
  public void write(LockedProject projectDependencies) {
    Configuration cfg = createConfiguration();
    try {
      Template template = cfg.getTemplate("pom-with-plugins.ftlx");
      try (Writer writer = dependenciesLockFile.writer()) {
        template.process(makeDataModel(pomMinimums, projectDependencies), writer);
      }
    } catch (IOException | TemplateException e) {
      throw new RuntimeException(e);
    }
  }

  private static Map<String, Object> makeDataModel(
      PomMinimums pomMinimums, LockedProject lockedProject) {
    Map<String, Object> dataModel = new HashMap<>();
    dataModel.put("pom", pomMinimums);
    dataModel.put("dependencies", lockedProject.dependencies);
    final Optional<Build> build = lockedProject.build;
    if (build.isPresent()) {
      dataModel.put("extensions", build.get().extensions);
      dataModel.put("plugins", build.get().plugins);
    }
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
    final PomLockFile.Contents contents;
    try {
      contents = PomLockFile.read(dependenciesLockFile.file);
    } catch (InvalidPomLockFile e) {
      throw new MojoExecutionException(e.getMessage(), e);
    }
    Dependencies artifacts = Dependencies.fromDependencies(contents.dependencies);
    if (contents.build.isPresent()) {
      Build build =
          Build.from(
              Plugins.from(contents.build.get().plugins),
              Extensions.from(contents.build.get().extensions));
      return LockedProject.from(artifacts, build, log);
    }
    return LockedProject.from(artifacts, log);
  }
}
