import java.io.File

import sbt.Keys._
import sbt._

import com.typesafe.sbt.pgp.PgpKeys

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
  .settings(addArtifact(Artifact("imce_md18_0_sp6_puic_resource", "zip", "zip"), artifactZipFile).settings: _*)
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


    libraryDependencies ++= Seq(

      "org.omg.tiwg.vendor.nomagic"
        % "com.nomagic.magicdraw.package"
        % "18.0-sp6" artifacts Artifact("com.nomagic.magicdraw.package", "zip", "zip", Some("part1"), Seq(), None, Map()),

      "org.omg.tiwg.vendor.nomagic"
        % "com.nomagic.magicdraw.package"
        % "18.0-sp6" artifacts Artifact("com.nomagic.magicdraw.package", "zip", "zip", Some("part2"), Seq(), None, Map())
    ),

    extractArchives := {
      val base = baseDirectory.value
      val up = update.value
      val s = streams.value
      val mdInstallDir = (mdInstallDirectory in ThisBuild).value

      if (!mdInstallDir.exists) {

        val parts = (for {
          cReport <- up.configurations
          if cReport.configuration == "compile"
          mReport <- cReport.modules
          if mReport.module.organization == "org.omg.tiwg.vendor.nomagic"
          (artifact, archive) <- mReport.artifacts
        } yield archive).sorted

        s.log.info(s"Extracting MagicDraw from ${parts.size} parts:")
        parts.foreach { p => s.log.info(p.getAbsolutePath) }

        val merged = File.createTempFile("md_merged", ".zip")
        println(s"merged: ${merged.getAbsolutePath}")

        val zip = File.createTempFile("md_no_install", ".zip")
        println(s"zip: ${zip.getAbsolutePath}")

        val script = File.createTempFile("unzip_md", ".sh")
        println(s"script: ${script.getAbsolutePath}")

        val out = new java.io.PrintWriter(new java.io.FileOutputStream(script))
        out.println("#!/bin/bash")
        out.println(parts.map(_.getAbsolutePath).mkString("cat ", " ", s" > ${merged.getAbsolutePath}"))
        out.println(s"zip -FF ${merged.getAbsolutePath} --out ${zip.getAbsolutePath}")
        out.println(s"unzip -q ${zip.getAbsolutePath} -d ${mdInstallDir.getAbsolutePath}")
        out.close()

        val result = sbt.Process(command = "/bin/bash", arguments = Seq[String](script.getAbsolutePath)).!

        require(0 <= result && result <= 2, s"Failed to execute script (exit=$result): ${script.getAbsolutePath}")

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
