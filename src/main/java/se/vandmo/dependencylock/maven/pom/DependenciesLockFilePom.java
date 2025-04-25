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
import org.apache.maven.plugin.logging.Log;
import se.vandmo.dependencylock.maven.Dependencies;
import se.vandmo.dependencylock.maven.DependenciesLockFile;
import se.vandmo.dependencylock.maven.LockFileAccessor;
import se.vandmo.dependencylock.maven.LockedDependencies;
import se.vandmo.dependencylock.maven.PomMinimums;

public final class DependenciesLockFilePom implements DependenciesLockFile {

  private static final Version VERSION = Configuration.VERSION_2_3_31;

  private final LockFileAccessor dependenciesLockFile;
  private final PomMinimums pomMinimums;
  private final Log log;

  private DependenciesLockFilePom(
      LockFileAccessor dependenciesLockFile, PomMinimums pomMinimums, Log log) {
    this.dependenciesLockFile = dependenciesLockFile;
    this.pomMinimums = pomMinimums;
    this.log = log;
  }

  public static DependenciesLockFilePom from(
      LockFileAccessor dependenciesLockFile, PomMinimums pomMinimums, Log log) {
    return new DependenciesLockFilePom(
        requireNonNull(dependenciesLockFile), requireNonNull(pomMinimums), requireNonNull(log));
  }

  @Override
  public void write(Dependencies projectDependencies) {
    Configuration cfg = createConfiguration();
    try {
      Template template = cfg.getTemplate("pom.ftlx");
      try (Writer writer = dependenciesLockFile.writer()) {
        template.process(makeDataModel(pomMinimums, projectDependencies), writer);
      }
    } catch (IOException | TemplateException e) {
      throw new RuntimeException(e);
    }
  }

  private static Map<String, Object> makeDataModel(
      PomMinimums pomMinimums, Dependencies artifacts) {
    Map<String, Object> dataModel = new HashMap<>();
    dataModel.put("pom", pomMinimums);
    dataModel.put("dependencies", artifacts);
    return dataModel;
  }

  private static Configuration createConfiguration() {
    Configuration cfg = new Configuration(VERSION);
    cfg.setClassForTemplateLoading(DependenciesLockFilePom.class, "");
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
  public LockedDependencies read() {
    Dependencies artifacts =
        Dependencies.fromDependencies(PomLockFile.read(dependenciesLockFile.file).dependencies);
    return LockedDependencies.from(artifacts, log);
  }
}
