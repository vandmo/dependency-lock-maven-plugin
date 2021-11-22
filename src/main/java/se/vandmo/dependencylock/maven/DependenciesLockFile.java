package se.vandmo.dependencylock.maven;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.io.Writer;


public final class DependenciesLockFile {

  private final File file;

  private DependenciesLockFile(File file) {
    this.file = file;
  }

  public static DependenciesLockFile fromBasedir(File basedir, String filename) {
    return new DependenciesLockFile(new File(basedir, filename));
  }

  public Reader reader() {
    try {
      return new FileReader(file);
    } catch (FileNotFoundException e) {
      throw new UncheckedIOException(e);
    }
  }

  public Writer writer() {
    file.getParentFile().mkdirs();
    try {
      return new FileWriter(file);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public boolean exists() {
    return file.exists();
  }

  public String filename() {
    return file.getAbsolutePath();
  }
}
