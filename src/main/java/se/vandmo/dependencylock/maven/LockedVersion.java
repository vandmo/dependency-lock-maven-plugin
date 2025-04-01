package se.vandmo.dependencylock.maven;

import static java.util.Objects.requireNonNull;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Objects;
import java.util.Optional;

public final class LockedVersion implements Comparable<LockedVersion> {

  public static final LockedVersion USE_MINE = new LockedVersion(Optional.empty(), true);

  public final Optional<String> version;
  public final boolean useMine;

  private LockedVersion(
      Optional<String> version,
      boolean useMine) {
    this.version = requireNonNull(version);
    this.useMine = useMine;
  }

  public static LockedVersion fromJson(JsonNode json) {
    if (json.isTextual()) {
      if (isBlank(json.textValue())) {
      throw new IllegalArgumentException("version may not be blank");
    }
      return new LockedVersion(Optional.of(json.textValue()), false);
    } else if (json.isObject()) {
      JsonNode value = json.get("use-mine");
      if (value == null || !value.isBoolean()) {
        throw new IllegalArgumentException("Illegal version "+json);
      }
      if (value.booleanValue()) {
        return new LockedVersion(Optional.empty(), true);
      } else {
        throw new IllegalArgumentException("Illegal version. 'use-mine' needs to be true");
      }
    } else {
      throw new IllegalArgumentException("Invalid value for version "+json);
    }
  }

  private static boolean isBlank(String s) {
    if (s == null) {
      return true;
    }
    return s.trim().equals("");
  }

  public static LockedVersion fromVersion(String version) {
    return new LockedVersion(Optional.of(requireNonNull(version)), false);
  }

  public JsonNode asJson() {
    if (useMine) {
      ObjectNode json = JsonNodeFactory.instance.objectNode();
      json.put("use-mine", true);
      return json;
    }
    return JsonNodeFactory.instance.textNode(version.get());
  }

  @Override
  public int compareTo(LockedVersion other) {
    return toString().compareTo(other.toString());
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("LockedVersion{");
    version.ifPresent(presentVersion -> sb.append("version=").append(presentVersion));
    if (useMine) {
      sb.append("useMine");
    }
    sb.append('}');
    return sb.toString();
  }

  public String resolveWithProjectVersion(String projectVersion) {
    if (useMine) {
      return projectVersion;
    } else {
      return version.get();
    }
  }

  @Override
  public int hashCode() {
    int hash = 7;
    hash = 83 * hash + Objects.hashCode(this.version);
    hash = 83 * hash + (this.useMine ? 1 : 0);
    return hash;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final LockedVersion other = (LockedVersion) obj;
    if (this.useMine != other.useMine) {
      return false;
    }
    if (!Objects.equals(this.version, other.version)) {
      return false;
    }
    return true;
  }

  public boolean matches(String version, String projectVersion) {
    String resolvedVersion = useMine ? projectVersion : this.version.get();
    return resolvedVersion.equals(version);
  }
}
