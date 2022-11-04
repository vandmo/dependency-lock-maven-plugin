import static java.nio.charset.StandardCharsets.UTF_8
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.containsInRelativeOrder
import static org.hamcrest.core.IsIterableContaining.hasItem

import org.apache.commons.io.FileUtils

buildLog = FileUtils.readLines(new File(basedir, "build.log"), UTF_8)
assertThat(buildLog, hasItem("[ERROR] Failed to execute goal se.vandmo:dependency-lock-maven-plugin:0-SNAPSHOT:check (default-cli) on project everything: Dependencies differ -> [Help 1]"))
assertThat(buildLog, containsInRelativeOrder(
        "[ERROR] Missing dependencies:",
        "[ERROR]   se.vandmo.testing:leaf:jar",
        "[ERROR] Extraneous dependencies:",
        "[ERROR]   se.vandmo.testing:another-leaf:jar:1.0:compile:optional=false",
        "[ERROR] The following dependencies differ:",
        "[ERROR]   Expected se.vandmo.testing:a-third-leaf:jar:2.0:test:optional=true@sha512:this/is/incorrect/yes7BeOCkxeXUiRQT19E58niRlyEKu69BXPXHB/xM8HQu1os08se3xTkbhWXG5xnGMFw== but found se.vandmo.testing:a-third-leaf:jar:1.0:compile:optional=false@sha512:QjAlcrqmdWL88+sTA4UWoLft4SrjCkhDlYR2nk8oGkGeUvubkGQzGDrmURXmQuEzROIb5Y02dI7/a11R5k5ZmQ==, wrong optional, scope, integrity and version"))
