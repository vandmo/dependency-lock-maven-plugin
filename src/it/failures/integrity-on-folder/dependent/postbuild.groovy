import static java.nio.charset.StandardCharsets.UTF_8
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.containsInRelativeOrder
import static org.hamcrest.core.IsIterableContaining.hasItem

import org.apache.commons.io.FileUtils

buildLog = FileUtils.readLines(new File(basedir, "build.log"), UTF_8)
assertThat(buildLog, hasItem("[ERROR] Failed to execute goal se.vandmo:dependency-lock-maven-plugin:0-SNAPSHOT:check (check) on project dependent: Dependencies differ -> [Help 1]"))
assertThat(buildLog, containsInRelativeOrder(
        "[ERROR] The following dependencies differ:",
        "[ERROR]   Expected se.vandmo.tests:dependee:jar:0-SNAPSHOT:compile:optional=false@sha512:AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA== but found se.vandmo.tests:dependee:jar:0-SNAPSHOT:compile:optional=false@<Folder>, wrong integrity"))
