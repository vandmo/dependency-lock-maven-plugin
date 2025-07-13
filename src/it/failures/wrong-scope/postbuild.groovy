import static java.nio.charset.StandardCharsets.UTF_8
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.containsInRelativeOrder
import static org.hamcrest.core.IsIterableContaining.hasItem

import org.apache.commons.io.FileUtils

buildLog = FileUtils.readLines(new File(basedir, "build.log"), UTF_8)
assertThat(buildLog, hasItem("[ERROR] Failed to execute goal se.vandmo:dependency-lock-maven-plugin:0-SNAPSHOT:check (default-cli) on project wrong-scope: Actual project differ from locked project -> [Help 1]"))
assertThat(buildLog, containsInRelativeOrder(
        "[ERROR] The following dependencies differ:",
        "[ERROR]   Expected se.vandmo.testing:leaf:jar:1.0:test:optional=false@sha512:98gbCti9u7jp73/lFrslV7BeOCkxeXUiRQT19E58niRlyEKu69BXPXHB/xM8HQu1os08se3xTkbhWXG5xnGMFw== but found se.vandmo.testing:leaf:jar:1.0:compile:optional=false@sha512:98gbCti9u7jp73/lFrslV7BeOCkxeXUiRQT19E58niRlyEKu69BXPXHB/xM8HQu1os08se3xTkbhWXG5xnGMFw==, wrong scope"))
