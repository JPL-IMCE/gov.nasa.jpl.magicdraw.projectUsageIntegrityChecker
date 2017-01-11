import java.io.{File, FileOutputStream}

import sbt.Keys._
import sbt._

import scala.xml._

import scala.Double
import scala.language.postfixOps
import scala.math._
import java.nio.charset.StandardCharsets
import scala.Console

import com.typesafe.sbt.pgp.PgpKeys

import gov.nasa.jpl.imce.sbt._

useGpg := true

shellPrompt in ThisBuild := { state => Project.extract(state).currentRef.project + "> " }

lazy val mdInstallDirectory = SettingKey[File]("md-install-directory", "MagicDraw Installation Directory")

mdInstallDirectory in Global := baseDirectory.value / "target" / "md.package"

lazy val artifactZipFile = taskKey[File]("Location of the zip artifact file")

lazy val updateInstall = TaskKey[Unit]("update-install", "Update the MD Installation directory")

lazy val md5Install = TaskKey[Unit]("md5-install", "Produce an MD5 report of the MD Installation directory")

lazy val zipInstall = TaskKey[File]("zip-install", "Zip the MD Installation directory")

def nativeUnzip(f: File, dir: File): Unit = {
  IO.createDirectory(dir)
  Process(Seq("unzip", "-q", f.getAbsolutePath, "-d", dir.getAbsolutePath), dir).! match {
    case 0 => ()
    case n => sys.error("Failed to run native unzip application!")
  }
}

def singleMatch(up: UpdateReport, f: DependencyFilter): File = {
  val files: Seq[File] = up.matching(f)
  require(1 == files.size)
  files.head
}

lazy val puic = Project("projectUsageIntegrityChecker", file("."))
  .enablePlugins(IMCEGitPlugin)
  .enablePlugins(IMCEReleasePlugin)
  .settings(IMCEReleasePlugin.libraryReleaseProcessSettings)
  .settings(IMCEPlugin.strictScalacFatalWarningsSettings)
  .settings(
    IMCEKeys.licenseYearOrRange := "2013-2016",
    IMCEKeys.organizationInfo := IMCEPlugin.Organizations.cae,
    IMCEKeys.targetJDK := IMCEKeys.jdk18.value,
    git.baseVersion := Versions.version,

    projectID := {
      val previous = projectID.value
      previous.extra(
        "build.date.utc" -> buildUTCDate.value,
        "artifact.kind" -> "magicdraw.resource.plugin")
    },

    // disable using the Scala version in output paths and artifacts
    crossPaths := false,

    artifactZipFile := {
      baseDirectory.value / "target" / s"imce_md18_0_sp6_puic-${version.value}-resource.zip"
    },

    addArtifact(
      Artifact("imce_md18_0_sp6_puic_resource", "zip", "zip", Some("resource"), Seq(), None, Map()),
      artifactZipFile),


    resourceDirectory in Compile := baseDirectory.value / "resources",
    javaSource in Compile := baseDirectory.value / "src",
    classDirectory in Compile := baseDirectory.value / "bin",
    cleanFiles += (classDirectory in Compile).value,


//    libraryDependencies ++= Seq(
//
//      "org.omg.tiwg.vendor.nomagic"
//        % "com.nomagic.magicdraw.package"
//        % "18.0-sp6" artifacts Artifact("com.nomagic.magicdraw.package", "zip", "zip", Some("part1"), Seq(), None, Map()),
//
//      "org.omg.tiwg.vendor.nomagic"
//        % "com.nomagic.magicdraw.package"
//        % "18.0-sp6" artifacts Artifact("com.nomagic.magicdraw.package", "zip", "zip", Some("part2"), Seq(), None, Map())
//    ),

    libraryDependencies +=
      "org.omg.tiwg.vendor.nomagic"
        % "com.nomagic.magicdraw.package"
        % "18.0-sp6.2"
        artifacts
      Artifact("com.nomagic.magicdraw.package", "pom", "pom", None, Seq(), None, Map()),

   extractArchives := {
      val base = baseDirectory.value
      val up = update.value
      val s = streams.value
      val showDownloadProgress = true // does not compile: logLevel.value <= Level.Debug

      val mdInstallDir = base / "target" / "md.package"
      if (!mdInstallDir.exists) {

        val libDir = mdInstallDir / "lib"

        IO.createDirectory(mdInstallDir)

        val tfilter: DependencyFilter = new DependencyFilter {
          def apply(c: String, m: ModuleID, a: Artifact): Boolean =
            a.extension == "pom" &&
              m.organization.startsWith("org.omg.tiwg.vendor.nomagic") &&
              m.name.startsWith("com.nomagic.magicdraw.package")
        }

        up
          .matching(tfilter)
          .headOption
          .fold[Unit]{
          s.log.warn("No MagicDraw POM artifact found!")
        }{ pom =>
          // Use unzipURL to download & extract
          //val files = IO.unzip(zip, mdInstallDir)
          val mdNoInstallZipDownloadURL = new URL(((XML.load(pom.absolutePath) \\ "properties") \ "md.core").text)

          s.log.info(
            s"=> found: ${pom.getName} at $mdNoInstallZipDownloadURL")

          // Get the credentials based on host
          credentials
            .value
            .flatMap {
              case dc: DirectCredentials if dc.host.equals(mdNoInstallZipDownloadURL.getHost) =>
                Some(dc)
              case _ =>
                None
            }
            .headOption
            .fold[Unit] {
            s.log.error(
              s"=> failed to get credentials for downloading MagicDraw no_install zip"
            )
          } { mdCredentials =>

            // 1. If no credentials are found, attempt a connection without basic authorization
            // 2. If username and password cannot be extracted (e.g., unsupported FileCredentials),
            //    then throw error
            // 3. If authorization wrong, ensure that SBT aborts

            val connection = mdNoInstallZipDownloadURL.openConnection()

            connection
              .setRequestProperty(
                  "Authorization",
                  "Basic " + java.util.Base64.getEncoder.encodeToString(
                    (mdCredentials.userName + ":" + mdCredentials.passwd)
                      .getBytes(StandardCharsets.UTF_8))
              )

            // Download the file into /target
            val size = connection.getContentLengthLong
            val input = connection.getInputStream
            val output = new FileOutputStream(base / "target" / "no_install.zip")

            s.log.info(s"=> Downloading $size bytes (= ${size / 1024 / 1024} MB)...")

            val bytes = new Array[Byte](1024 * 1024)
            var totalBytes: Double = 0
            Iterator
              .continually(input.read(bytes))
              .takeWhile(-1 != _)
              .foreach { read =>
                totalBytes += read
                output.write(bytes, 0, read)

                if (showDownloadProgress) {
                  Console.printf(
                    "    %.2f MB / %.2f MB (%.1f%%)\r",
                    totalBytes / 1024 / 1024,
                    size * 1.0 / 1024.0 / 1024.0,
                    (totalBytes / size) * 100)
                }
              }

            output.close()

            // Use unzipURL to download & extract
            val files = IO.unzip(base / "target" / "no_install.zip", mdInstallDir)
            s.log.info(
              s"=> created md.install.dir=$mdInstallDir with ${files.size} " +
                s"files extracted from zip located at: $mdNoInstallZipDownloadURL")
          }

        }

        val pfilter: DependencyFilter = new DependencyFilter {
          def apply(c: String, m: ModuleID, a: Artifact): Boolean =
            (a.`type` == "zip" || a.`type` == "resource") &&
              a.extension == "zip" &&
              (m.organization.startsWith("gov.nasa.jpl") || m.organization.startsWith("com.nomagic")) &&
              (m.name.startsWith("cae_md") ||
                m.name.startsWith("gov.nasa.jpl.magicdraw.projectUsageIntegrityChecker") ||
                m.name.startsWith("imce.dynamic_scripts.magicdraw.plugin") ||
                m.name.startsWith("com.nomagic.magicdraw.package"))
        }
        val ps: Seq[File] = up.matching(pfilter)
        ps.foreach { zip =>
          // Use unzipURL to download & extract
          val files = IO.unzip(zip, mdInstallDir)
          s.log.info(
            s"=> created md.install.dir=$mdInstallDir with ${files.size} " +
              s"files extracted from zip: ${zip.getName}")
        }

        val mdDynamicScriptsDir = mdInstallDir / "dynamicScripts"
        IO.createDirectory(mdDynamicScriptsDir)

        val zfilter: DependencyFilter = new DependencyFilter {
          def apply(c: String, m: ModuleID, a: Artifact): Boolean =
            (a.`type` == "zip" || a.`type` == "resource") &&
              a.extension == "zip" &&
              (m.organization.startsWith("gov.nasa.jpl") || m.organization.startsWith("org.omg.tiwg")) &&
              !(m.name.startsWith("cae_md") ||
                m.name.startsWith("gov.nasa.jpl.magicdraw.projectUsageIntegrityChecker") ||
                m.name.startsWith("imce.dynamic_scripts.magicdraw.plugin") ||
                m.name.startsWith("imce.third_party"))
        }
        val zs: Seq[File] = up.matching(zfilter)
        zs.foreach { zip =>
          val files = IO.unzip(zip, mdDynamicScriptsDir)
          s.log.info(
            s"=> extracted ${files.size} DynamicScripts files from zip: ${zip.getName}")
        }

      } else
        s.log.info(
          s"=> use existing md.install.dir=$mdInstallDir")
    },

    unmanagedJars in Compile := {
      val base = baseDirectory.value
      val up = update.value
      val s = streams.value
      val mdInstallDir = (mdInstallDirectory in ThisBuild).value

      val depJars = ((base / "lib") ** "*.jar").get.map(Attributed.blank)

      val libJars = ((mdInstallDir / "lib") ** "*.jar").get.map(Attributed.blank)
      val allJars = libJars ++ depJars

      s.log.info(s"=> Adding ${allJars.size} unmanaged jars")

      allJars
    },

    compile <<= (compile in Compile) dependsOn extractArchives,

    publish <<= publish dependsOn zipInstall,
    PgpKeys.publishSigned <<= PgpKeys.publishSigned dependsOn zipInstall,

    publishLocal <<= publishLocal dependsOn zipInstall,
    PgpKeys.publishLocalSigned <<= PgpKeys.publishLocalSigned dependsOn zipInstall,

    zipInstall <<=
      (baseDirectory, update, streams,
        mdInstallDirectory in Global,
        artifactZipFile,
        packageBin in Compile,
        packageSrc in Compile,
        packageDoc in Compile,
        makePom, buildUTCDate
        ) map {
        (base, up, s, mdInstallDir, zip, libJar, libSrc, libDoc, pom, d) =>

          import com.typesafe.sbt.packager.universal._

          val root = base / "target" / "imce_md18_0_sp6_puic"
          s.log.info(s"\n*** top: $root")

          IO.copyDirectory(base / "doc", root / "manual" / "ProjectUsageIntegrityChecker", overwrite=true, preserveLastModified=true)
          IO.copyDirectory(base / "profiles", root / "profiles/", overwrite=true, preserveLastModified=true)
          IO.copyDirectory(base / "samples", root / "samples", overwrite=true, preserveLastModified=true)

          val pluginDir = root / "plugins" / "gov.nasa.jpl.magicdraw.projectUsageIntegrityChecker"
          IO.createDirectory(pluginDir)

          IO.copyFile(base / "MDTeamworkProjectIDSuffixes.txt", pluginDir / "MDTeamworkProjectIDSuffixes.txt")
          IO.copyFile(base / "runOpenAuditTests.ant", pluginDir / "runOpenAuditTests.ant")
          IO.copyFile(base / "runOpenAuditTests.README.txt", pluginDir / "runOpenAuditTests.README.txt")

          IO.copyDirectory(base / "icons", pluginDir / "icons", overwrite=true, preserveLastModified=true)
          IO.copyDirectory(base / "lib", pluginDir / "lib", overwrite=true, preserveLastModified=true)

          IO.copyFile(libJar, pluginDir / "lib" / libJar.getName)
          IO.copyFile(libSrc, pluginDir / "lib" / libSrc.getName)
          IO.copyFile(libDoc, pluginDir / "lib" / libDoc.getName)

          val libDir = base / "projectUsageIntegrityChecker" / "lib"
          val libs = (PathFinder(libDir).*** --- libDir) pair Path.rebase(libDir, pluginDir / "lib")
          IO.copy(libs, overwrite=true, preserveLastModified=true)

          val pluginInfo =
            <plugin id="gov.nasa.jpl.magicdraw.projectUsageIntegrityChecker"
                    name="IMCE Project Usage Integrity Checker"
                    version={Versions.version} internalVersion={Versions.version + "0"}
                    provider-name="JPL"
                    class="gov.nasa.jpl.magicdraw.projectUsageIntegrity.ProjectUsageIntegrityPlugin">
              <requires>
                <api version="1.0"></api>
              </requires>
              <runtime>
                <library name="lib/commons-exec-1.1.jar"/>
                <library name="lib/jgrapht-0.8.3-jdk1.6.jar"/>
                <library name="lib/snakeyaml.jar"/>
                <library name={"lib/" + libJar.getName}/>
              </runtime>
            </plugin>

          xml.XML.save(
            filename=(pluginDir / "plugin.xml").getAbsolutePath,
            node=pluginInfo,
            enc="UTF-8")

          val resourceManager = root / "data" / "resourcemanager"
          IO.createDirectory(resourceManager)
          val resourceDescriptorFile = resourceManager / "MDR_Plugin_MagicDrawProjectUsageIntegrity_75319_descriptor.xml"
          val resourceDescriptorInfo =
            <resourceDescriptor critical="false" date={d}
                                description="IMCE Project Usage Integrity Checker Plugin"
                                group="imce Resource"
                                homePage="https://github.com/JPL-IMCE/gov.nasa.jpl.magicdraw.projectUsageIntegrityChecker"
                                id="75319"
                                mdVersionMax="higher"
                                mdVersionMin="18.0"
                                name="IMCE Project Usage Integrity Checker Plugin"
                                product="IMCE Project Usage Integrity Checker Plugin"
                                restartMagicdraw="false" type="Plugin">
              <version human={Versions.version} internal={Versions.version} resource={Versions.version + "0"}/>
              <provider email="nicolas.f.rouquette@jpl.nasa.gov"
                        homePage="https://github.com/NicolasRouquette"
                        name="imce"/>
              <edition>Reader</edition>
              <edition>Community</edition>
              <edition>Standard</edition>
              <edition>Professional Java</edition>
              <edition>Professional C++</edition>
              <edition>Professional C#</edition>
              <edition>Professional ArcStyler</edition>
              <edition>Professional EFFS ArcStyler</edition>
              <edition>OptimalJ</edition>
              <edition>Professional</edition>
              <edition>Architect</edition>
              <edition>Enterprise</edition>
              <requiredResource id="1440">
                <minVersion human="17.0" internal="169010"/>
              </requiredResource>
              <installation>
                <file from="manual/ProjectUsageIntegrityChecker/PUIC-May2013.pptx"
                      to="manual/ProjectUsageIntegrityChecker/PUIC-May2013.pptx"/>
                <file from="manual/ProjectUsageIntegrityChecker/PUICLocalMigrationSupport-Dec2014.pptx"
                      to="manual/ProjectUsageIntegrityChecker/PUICLocalMigrationSupport-Dec2014.pptx"/>

                <file from="profiles/SSCAEProjectUsageIntegrityProfile.mdzip"
                      to="profiles/SSCAEProjectUsageIntegrityProfile.mdzip"/>

                <file from="plugins/gov.nasa.jpl.magicdraw.projectUsageIntegrityChecker/plugin.xml"
                      to="plugins/gov.nasa.jpl.magicdraw.projectUsageIntegrityChecker/plugin.xml"/>

                <file from="plugins/gov.nasa.jpl.magicdraw.projectUsageIntegrityChecker/MDTeamworkProjectIDSuffixes.txt"
                        to="plugins/gov.nasa.jpl.magicdraw.projectUsageIntegrityChecker/MDTeamworkProjectIDSuffixes.txt"/>
                <file from="plugins/gov.nasa.jpl.magicdraw.projectUsageIntegrityChecker/runOpenAuditTests.ant" 
                        to="plugins/gov.nasa.jpl.magicdraw.projectUsageIntegrityChecker/runOpenAuditTests.ant"/>
                <file from="plugins/gov.nasa.jpl.magicdraw.projectUsageIntegrityChecker/runOpenAuditTests.README.txt" 
                      to="plugins/gov.nasa.jpl.magicdraw.projectUsageIntegrityChecker/runOpenAuditTests.README.txt"/>

                <file from="plugins/gov.nasa.jpl.magicdraw.projectUsageIntegrityChecker/lib/commons-exec-1.1.jar" 
                      to="plugins/gov.nasa.jpl.magicdraw.projectUsageIntegrityChecker/lib/commons-exec-1.1.jar"/>
                <file from="plugins/gov.nasa.jpl.magicdraw.projectUsageIntegrityChecker/lib/commons-exec-1.1-javadoc.jar"
                      to="plugins/gov.nasa.jpl.magicdraw.projectUsageIntegrityChecker/lib/commons-exec-1.1-javadoc.jar"/>
                <file from="plugins/gov.nasa.jpl.magicdraw.projectUsageIntegrityChecker/lib/commons-exec-1.1-sources.jar"
                      to="plugins/gov.nasa.jpl.magicdraw.projectUsageIntegrityChecker/lib/commons-exec-1.1-sources.jar"/>
                <file from="plugins/gov.nasa.jpl.magicdraw.projectUsageIntegrityChecker/lib/jgrapht-0.8.3-jdk1.6.jar" 
                      to="plugins/gov.nasa.jpl.magicdraw.projectUsageIntegrityChecker/lib/jgrapht-0.8.3-jdk1.6.jar"/>
                <file from="plugins/gov.nasa.jpl.magicdraw.projectUsageIntegrityChecker/lib/snakeyaml.jar"
                      to="plugins/gov.nasa.jpl.magicdraw.projectUsageIntegrityChecker/lib/snakeyaml.jar"/>
                <file from={"plugins/gov.nasa.jpl.magicdraw.projectUsageIntegrityChecker/lib/"+libJar.getName}
                      to={"plugins/gov.nasa.jpl.magicdraw.projectUsageIntegrityChecker/lib/"+libJar.getName}/>
                <file from={"plugins/gov.nasa.jpl.magicdraw.projectUsageIntegrityChecker/lib/"+libSrc.getName}
                      to={"plugins/gov.nasa.jpl.magicdraw.projectUsageIntegrityChecker/lib/"+libSrc.getName}/>
                <file from={"plugins/gov.nasa.jpl.magicdraw.projectUsageIntegrityChecker/lib/"+libDoc.getName}
                      to={"plugins/gov.nasa.jpl.magicdraw.projectUsageIntegrityChecker/lib/"+libDoc.getName}/>
                
                <file from="plugins/gov.nasa.jpl.magicdraw.projectUsageIntegrityChecker/icons/INCONSISTENT.png" 
                      to="plugins/gov.nasa.jpl.magicdraw.projectUsageIntegrityChecker/icons/INCONSISTENT.png"/>
                
                <file from="plugins/gov.nasa.jpl.magicdraw.projectUsageIntegrityChecker/icons/DEPRECATED.png" 
                      to="plugins/gov.nasa.jpl.magicdraw.projectUsageIntegrityChecker/icons/DEPRECATED.png"/>
                <file from="plugins/gov.nasa.jpl.magicdraw.projectUsageIntegrityChecker/icons/DEPRECATED-WARNING.png" 
                      to="plugins/gov.nasa.jpl.magicdraw.projectUsageIntegrityChecker/icons/DEPRECATED-WARNING.png"/>
                <file from="plugins/gov.nasa.jpl.magicdraw.projectUsageIntegrityChecker/icons/DEPRECATED-ERROR.png"
                      to="plugins/gov.nasa.jpl.magicdraw.projectUsageIntegrityChecker/icons/DEPRECATED-ERROR.png"/>
                
                <file from="plugins/gov.nasa.jpl.magicdraw.projectUsageIntegrityChecker/icons/INCUBATOR.png"
                      to="plugins/gov.nasa.jpl.magicdraw.projectUsageIntegrityChecker/icons/INCUBATOR.png"/>
                <file from="plugins/gov.nasa.jpl.magicdraw.projectUsageIntegrityChecker/icons/INCUBATOR-WARNING.png"
                      to="plugins/gov.nasa.jpl.magicdraw.projectUsageIntegrityChecker/icons/INCUBATOR-WARNING.png"/>
                <file from="plugins/gov.nasa.jpl.magicdraw.projectUsageIntegrityChecker/icons/INCUBATOR-ERROR.png"
                      to="plugins/gov.nasa.jpl.magicdraw.projectUsageIntegrityChecker/icons/INCUBATOR-ERROR.png"/>
                
                <file from="plugins/gov.nasa.jpl.magicdraw.projectUsageIntegrityChecker/icons/RECOMMENDED.png" 
                      to="plugins/gov.nasa.jpl.magicdraw.projectUsageIntegrityChecker/icons/RECOMMENDED.png"/>
                <file from="plugins/gov.nasa.jpl.magicdraw.projectUsageIntegrityChecker/icons/RECOMMENDED-WARNING.png" 
                      to="plugins/gov.nasa.jpl.magicdraw.projectUsageIntegrityChecker/icons/RECOMMENDED-WARNING.png"/>
                <file from="plugins/gov.nasa.jpl.magicdraw.projectUsageIntegrityChecker/icons/RECOMMENDED-ERROR.png" 
                      to="plugins/gov.nasa.jpl.magicdraw.projectUsageIntegrityChecker/icons/RECOMMENDED-ERROR.png"/>

                <file from="plugins/gov.nasa.jpl.magicdraw.projectUsageIntegrityChecker/samples/ProjectUsageIntegrityChecker/INCONSISTENT.mdzip"
                      to="plugins/gov.nasa.jpl.magicdraw.projectUsageIntegrityChecker/samples/ProjectUsageIntegrityChecker/INCONSISTENT.mdzip"/>
                <file from="plugins/gov.nasa.jpl.magicdraw.projectUsageIntegrityChecker/samples/ProjectUsageIntegrityChecker/P1_DEPRECATED.mdzip"
                      to="plugins/gov.nasa.jpl.magicdraw.projectUsageIntegrityChecker/samples/ProjectUsageIntegrityChecker/P1_DEPRECATED.mdzip"/>
                <file from="plugins/gov.nasa.jpl.magicdraw.projectUsageIntegrityChecker/samples/ProjectUsageIntegrityChecker/P2_DEPRECATED_constrains_INCUBATOR_as_ERROR.mdzip"
                      to="plugins/gov.nasa.jpl.magicdraw.projectUsageIntegrityChecker/samples/ProjectUsageIntegrityChecker/P2_DEPRECATED_constrains_INCUBATOR_as_ERROR.mdzip"/>
                <file from="plugins/gov.nasa.jpl.magicdraw.projectUsageIntegrityChecker/samples/ProjectUsageIntegrityChecker/P3_INCUBATOR.mdzip" 
                      to="plugins/gov.nasa.jpl.magicdraw.projectUsageIntegrityChecker/samples/ProjectUsageIntegrityChecker/P3_INCUBATOR.mdzip"/>
                <file from="plugins/gov.nasa.jpl.magicdraw.projectUsageIntegrityChecker/samples/ProjectUsageIntegrityChecker/P4_excludes_P1.mdzip"
                      to="plugins/gov.nasa.jpl.magicdraw.projectUsageIntegrityChecker/samples/ProjectUsageIntegrityChecker/P4_excludes_P1.mdzip"/>
                <file from="plugins/gov.nasa.jpl.magicdraw.projectUsageIntegrityChecker/samples/ProjectUsageIntegrityChecker/P5_uses_P1,P4.mdzip" 
                      to="plugins/gov.nasa.jpl.magicdraw.projectUsageIntegrityChecker/samples/ProjectUsageIntegrityChecker/P5_uses_P1,P4.mdzip"/>
                <file from="plugins/gov.nasa.jpl.magicdraw.projectUsageIntegrityChecker/samples/ProjectUsageIntegrityChecker/Supplier-Client-Example.mdzip" 
                      to="plugins/gov.nasa.jpl.magicdraw.projectUsageIntegrityChecker/samples/ProjectUsageIntegrityChecker/Supplier-Client-Example.mdzip"/>
                <file from="plugins/gov.nasa.jpl.magicdraw.projectUsageIntegrityChecker/samples/ProjectUsageIntegrityChecker/Use_P1_P3_with_P2.mdzip" 
                      to="plugins/gov.nasa.jpl.magicdraw.projectUsageIntegrityChecker/samples/ProjectUsageIntegrityChecker/Use_P1_P3_with_P2.mdzip"/>
                <file from="plugins/gov.nasa.jpl.magicdraw.projectUsageIntegrityChecker/samples/ProjectUsageIntegrityChecker/Use_P1_P3.mdzip"
                      to="plugins/gov.nasa.jpl.magicdraw.projectUsageIntegrityChecker/samples/ProjectUsageIntegrityChecker/Use_P1_P3.mdzip"/>
              </installation>
            </resourceDescriptor>

          xml.XML.save(
            filename=resourceDescriptorFile.getAbsolutePath,
            node=resourceDescriptorInfo,
            enc="UTF-8")

          val fileMappings = (root.*** --- root) pair relativeTo(root)
          ZipHelper.zipNIO(fileMappings, zip)

          s.log.info(s"\n*** Created the zip: $zip")
          zip
      }
  )
