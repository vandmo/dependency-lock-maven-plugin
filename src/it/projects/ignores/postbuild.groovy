import org.apache.commons.io.FileUtils

import static java.nio.charset.StandardCharsets.UTF_8
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.containsInRelativeOrder

buildLog = FileUtils.readLines(new File(basedir, "build.log"), UTF_8)
assertThat(buildLog, containsInRelativeOrder(
        "[INFO] --- dependency-lock-maven-plugin:0-SNAPSHOT:check (default-cli) @ pom ---",
        "[INFO] Ignoring se.vandmo.testing:leaf:jar:123:compile:jar from lock file",
        "[INFO] Ignoring se.vandmo.testing:no-such-artifact:jar:1.0:compile:jar from lock file",
        "[INFO] Ignoring se.vandmo.testing:leaf:jar:1.0:compile:jar from actual dependencies",
        "[INFO] Ignoring se.vandmo.testing:with-dependency:jar:1.0:compile:jar from actual dependencies",
        "[INFO] Actual dependencies matches locked dependencies"))
