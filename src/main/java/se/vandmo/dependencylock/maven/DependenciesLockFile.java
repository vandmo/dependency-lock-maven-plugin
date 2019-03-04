package se.vandmo.dependencylock.maven;

import static se.vandmo.dependencylock.maven.JsonUtils.readJson;
import static se.vandmo.dependencylock.maven.JsonUtils.writeJson;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.File;


public final class DependenciesLockFile {

  private final File file;

  private DependenciesLockFile(File file) {
    this.file = file;
  }

  public static DependenciesLockFile fromBasedir(File basedir) {
    return new DependenciesLockFile(new File(basedir, "dependencies-lock.json"));
  }

  public LockedDependencies read() {
    JsonNode json = readJson(file);
    if (!json.isObject()) {
      throw new IllegalStateException("Expected top level type to be an object");
    }
    JsonNode dependencies = json.get("dependencies");
    if (dependencies == null || !dependencies.isArray()) {
      throw new IllegalStateException("Expected a property named 'dependencies' of type array");
    }
    return LockedDependencies.fromJson(dependencies);
  }

  public void write(LockedDependencies lockedDependencies) {
    ObjectNode json = JsonNodeFactory.instance.objectNode();
    json.set("dependencies", lockedDependencies.asJson());
    writeJson(file, json);
  }

  public void format() {
    write(read());
  }

  public boolean exists() {
    return file.exists();
  }

}
