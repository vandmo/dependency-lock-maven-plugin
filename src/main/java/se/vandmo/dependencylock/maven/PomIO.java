package se.vandmo.dependencylock.maven;

import static freemarker.template.TemplateExceptionHandler.RETHROW_HANDLER;

import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.DefaultObjectWrapperBuilder;
import freemarker.template.ObjectWrapper;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.Version;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;


public final class PomIO {

  public static final Version VERSION = Configuration.VERSION_2_3_31;

  private PomIO() {}

  public static void writePom(DependenciesLockFile file, PomMinimums pomMinimums, Artifacts projectDependencies) {
    Configuration cfg = createConfiguration();
    try {
      Template template = cfg.getTemplate("pom.ftlx");
      try (Writer writer = file.writer()) {
        template.process(makeDataModel(pomMinimums, projectDependencies), writer);
      }
    } catch (IOException | TemplateException e) {
      throw new RuntimeException(e);
    }
  }

  private static Map<String, Object> makeDataModel(
      PomMinimums pomMinimums,
      Artifacts artifacts) {
    Map<String, Object> dataModel = new HashMap<>();
    dataModel.put("pom", pomMinimums);
    dataModel.put("dependencies", artifacts);
    return dataModel;
  }

  private static Configuration createConfiguration() {
    Configuration cfg = new Configuration(VERSION);
    cfg.setClassForTemplateLoading(PomIO.class, "");
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

  public static LockedDependencies readPom(DependenciesLockFile lockFile, Log log) {
    MavenXpp3Reader pomReader = new MavenXpp3Reader();
    try (Reader reader = lockFile.reader()) {
      Model pom = pomReader.read(reader);
      Artifacts artifacts = Artifacts.from(pom.getDependencies());
      return LockedDependencies.from(artifacts, log);
    } catch (IOException | XmlPullParserException e) {
      throw new RuntimeException(e);
    }
  }
}
