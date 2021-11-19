package se.vandmo.dependencylock.maven;

import static se.vandmo.dependencylock.maven.JsonUtils.readJson;
import static se.vandmo.dependencylock.maven.JsonUtils.writeJson;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.File;
import org.apache.maven.plugin.logging.Log;


public final class DependenciesLockFile {

  public static final String DEFAULT_FILENAME = "dependencies-lock.json";

  private final File file;

  private DependenciesLockFile(File file) {
    this.file = file;
  }

  public static DependenciesLockFile fromBasedir(File basedir, String filename) {
    return new DependenciesLockFile(new File(basedir, filename));
  }

  public LockedDependencies read(Log log) {
    JsonNode json = readJson(file);
    if (!json.isObject()) {
      throw new IllegalStateException("Expected top level type to be an object");
    }
    JsonNode dependencies = json.get("dependencies");
    if (dependencies == null || !dependencies.isArray()) {
      throw new IllegalStateException("Expected a property named 'dependencies' of type array");
    }
    return LockedDependencies.fromJson(dependencies, log);
  }

  public void write(LockedDependencies lockedDependencies) {
    ObjectNode json = JsonNodeFactory.instance.objectNode();
    json.set("dependencies", lockedDependencies.asJson());
    writeJson(file, json);
  }

  public void format(Log log) {
    write(read(log));
  }

  public boolean exists() {
    return file.exists();
  }

}
