package se.vandmo.dependencylock.maven;

import java.io.File;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.apache.maven.execution.MavenSession;
/** A caching decorator over a checksum calculator. */
public class CachingChecksumCalculator implements Checksum.ChecksumCalculator {

  private static final String CHECKSUM_CACHE_SESSION_KEY = "dependency-lock-maven-checksum-cache";

  private final Checksum.ChecksumCalculator delegate;
  private final ConcurrentMap<String, String> checksumCache;

  public CachingChecksumCalculator(
      final Checksum.ChecksumCalculator delegate,
      final ConcurrentMap<String, String> checksumCache) {
    this.delegate = delegate;
    this.checksumCache = checksumCache;
  }

  @Override
  public String getAlgorithmHeader() {
    return delegate.getAlgorithmHeader();
  }

  @Override
  public String calculateFor(ArtifactReference artifact, File file) {
    return checksumCache.computeIfAbsent(
        artifact.getArtifactKey(), k -> delegate.calculateFor(artifact, file));
  }

  /**
   * Registers a caching decorator over the current checksum calculator. The internal details of the
   * cache will be stored in the Maven session, allowing the cache to be used across multiple
   * project builds in a reactor (there are limitations to this, as not all projects in a reactor
   * will share a classloader, so the caching will fall back in those instances to a project scoped
   * cache).
   *
   * @param session the Maven session for cross-project builds.
   */
  public static void register(MavenSession session) {
    Checksum.ChecksumCalculator calculator = Checksum.getCalculator();
    if (!(calculator instanceof CachingChecksumCalculator)) {
      ConcurrentMap<String, String> sessionChecksumCache = null;
      try {
        sessionChecksumCache =
            (ConcurrentMap<String, String>)
                session.getSystemProperties().get(CHECKSUM_CACHE_SESSION_KEY);
      } catch (ClassCastException ignored) {
      }
      if (sessionChecksumCache == null) {
        sessionChecksumCache = new ConcurrentHashMap<>();
        session.getSystemProperties().put(CHECKSUM_CACHE_SESSION_KEY, sessionChecksumCache);
      }
      Checksum.setCalculator(new CachingChecksumCalculator(calculator, sessionChecksumCache));
    }
  }
}
