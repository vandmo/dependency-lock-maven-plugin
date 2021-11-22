package se.vandmo.dependencylock.maven;

import static java.util.Objects.requireNonNull;
import static se.vandmo.dependencylock.maven.JsonUtils.readJson;
import static se.vandmo.dependencylock.maven.JsonUtils.writeJson;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.io.Writer;
import org.apache.maven.plugin.logging.Log;


public final class DependenciesLockFileJson {

  private final DependenciesLockFile dependenciesLockFile;

  private DependenciesLockFileJson(DependenciesLockFile dependenciesLockFile) {
    this.dependenciesLockFile = dependenciesLockFile;
  }

  public static DependenciesLockFileJson from(DependenciesLockFile dependenciesLockFile) {
    return new DependenciesLockFileJson(requireNonNull(dependenciesLockFile));
  }

  public LockedDependencies read(Log log) {
    JsonNode json = read();
    if (!json.isObject()) {
      throw new IllegalStateException("Expected top level type to be an object");
    }
    JsonNode dependencies = json.get("dependencies");
    if (dependencies == null || !dependencies.isArray()) {
      throw new IllegalStateException("Expected a property named 'dependencies' of type array");
    }
    return LockedDependencies.fromJson(dependencies, log);
  }

  private JsonNode read() {
    try (Reader reader = dependenciesLockFile.reader()) {
      return readJson(reader);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public void write(LockedDependencies lockedDependencies) {
    ObjectNode json = JsonNodeFactory.instance.objectNode();
    json.set("dependencies", lockedDependencies.asJson());
    try (Writer writer = dependenciesLockFile.writer()) {
      writeJson(writer, json);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public void format(Log log) {
    write(read(log));
  }

  public boolean exists() {
    return dependenciesLockFile.exists();
  }
}
