import org.apache.commons.io.FileUtils

import static java.nio.charset.StandardCharsets.UTF_8
import static java.util.Arrays.asList
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.containsInRelativeOrder
import static org.hamcrest.core.IsIterableContaining.hasItem
import static org.junit.Assert.assertTrue

File lockFile = new File(basedir, ".dependency-lock/pom.xml");

assertTrue(lockFile.isFile());

buildLog = FileUtils.readLines(new File(basedir, "build.log"), UTF_8)
assertThat(buildLog, hasItem("[ERROR] Failed to execute goal se.vandmo:dependency-lock-maven-plugin:0-SNAPSHOT:check (default-cli) on project pom: Dependencies differ -> [Help 1]"))
assertThat(buildLog, containsInRelativeOrder(
        "[ERROR] Missing dependencies:",
        "[ERROR]   io.netty:netty-buffer:jar:LockedVersion{version=4.1.65.Final}:compile:jar",
        "[ERROR]   io.netty:netty-common:jar:LockedVersion{version=4.1.65.Final}:compile:jar",
        "[ERROR]   io.netty:netty-resolver:jar:LockedVersion{version=4.1.65.Final}:compile:jar",
        "[ERROR]   io.netty:netty-transport-native-epoll:linux-x86_64:jar:LockedVersion{version=4.1.65.Final}:compile:jar",
        "[ERROR]   io.netty:netty-transport-native-unix-common:jar:LockedVersion{version=4.1.65.Final}:compile:jar",
        "[ERROR]   io.netty:netty-transport:jar:LockedVersion{version=4.1.65.Final}:compile:jar",
        "[ERROR]   org.apache.maven.plugin-tools:maven-plugin-annotations:jar:LockedVersion{version=3.6.1}:runtime:jar",
        "[ERROR]   org.codehaus.cargo:simple-war:war:LockedVersion{version=1.9.8}:compile:war"))