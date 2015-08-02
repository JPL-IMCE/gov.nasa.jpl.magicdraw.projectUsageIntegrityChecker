
import gov.nasa.jpl.mbee.sbt._
import sbt.Keys._
import sbt._

lazy val jpl_omg_oti_magicdraw_dynamicscripts = Project("gov-nasa-jpl-magicdraw-projectUsageIntegrityChecker", file(".")).
  settings(GitVersioning.buildSettings). // in principle, unnecessary; in practice: doesn't work without this
  enablePlugins(MBEEGitPlugin, MBEEMagicDrawEclipseClasspathPlugin).
  settings(MBEEPlugin.mbeeAspectJSettings).
  settings(
    MBEEKeys.mbeeLicenseYearOrRange := "2013-2015",
    MBEEKeys.mbeeOrganizationInfo := MBEEPlugin.MBEEOrganizations.secae,

    javaSource in Compile := baseDirectory.value / "src",

    classDirectory in Compile := baseDirectory.value / "bin"
  )

