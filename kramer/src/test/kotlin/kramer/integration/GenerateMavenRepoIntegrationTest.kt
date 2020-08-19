/*
 * Copyright (C) 2020 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 *
 */
package kramer.integration

import com.github.ajalt.clikt.core.subcommands
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.squareup.tools.maven.resolution.Repositories.GOOGLE_ANDROID
import com.squareup.tools.maven.resolution.Repositories.MAVEN_CENTRAL
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.file.Files
import java.nio.file.Paths
import kramer.GenerateMavenRepo
import kramer.Kramer
import org.junit.After
import org.junit.Ignore
import org.junit.Test

/**
 * Integration tests for [GenerateMavenRepoCommand]. These are dependent on access to the network
 * and to the maven repositories it fetches from. It's runtime depends on latencies and throughput
 * to the above repositories. That said, from a home office internet it completes in less than 10
 * seconds on average (which includes 2 large no-cache scenarios).
 */
class GenerateMavenRepoIntegrationTest {
  private val relativeDir = "test_workspace/src/test/kotlin"
  private val packageDir = this.javaClass.`package`!!.name.replace(".", "/")
  private val tmpDir = Files.createTempDirectory("resolution-test-")
  private val cacheDir = tmpDir.resolve("localcache")
  private val runfiles = Paths.get(System.getenv("JAVA_RUNFILES")!!)
  val repoArgs = listOf(
    "--repository=${MAVEN_CENTRAL.id}|${MAVEN_CENTRAL.url}",
    "--repository=${GOOGLE_ANDROID.id}|${GOOGLE_ANDROID.url}",
    "--repository=spring_io_plugins|https://repo.spring.io/plugins-release",
    "--local_maven_cache=$cacheDir"
  )
  private val baos = ByteArrayOutputStream()
  private val mavenRepo = GenerateMavenRepo()
  private val cmd = Kramer(output = PrintStream(baos)).subcommands(mavenRepo)

  @After fun tearDown() {
    tmpDir.toFile().deleteRecursively()
    check(!Files.exists(cacheDir)) { "Failed to tear down and delete temp directory." }
  }

  @Test fun simpleResolution() {
    val args = configFlags("simple", "gen-maven-repo")
    val output = cmd.test(args, baos)
    assertThat(output).contains("Building workspace for 1 artifacts")
    assertThat(output).contains("Generated 1 build files in ")
    assertThat(output).contains("Resolved 1 artifacts with 100 threads in")
    val build = mavenRepo.readBuildFile("javax.inject")
    assertThat(build).contains("javax.inject:javax.inject:1")
    assertThat(build).contains("name = \"javax_inject\"")
    assertThat(build).contains("@javax_inject_javax_inject//maven")
  }

  @Test fun excludesSuccess() {
    val args = configFlags("excludes-success", "gen-maven-repo")
    val output = cmd.test(args, baos)
    assertThat(output).contains("Building workspace for 1 artifacts")
    assertThat(output).contains("Generated 1 build files in ")
    assertThat(output).contains("Resolved 1 artifacts with 100 threads in")
  }

  @Test fun excludesFailure() {
    val args = configFlags("excludes-failure", "gen-maven-repo")
    val output = cmd.fail(args, baos)
    assertThat(output).contains("Building workspace for 1 artifacts")
    assertThat(output).contains("Generated 1 build files in ")
    assertThat(output).contains("Resolved 1 artifacts with 100 threads in")

    assertThat(output)
      .contains("ERROR: Un-declared artifacts referenced in the dependencies of some artifacts.")
    assertThat(output)
      .contains(""""org.apache.maven:maven-builder-support:3.6.3": {"insecure": True}""")
    assertThat(output).contains(""""exclude": ["org.apache.maven:maven-builder-support"]""")
  }

  @Test fun buildSnippetOverridesUndeclared() {
    // If an artifact has a build snippet, it's deps should not contribute to the required list.
    // This config includes a build snippet but no excludes.
    val args = configFlags("build-snippet", "gen-maven-repo")
    val output = cmd.test(args, baos)
    assertThat(output).contains("Building workspace for 1 artifacts")
    assertThat(output).contains("Generated 1 build files in ")
    assertThat(output).contains("Resolved 1 artifacts with 100 threads in")
  }

  @Test fun dontPropagateOptionalDeps() {
    val args = configFlags("optional", "gen-maven-repo")
    val output = cmd.test(args, baos)
    assertThat(output).contains("Building workspace for 2 artifacts")
    assertThat(output).contains("Generated 2 build files in ")
    assertThat(output).contains("Resolved 2 artifacts with 100 threads in")
  }

  @Test fun jetifierMatch() {
    val args = configFlags("jetifier", "gen-maven-repo")
    val output = cmd.test(args, baos)
    assertThat(output).contains("Building workspace for 2 artifacts")
    assertThat(output).contains("Generated 2 build files in ")
    assertThat(output).contains("Resolved 2 artifacts with 100 threads in")

    val guava = mavenRepo.readBuildFile("com.google.guava")
    assertThat(guava).contains("jetify = True")
    val jimfs = mavenRepo.readBuildFile("com.google.jimfs")
    assertThat(jimfs).doesNotContain("jetify")
  }

  @Test fun jetifierMap() {
    val args = configFlags("jetifier-map", "gen-maven-repo")
    val output = cmd.test(args, baos)
    assertThat(output).contains("Building workspace for 2 artifacts")
    assertThat(output).contains("Generated 2 build files in ")
    assertThat(output).contains("Resolved 2 artifacts with 100 threads in")

    val picasso = mavenRepo.readBuildFile("com.squareup.picasso")
    assertThat(picasso).contains("jetify = True")
    assertThat(picasso).contains("@maven//androidx/annotation")
  }

  @Test fun jetifierMapMissingArtifact() {
    val args = configFlags("jetifier-map-missing-artifact", "gen-maven-repo")
    val output = cmd.fail(args, baos)
    assertThat(output).contains("Building workspace for 1 artifacts")
    assertThat(output).contains("Generated 1 build files in ")
    assertThat(output).contains("Resolved 1 artifacts with 100 threads in")
    assertThat(output)
      .contains("ERROR: Un-declared artifacts referenced in the dependencies of some artifacts.")

    // Picasso declares dep on com.android.support:support-annotation. Want to see androidx here.
    assertThat(output).contains("androidx.annotation:annotation:SOME_VERSION")
  }

  @Test fun jetifierPreAndroidXArtifact() {
    val args = configFlags("android-support", "gen-maven-repo")
    val output = cmd.fail(args, baos)
    assertThat(output).contains("Building workspace for 3 artifacts")
    assertThat(output).contains("Generated 3 build files in ")
    assertThat(output).contains("Resolved 3 artifacts with 100 threads in")

    assertThat(output)
      .contains("ERROR: Jetifier enabled but pre-androidX support artifacts specified:")
    assertThat(output)
      .contains(
        "com.android.support:support-annotations (should be androidx.annotation:annotation)"
      )
    assertThat(output).doesNotContain("javax.inject:javax.inject")
  }

  @Test fun jetifierPreAndroidXArtifactDisabled() {
    val args = configFlags("android-support-check-disabled", "gen-maven-repo")
    val output = cmd.test(args, baos)
    assertThat(output).contains("Building workspace for 3 artifacts")
    assertThat(output).contains("Generated 3 build files in ")
    assertThat(output).contains("Resolved 3 artifacts with 100 threads in")
  }

  @Test fun largeListOfArtifacts() {
    val args = configFlags("large", "gen-maven-repo")
    val output = cmd.test(args, baos)
    assertThat(output).contains("Building workspace for 469 artifacts")
    assertThat(output).contains("Generated 230 build files in ")
    assertThat(output).contains("Resolved 469 artifacts with 100 threads in")
  }

  // This is the flakiest test design that ever flaked, but we want a sense that there is
  // a speed up with large artifact lists, and to have a canary.
  //
  // Also, it's not presently honoring the prior-cache in the test environment, so i've
  // disabled it for now. The caching works fine in normal operations, so it's unclear what's
  // happening. Leaving this here TBD
  //
  // TODO Fix this or migrate it to a more appropriate performance test enviornment.
  //
  @Ignore("This is a performance test, crazy flaky, and not quite right yet.")
  @Test fun largeListOfArtifactsWithCaching() {
    val timingMatcher = "with [0-9]* threads in ([0-9.]*) seconds".toRegex()

    val output1 = cmd.test(configFlags("large", "gen-maven-repo"), baos)
    val result1 = timingMatcher.find(output1)
    assertWithMessage("Expected to match ${timingMatcher.pattern}").that(result1).isNotNull()
    val time1 = result1!!.groupValues[1].toFloat()
    assertWithMessage("Expected non-cached first run, but run took $time1 seconds")
      .that(time1)
      .isGreaterThan(4.0f)

    assertThat(Files.exists(cacheDir.resolve("junit/junit/4.13/junit-4.13.pom"))).isTrue()
    val output2 = cmd.test(
      configFlags(
        "large",
        "gen-maven-repo",
        workspace = "workspace2",
        kramerArgs = listOf(
          "--repository=foo|localhost:0", // force fake repo for this run - all cache.
          "--local_maven_cache=$cacheDir"
        )
      ),
      baos
    )
    val result2 = timingMatcher.find(output2)
    assertWithMessage("Expected to match ${timingMatcher.pattern}").that(result2).isNotNull()
    val time2 = result2!!.groupValues[1].toFloat()
    assertWithMessage("Expected fast cache run but took $time2 seconds")
      .that(time2)
      .isLessThan(3.0f)

    assertThat(output2).contains("Resolved 469 artifacts with 100 threads in ")
  }

  private fun GenerateMavenRepo.readBuildFile(groupId: String): String {
    val groupPath = groupId.replace(".", "/")
    val workspace = workspace.toAbsolutePath()
    val buildFile = workspace.resolve(groupPath).resolve("BUILD.bazel")
    assertWithMessage("File does not exist: $buildFile").that(Files.exists(buildFile)).isTrue()
    return Files.readAllLines(buildFile).joinToString("\n")
  }

  private fun configFlags(
    label: String,
    command: String,
    threads: Int = 100,
    workspace: String = "workspace",
    kramerArgs: List<String> = repoArgs
  ) =
    kramerArgs +
      command +
      "--threads=$threads" +
      "--workspace=$tmpDir/$workspace" +
      "--configuration=$runfiles/$relativeDir/$packageDir/test-$label-config.json"
}
