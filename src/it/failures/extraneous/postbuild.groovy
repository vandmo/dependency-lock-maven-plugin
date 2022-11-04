import static java.nio.charset.StandardCharsets.UTF_8
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.containsInRelativeOrder
import static org.hamcrest.core.IsIterableContaining.hasItem

import org.apache.commons.io.FileUtils

buildLog = FileUtils.readLines(new File(basedir, "build.log"), UTF_8)
assertThat(buildLog, hasItem("[ERROR] Failed to execute goal se.vandmo:dependency-lock-maven-plugin:0-SNAPSHOT:check (default-cli) on project extraneous: Dependencies differ -> [Help 1]"))
assertThat(buildLog, containsInRelativeOrder(
        "[ERROR] Extraneous dependencies:",
        "[ERROR]   se.vandmo.testing:leaf:jar:1.0:compile:optional=false"))
