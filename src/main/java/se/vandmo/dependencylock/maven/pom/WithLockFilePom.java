package se.vandmo.dependencylock.maven.pom;

import static freemarker.template.TemplateExceptionHandler.RETHROW_HANDLER;

import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapperBuilder;
import freemarker.template.ObjectWrapper;
import freemarker.template.Version;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import se.vandmo.dependencylock.maven.PomMinimums;
import se.vandmo.dependencylock.maven.ProfiledDependencies;

class WithLockFilePom {

  private static final Version VERSION = Configuration.VERSION_2_3_31;

  final Configuration createConfiguration() {
    Configuration cfg = new Configuration(VERSION);
    cfg.setClassForTemplateLoading(WithLockFilePom.class, "");
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

  final Map<String, Object> makeDataModel(
      PomMinimums pomMinimums, ProfiledDependencies projectDependencies) {
    Map<String, Object> dataModel = new HashMap<>();
    dataModel.put("pom", pomMinimums);
    dataModel.put("dependencies", projectDependencies.getSharedDependencies());
    dataModel.put(
        "profiles",
        projectDependencies
            .profileEntries()
            .filter(entry -> !entry.isEmpty())
            .sorted(Comparator.comparing(entry -> entry.getProfile().getId()))
            .map(
                entry -> {
                  Map<String, Object> profile = new HashMap<>();
                  profile.put("id", entry.getProfile().getId());
                  profile.put("activation", entry.getProfile().getActivation());
                  profile.put("dependencies", entry.getDependencies());
                  return profile;
                })
            .collect(Collectors.toList()));
    return dataModel;
  }
}
