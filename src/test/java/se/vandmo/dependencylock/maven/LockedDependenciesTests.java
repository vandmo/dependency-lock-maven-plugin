package se.vandmo.dependencylock.maven;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class LockedDependenciesTests {

  @Test
  public void snapshotMatch() {
    assertTrue(LockedDependencies.snapshotMatch("1.0", "1.0"));
    assertFalse(LockedDependencies.snapshotMatch("1.0", "1.0.1"));
    assertTrue(LockedDependencies.snapshotMatch("0-SNAPSHOT", "0-20221104.072032-1"));
    assertTrue(LockedDependencies.snapshotMatch("0-20221104.072032-1", "0-SNAPSHOT"));
    assertFalse(LockedDependencies.snapshotMatch("0-SNAPSHOT.1", "0-20221104.072032-1.1"));
    assertTrue(LockedDependencies.snapshotMatch("1.2.3-SNAPSHOT", "1.2.3-20221104.072032-1"));
  }

  @Test
  public void stripSnapshot() {
    assertEquals("1.0", LockedDependencies.stripSnapshot("1.0"));
    assertEquals("1.0", LockedDependencies.stripSnapshot("1.0-SNAPSHOT"));
    assertEquals("1.2.3", LockedDependencies.stripSnapshot("1.2.3-SNAPSHOT"));
    assertEquals("0", LockedDependencies.stripSnapshot("0-20221104.072032-1"));
    assertEquals("1.2.3", LockedDependencies.stripSnapshot("1.2.3-20221104.072032-1"));
    assertEquals(
        "1.2.3-20221104.072032-1-1", LockedDependencies.stripSnapshot("1.2.3-20221104.072032-1-1"));
  }
}
