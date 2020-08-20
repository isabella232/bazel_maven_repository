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
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.file.Files
import java.nio.file.Paths
import kramer.FetchArtifactCommand
import kramer.Kramer
import org.junit.Test

/**
 * Integration tests for [FetchArtifactCommand]. These are dependent on access to the network and
 * to the maven repositories it fetches from. It's runtime depends on latencies and throughput to
 * the above repositories. That said, from a home office internet it completes in less than 10
 * seconds on average.
 */
class FetchArtifactIntegrationTest {
  private val relativeDir = "test_workspace/src/test/kotlin"
  private val packageDir = this.javaClass.`package`!!.name.replace(".", "/")
  private val tmpDir = Files.createTempDirectory("resolution-test-")
  private val cacheDir = tmpDir.resolve("localcache")
  private val runfiles = Paths.get(System.getenv("JAVA_RUNFILES")!!)
  private val baos = ByteArrayOutputStream()
  private val fetchCommand = FetchArtifactCommand()
  private val cmd = Kramer(output = PrintStream(baos)).subcommands(fetchCommand)

  @Test fun fetchJarInsecurely() {
    val output = cmd.test(flags("com.google.guava:guava:18.0"), baos)
    assertThat(output).contains("Fetched com.google.guava:guava:18.0 insecurely in")
    fetchCommand.assertExists("com/google/guava/guava/18.0/guava-18.0.pom")
    fetchCommand.assertExists("com/google/guava/guava/18.0/maven-bundle-guava-18.0-classes.jar")
    val build = Files.readAllLines(fetchCommand.dir.resolve("BUILD.bazel")).joinToString("\n")
    assertThat(build).contains("com/google/guava/guava/18.0/maven-bundle-guava-18.0-classes.jar")
  }

  @Test fun fetchJarWithSha() {
    val output = cmd.test(flags(
      artifactSpec = "com.google.guava:guava:18.0",
      sha256 = "d664fbfc03d2e5ce9cab2a44fb01f1d0bf9dfebeccc1a473b1f9ea31f79f6f99"
    ), baos)
    assertThat(output).contains("Fetched com.google.guava:guava:18.0 in")
    assertThat(output).doesNotContain("SHA256")
    fetchCommand.assertExists("com/google/guava/guava/18.0/guava-18.0.pom")
    fetchCommand.assertExists("com/google/guava/guava/18.0/maven-bundle-guava-18.0-classes.jar")
    val build = Files.readAllLines(fetchCommand.dir.resolve("BUILD.bazel")).joinToString("\n")
    assertThat(build).contains("com/google/guava/guava/18.0/maven-bundle-guava-18.0-classes.jar")
  }

  @Test fun fetchJarInsecurelyWithSources() {
    val output = cmd.test(flags("com.google.guava:guava:18.0"), baos)
    assertThat(output).contains("Fetched com.google.guava:guava:18.0 insecurely in")
    fetchCommand.assertExists("com/google/guava/guava/18.0/guava-18.0.pom")
    fetchCommand.assertExists("com/google/guava/guava/18.0/maven-bundle-guava-18.0-classes.jar")
    val build = Files.readAllLines(fetchCommand.dir.resolve("BUILD.bazel")).joinToString("\n")
    assertThat(build).contains("com/google/guava/guava/18.0/maven-bundle-guava-18.0-classes.jar")
    assertThat(build).contains("com/google/guava/guava/18.0/guava-18.0-sources.jar")
  }

  @Test fun fetchAarInsecurely() {
    val output = cmd.test(flags("androidx.core:core:1.1.0"), baos)
    assertThat(output).contains("Fetched androidx.core:core:1.1.0 insecurely in")
    fetchCommand.assertExists("androidx/core/core/1.1.0/core-1.1.0.pom")
    fetchCommand.assertExists("androidx/core/core/1.1.0/maven-aar-core-1.1.0-classes.jar")
    fetchCommand.assertExists("AndroidManifest.xml")
    val build = Files.readAllLines(fetchCommand.dir.resolve("BUILD.bazel")).joinToString("\n")
    assertThat(build).contains("androidx/core/core/1.1.0/maven-aar-core-1.1.0-classes.jar")
    assertThat(build).contains("AndroidManifest.xml")
  }

  @Test fun fetchAarInsecurelyWithSources() {
    val output = cmd.test(flags("androidx.core:core:1.1.0"), baos)
    assertThat(output).contains("Fetched androidx.core:core:1.1.0 insecurely in")
    fetchCommand.assertExists("androidx/core/core/1.1.0/core-1.1.0.pom")
    fetchCommand.assertExists("androidx/core/core/1.1.0/maven-aar-core-1.1.0-classes.jar")
    fetchCommand.assertExists("AndroidManifest.xml")
    val build = Files.readAllLines(fetchCommand.dir.resolve("BUILD.bazel")).joinToString("\n")
    assertThat(build).contains("androidx/core/core/1.1.0/maven-aar-core-1.1.0-classes.jar")
    assertThat(build).contains("AndroidManifest.xml")
    assertThat(build).contains("androidx/core/core/1.1.0/core-1.1.0-sources.jar")
  }

  @Test fun testGooglePlay() {
    val output = cmd.test(flags("com.google.android.gms:play-services-base:17.1.0"), baos)
    assertThat(output).contains("Fetched com.google.android.gms:play-services-base:17.1.0 insecurely")
  }

  @Test fun testUnavailableArtifact() {
    val output = cmd.fail(flags(artifactSpec = "com.google.noguava:noguava:18.0"), baos)
    assertThat(output).contains("ERROR: Could not resolve com.google.noguava:noguava:18.0!")
    assertThat(output).contains("Attempted from [https://repo.maven.apache.org/maven2,")
  }

  private val FetchArtifactCommand.dir get() = workspace.resolve(fetchCommand.prefix)
  private fun FetchArtifactCommand.assertExists(path: String) {
    val file = dir.resolve(path)
    assertWithMessage("Missing file $file.").that(Files.exists(file)).isTrue()
  }

  private fun flags(
    artifactSpec: String,
    workspace: String = "workspace",
    sha256: String? = null,
    config: String? = null,
    settings: String? = null
  ): List<String> {
    val testSourceDir = "$runfiles/$relativeDir/$packageDir"
    val kramerConfig = config ?: "kramer-config.json"
    val settingsFlag =
      if (settings != null) listOf("--settings=$testSourceDir/$settings")
      else listOf()
    return settingsFlag +
        "--verbose" +
        "--verbose" +
        "--local_maven_cache=$cacheDir" +
        "--config=$testSourceDir/$kramerConfig" +
        "fetch-artifact" +
        "--workspace=$tmpDir/$workspace" +
        (if (sha256 != null) listOf("--sha256=$sha256") else listOf()) +
        artifactSpec
  }
}
