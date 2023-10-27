package io.mvnpm.maven.locker.pom;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import com.google.common.io.Resources;
import io.mvnpm.maven.locker.Artifacts;
import io.mvnpm.maven.locker.DependenciesLockFile;
import io.mvnpm.maven.locker.DependenciesLockFileAccessor;
import io.mvnpm.maven.locker.LockedDependencies;
import io.mvnpm.maven.locker.PomMinimums;
import io.quarkus.qute.Qute;
import org.apache.maven.plugin.logging.Log;

public final class DependenciesLockFilePom implements DependenciesLockFile {

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
    try {
      URL url = Resources.getResource(this.getClass(),"pom.xml");
      String template = Resources.toString(url, StandardCharsets.UTF_8);
      final String fmted = Qute.fmt(template, makeDataModel(pomMinimums, projectDependencies));
      try (Writer writer = dependenciesLockFile.writer()) {
        writer.write(fmted);
      }
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static Map<String, Object> makeDataModel(PomMinimums pomMinimums, Artifacts artifacts) {
    Map<String, Object> dataModel = new HashMap<>();
    dataModel.put("pom", pomMinimums);
    dataModel.put("dependencies", artifacts);
    return dataModel;
  }


  @Override
  public LockedDependencies read() {
    Artifacts artifacts = Artifacts.fromArtifacts(PomLockFile.read(dependenciesLockFile.file));
    return LockedDependencies.from(artifacts, log);
  }
}
