package se.vandmo.dependencylock.maven;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.io.Writer;

public final class LockFileAccessor {

  public final File file;

  private LockFileAccessor(File file) {
    this.file = file;
  }

  public static LockFileAccessor fromBasedir(File basedir, String filename) {
    return new LockFileAccessor(new File(basedir, filename));
  }

  public Reader reader() {
    try {
      return new InputStreamReader(new FileInputStream(file), UTF_8);
    } catch (FileNotFoundException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static Writer writerFor(File file) {
    file.getParentFile().mkdirs();
    try {
      return new OutputStreamWriter(new FileOutputStream(file), UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public Writer writer() {
    return writerFor(file);
  }

  public Writer writer(String firstChild, String... rest) {
    File childFile = new File(file.getParentFile(), firstChild);
    for (String child : rest) {
      childFile = new File(childFile, child);
    }
    return writerFor(childFile);
  }

  public boolean exists() {
    return file.exists();
  }

  public String filename() {
    return file.getAbsolutePath();
  }
}
