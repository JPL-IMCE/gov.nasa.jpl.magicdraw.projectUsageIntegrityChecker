import better.files.{File => BFile, _}
import java.io.File
import java.nio.file.Files

import sbt.Keys._
import sbt._

import com.typesafe.sbt.pgp.PgpKeys
import com.typesafe.sbt.SbtAspectj._
import com.typesafe.sbt.SbtAspectj.AspectjKeys._

import scala.xml.{Node => XNode}
import scala.xml.transform._

import scala.collection.JavaConversions._

import gov.nasa.jpl.imce.sbt._

useGpg := true

developers := List(
  Developer(
    id="rouquett",
    name="Nicolas F. Rouquette",
    email="nicolas.f.rouquette@jpl.nasa.gov",
    url=url("https://gateway.jpl.nasa.gov/personal/rouquett/default.aspx")),
  Developer(
    id="kerzhmer",
    name="Aleksandr A. Kerzhner",
    email="aleksandr.a.kerzhner@jpl.nasa.gov",
    url=url("https://gateway.jpl.nasa.gov/personal/kerzhner/default.aspx")),
  Developer(
    id="khavelun",
    name="Klaus Havelund",
    email="klaus.havelund@jpl.nasa.gov",
    url=url("https://gateway.jpl.nasa.gov/personal/khavelun/default.aspx")))

shellPrompt in ThisBuild := { state => Project.extract(state).currentRef.project + "> " }

cleanFiles <+=
  baseDirectory { base => base / "cae.md.package" }

lazy val mdInstallDirectory = SettingKey[File]("md-install-directory", "MagicDraw Installation Directory")

mdInstallDirectory in Global :=
  baseDirectory.value / "cae.md.package" / ("cae.md18_0sp5.puic-" + Versions.version)

lazy val artifactZipFile = taskKey[File]("Location of the zip artifact file")

lazy val extractArchives = TaskKey[Seq[Attributed[File]]]("extract-archives", "Extracts ZIP files")

lazy val updateInstall = TaskKey[Unit]("update-install", "Update the MD Installation directory")

lazy val md5Install = TaskKey[Unit]("md5-install", "Produce an MD5 report of the MD Installation directory")

lazy val zipInstall = TaskKey[File]("zip-install", "Zip the MD Installation directory")

lazy val buildUTCDate = SettingKey[String]("build-utc-date", "The UDC Date of the build")

buildUTCDate in Global := {
  import java.util.{ Date, TimeZone }
  val formatter = new java.text.SimpleDateFormat("yyyy-MM-dd-HH:mm")
  formatter.setTimeZone(TimeZone.getTimeZone("UTC"))
  formatter.format(new Date)
}

lazy val puic = Project("projectUsageIntegrityChecker", file("projectUsageIntegrityChecker"))
  .enablePlugins(IMCEGitPlugin)
  .enablePlugins(IMCEReleasePlugin)
  .settings(IMCEReleasePlugin.libraryReleaseProcessSettings)
  .settings(addArtifact(Artifact("cae_md18_0_sp5_puic_resource", "zip", "zip"), artifactZipFile).settings: _*)
  .settings(
    IMCEKeys.licenseYearOrRange := "2013-2016",
    IMCEKeys.organizationInfo := IMCEPlugin.Organizations.cae,
    IMCEKeys.targetJDK := IMCEKeys.jdk18.value,
    git.baseVersion := Versions.version,

    organization := "gov.nasa.jpl.cae.magicdraw.plugins",
    name := "cae_md18_0_sp5_puic",
    homepage := Some(url("https://github.jpl.nasa.gov/secae/gov.nasa.jpl.magicdraw.projectUsageIntegrityChecker")),

    projectID := {
      val previous = projectID.value
      previous.extra("build.date.utc" -> buildUTCDate.value)
    },

    artifactZipFile := {
      baseDirectory.value / "target" / "cae_md18_0_sp5_puic_resource.zip"
    },

    addArtifact(Artifact("cae_md18_0_sp5_puic_resource", "zip", "zip"), artifactZipFile),

    resourceDirectory in Compile := baseDirectory.value / "resources",
    javaSource in Compile := baseDirectory.value / "src",
    classDirectory in Compile := baseDirectory.value / "bin",
    cleanFiles += (classDirectory in Compile).value,
    libraryDependencies +=
      "gov.nasa.jpl.cae.magicdraw.packages" %% "cae_md18_0_sp5_aspectj_scala" % Versions.aspectj_scala_package %
        "compile" artifacts Artifact("cae_md18_0_sp5_aspectj_scala", "zip", "zip"),

    extractArchives <<= (baseDirectory, libraryDependencies, update, streams,
      mdInstallDirectory in Global, scalaBinaryVersion) map {
      (base, libs, up, s, mdInstallDir, sbV) =>

        if (!mdInstallDir.exists) {

          libs.foreach { dep: ModuleID =>
            s.log.info(s"direct: $dep")
          }
          val filter: DependencyFilter = new DependencyFilter {
            def apply(c: String, m: ModuleID, a: Artifact): Boolean = {
              val ok1 = a.`type` == "zip" && a.extension == "zip"
              val ok2 = libs.find { dep: ModuleID =>
                ok1 && dep.organization == m.organization && m.name == dep.name + "_" + sbV
              }
              ok2.isDefined
            }
          }
          val zips: Seq[File] = up.matching(filter)
          zips.foreach { zip =>
            val files = IO.unzip(zip, mdInstallDir)
            s.log.info(
              s"=> created md.install.dir=$mdInstallDir with ${files.size} " +
                s"files extracted from zip: ${zip.getName}")
          }
          val mdRootFolder = mdInstallDir / s"cae.md18_0sp5.aspectj_scala-${Versions.aspectj_scala_package}"
          require(
            mdRootFolder.exists && mdRootFolder.canWrite,
            s"mdRootFolder: $mdRootFolder")
          IO.listFiles(mdRootFolder).foreach { f =>
            val fp = f.toPath
            Files.move(
              fp,
              mdInstallDir.toPath.resolve(fp.getFileName),
              java.nio.file.StandardCopyOption.REPLACE_EXISTING)
          }
          IO.delete(mdRootFolder)

          val mdBinFolder = mdInstallDir / "bin"
          require(mdBinFolder.exists, "md bin: $mdBinFolder")
          val mdPropertiesFiles: Seq[File] = mdBinFolder.listFiles(new java.io.FilenameFilter() {
            override def accept(dir: File, name: String): Boolean =
              name.endsWith(".properties")
          })

          mdPropertiesFiles.foreach { mdPropertyFile: File =>

            val mdPropertyName = mdPropertyFile.name
            val unpatchedContents: String = IO.read(mdPropertyFile)

            // Remove "-Dlocal.config.dir.ext\=<value>" or "-Dlocal.config.dir.ext=<value>" regardless of what <value> is.
            val patchedContents1 = unpatchedContents.replaceAll(
              "-Dlocal.config.dir.ext\\\\?=[a-zA-Z0-9_.\\\\-]*",
              "-Dlocal.config.dir.ext\\\\=-puic-" + Versions.version)

            IO.write(file = mdPropertyFile, content = patchedContents1, append = false)
          }

        } else {
          s.log.info(
            s"=> use existing md.install.dir=$mdInstallDir")
        }

        val libPath = (mdInstallDir / "lib").toPath
        val mdJars = for {
          jar <- Files.walk(libPath).iterator().filter(_.toString.endsWith(".jar")).map(_.toFile)
        } yield Attributed.blank(jar)

        mdJars.toSeq
    },

    unmanagedJars in Compile <++= extractArchives,

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

          val root = base / "target" / "cae_md18_0_sp5_puic"
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
                    name="CAE Project Usage Integrity Checker"
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
                                description="CAE Project Usage Integrity Checker Plugin"
                                group="CAE Resource"
                                homePage="https://github.jpl.nasa.gov/secae/gov.nasa.jpl.magicdraw.projectUsageIntegrityChecker"
                                id="75319"
                                mdVersionMax="higher"
                                mdVersionMin="18.0"
                                name="CAE Project Usage Integrity Checker Plugin"
                                product="CAE Project Usage Integrity Checker Plugin"
                                restartMagicdraw="false" type="Plugin">
              <version human={Versions.version} internal={Versions.version} resource={Versions.version + "0"}/>
              <provider email="nicolas.f.rouquette@jpl.nasa.gov"
                        homePage="https://github.jpl.nasa.gov/secae/gov.nasa.jpl.magicdraw.projectUsageIntegrityChecker"
                        name="CAE"/>
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
                <file from="plugins/gov.nasa.jpl.magicdraw.projectUsageIntegrityChecker/lib/jgrapht-0.8.3-jdk1.6.jar" 
                      to="plugins/gov.nasa.jpl.magicdraw.projectUsageIntegrityChecker/lib/jgrapht-0.8.3-jdk1.6.jar"/>
                <file from="plugins/gov.nasa.jpl.magicdraw.projectUsageIntegrityChecker/lib/snakeyaml.jar"
                      to="plugins/gov.nasa.jpl.magicdraw.projectUsageIntegrityChecker/lib/snakeyaml.jar"/>
                <file from={"plugins/gov.nasa.jpl.magicdraw.projectUsageIntegrityChecker/lib/"+libJar.getName}
                      to={"plugins/gov.nasa.jpl.magicdraw.projectUsageIntegrityChecker/lib/"+libJar.getName}/>
                
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
  .settings(IMCEPlugin.strictScalacFatalWarningsSettings)

lazy val root = Project("puic-package", file("."))
  .enablePlugins(IMCEGitPlugin)
  .enablePlugins(IMCEReleasePlugin)
  .aggregate(puic)
  .dependsOn(puic)
  .settings(addArtifact(Artifact("cae_md18_0_sp5_puic", "zip", "zip"), artifactZipFile).settings: _*)
  .settings(
    IMCEKeys.licenseYearOrRange := "2013-2016",
    IMCEKeys.organizationInfo := IMCEPlugin.Organizations.cae,
    IMCEKeys.targetJDK := IMCEKeys.jdk18.value,
    git.baseVersion := Versions.version,
    
    organization := "gov.nasa.jpl.cae.magicdraw.packages",
    name := "cae_md18_0_sp5_puic",
    homepage := Some(url("https://github.jpl.nasa.gov/secae/gov.nasa.jpl.magicdraw.projectUsageIntegrityChecker")),

    projectID := {
      val previous = projectID.value
      previous.extra("build.date.utc" -> buildUTCDate.value)
    },

    pomPostProcess <<= (pomPostProcess, mdInstallDirectory in Global) {
      (previousPostProcess, mdInstallDir) => { (node: XNode) =>
        val processedNode: XNode = previousPostProcess(node)
        val mdUpdateDir = UpdateProperties(mdInstallDir)
        val resultNode: XNode = new RuleTransformer(mdUpdateDir)(processedNode)
        resultNode
      }
    },

    artifactZipFile := {
      baseDirectory.value / "target" / "cae_md18_0_sp5_puic.zip"
    },

    addArtifact(Artifact("cae_md18_0_sp5_puic", "zip", "zip"), artifactZipFile),

    // disable publishing the main jar produced by `package`
    publishArtifact in(Compile, packageBin) := false,

    // disable publishing the main API jar
    publishArtifact in(Compile, packageDoc) := false,

    // disable publishing the main sources jar
    publishArtifact in(Compile, packageSrc) := false,

    // disable publishing the jar produced by `test:package`
    publishArtifact in(Test, packageBin) := false,

    // disable publishing the test API jar
    publishArtifact in(Test, packageDoc) := false,

    // disable publishing the test sources jar
    publishArtifact in(Test, packageSrc) := false,

    publish <<= publish dependsOn zipInstall,
    PgpKeys.publishSigned <<= PgpKeys.publishSigned dependsOn zipInstall,

    publish <<= publish dependsOn (publish in puic),
    PgpKeys.publishSigned <<= PgpKeys.publishSigned dependsOn (PgpKeys.publishSigned in puic),

    publishLocal <<= publishLocal dependsOn zipInstall,
    PgpKeys.publishLocalSigned <<= PgpKeys.publishLocalSigned dependsOn zipInstall,

    publishLocal <<= publishLocal dependsOn (publishLocal in puic),
    PgpKeys.publishLocalSigned <<= PgpKeys.publishLocalSigned dependsOn (PgpKeys.publishLocalSigned in puic),

    makePom <<= makePom dependsOn md5Install,

    md5Install <<=
      ((baseDirectory, update, streams,
        mdInstallDirectory in Global,
        version
        ) map {
        (base, up, s, mdInstallDir, buildVersion) =>

          s.log.info(s"***(2) MD5 of md.install.dir=$mdInstallDir")

      }) dependsOn updateInstall,

    updateInstall <<=
      (baseDirectory, update, streams,
        mdInstallDirectory in Global,
        artifactZipFile in puic) map {
        (base, up, s, mdInstallDir, puicResource) =>

          s.log.info(s"***(1) Updating md.install.dir=$mdInstallDir")
          s.log.info(s"***    Installling resource=$puicResource")

          val files = IO.unzip(puicResource, mdInstallDir)
          s.log.info(
            s"=> installed resource in md.install.dir=$mdInstallDir with ${files.size} " +
              s"files extracted from zip: ${puicResource.getName}")
      },

    zipInstall <<=
      (baseDirectory, update, streams,
        mdInstallDirectory in Global,
        artifactZipFile,
        makePom, scalaBinaryVersion
        ) map {
        (base, up, s, mdInstallDir, zip, pom, sbV) =>

          import java.nio.file.attribute.PosixFilePermission
          import com.typesafe.sbt.packager.universal._

          s.log.info(s"\n*** Creating the zip: $zip")

          val parentDir = mdInstallDir.getParentFile
          val top: BFile = mdInstallDir.toScala

          val macosExecutables: Iterator[BFile] = top.glob("**/*.app/Content/MacOS/*")
          macosExecutables.foreach { f: BFile =>
            s.log.info(s"* +X $f")
            f.addPermission(PosixFilePermission.OWNER_EXECUTE)
          }

          val windowsExecutables: Iterator[BFile] = top.glob("**/*.exe")
          windowsExecutables.foreach { f: BFile =>
            s.log.info(s"* +X $f")
            f.addPermission(PosixFilePermission.OWNER_EXECUTE)
          }

          val javaExecutables: Iterator[BFile] = top.glob("jre*/**/bin/*")
          javaExecutables.foreach { f: BFile =>
            s.log.info(s"* +X $f")
            f.addPermission(PosixFilePermission.OWNER_EXECUTE)
          }

          val unixExecutables: Iterator[BFile] = top.glob("bin/{magicdraw,submit_issue}")
          unixExecutables.foreach { f: BFile =>
            s.log.info(s"* +X $f")
            f.addPermission(PosixFilePermission.OWNER_EXECUTE)
          }

          val zipDir = zip.getParentFile.toScala
          Cmds.mkdirs(zipDir)

          val fileMappings = mdInstallDir.*** pair relativeTo(parentDir)
          ZipHelper.zipNative(fileMappings, zip)

          s.log.info(s"\n*** Created the zip: $zip")

          zip
      }
  )
  .settings(IMCEReleasePlugin.packageReleaseProcessSettings)

def UpdateProperties(mdInstall: File): RewriteRule = {

  println(s"update properties for md.install=$mdInstall")
  val binDir = mdInstall / "bin"
  require(binDir.exists, binDir)
  val binSub = MD5SubDirectory(
    name = "bin",
    files = IO
      .listFiles(binDir, GlobFilter("*.properties"))
      .sorted.map(MD5.md5File(binDir)))

  val docGenScriptsDir = mdInstall / "DocGenUserScripts"
  require(docGenScriptsDir.exists, docGenScriptsDir)
  val scriptsSub = MD5SubDirectory(
    name = "DocGenUserScripts",
    dirs = IO
      .listFiles(docGenScriptsDir, DirectoryFilter)
      .sorted.map(MD5.md5Directory(docGenScriptsDir)))

  val libDir = mdInstall / "lib"
  require(libDir.exists, libDir)
  val libSub = MD5SubDirectory(
    name = "lib",
    files = IO
      .listFiles(libDir, GlobFilter("*.jar"))
      .sorted.map(MD5.md5File(libDir)))

  val pluginsDir = mdInstall / "plugins"
  require(pluginsDir.exists)
  val pluginsSub = MD5SubDirectory(
    name = "plugins",
    dirs = IO
      .listFiles(pluginsDir, DirectoryFilter)
      .sorted.map(MD5.md5Directory(pluginsDir)))

  val modelsDir = mdInstall / "modelLibraries"
  require(modelsDir.exists, libDir)
  val modelsSub = MD5SubDirectory(
    name = "modelLibraries",
    files = IO
      .listFiles(modelsDir, GlobFilter("*.mdzip") || GlobFilter("*.mdxml"))
      .sorted.map(MD5.md5File(modelsDir)))

  val profilesDir = mdInstall / "profiles"
  require(profilesDir.exists, libDir)
  val profilesSub = MD5SubDirectory(
    name = "profiles",
    files = IO
      .listFiles(profilesDir, GlobFilter("*.mdzip") || GlobFilter("*.mdxml"))
      .sorted.map(MD5.md5File(profilesDir)))

  val samplesDir = mdInstall / "samples"
  require(samplesDir.exists, libDir)
  val samplesSub = MD5SubDirectory(
    name = "samples",
    files = IO
      .listFiles(samplesDir, GlobFilter("*.mdzip") || GlobFilter("*.mdxml"))
      .sorted.map(MD5.md5File(samplesDir)))

  val all = MD5SubDirectory(
    name = ".",
    sub = Seq(binSub, libSub, pluginsSub, modelsSub, profilesSub, scriptsSub, samplesSub))

  new RewriteRule {

    import spray.json._
    import MD5JsonProtocol._

    override def transform(n: XNode): Seq[XNode] = n match {
      case <md5></md5> =>
        <md5>
          {all.toJson}
        </md5>
      case _ =>
        n
    }
  }
}
