package se.vandmo.dependencylock.maven;

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

public final class DependenciesLockFilePom implements DependenciesLockFile {

  private static final Version VERSION = Configuration.VERSION_2_3_31;

  private final DependenciesLockFileAccessor dependenciesLockFile;
  private final PomMinimums pomMinimums;
  private final Log log;

  private DependenciesLockFilePom(
      DependenciesLockFileAccessor dependenciesLockFile, PomMinimums pomMinimums, Log log) {
    this.dependenciesLockFile = dependenciesLockFile;
    this.pomMinimums = pomMinimums;
    this.log = log;
  }

  public static DependenciesLockFilePom from(
      DependenciesLockFileAccessor dependenciesLockFile, PomMinimums pomMinimums, Log log) {
    return new DependenciesLockFilePom(
        requireNonNull(dependenciesLockFile), requireNonNull(pomMinimums), requireNonNull(log));
  }

  @Override
  public void write(Artifacts projectDependencies) {
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

  private static Map<String, Object> makeDataModel(PomMinimums pomMinimums, Artifacts artifacts) {
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
  public LockedDependencies read(boolean enableIntegrityChecking) {
    Artifacts artifacts = Artifacts.fromArtifacts(PomLockFile.read(dependenciesLockFile.file), enableIntegrityChecking);
    return LockedDependencies.from(artifacts, log, enableIntegrityChecking);
  }
}
