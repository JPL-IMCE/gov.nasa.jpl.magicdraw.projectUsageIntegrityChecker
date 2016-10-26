
sbtPlugin := false

name := "gov.nasa.jpl.magicdraw.projectUsageIntegrityChecker"

description := "Project Usage Integrity Checker for MagicDraw 18"

moduleName := name.value

organization := "gov.nasa.jpl.imce"

organizationName := "JPL-IMCE"

organizationHomepage := Some(url(s"https://github.com/${organizationName.value}"))

homepage := Some(url(s"https://jpl-imce.github.io/${moduleName.value}"))

git.remoteRepo := s"git@github.com:${organizationName.value}/${moduleName.value}"

scmInfo := Some(ScmInfo(
  browseUrl = url(s"https://github.com/${organizationName.value}/${moduleName.value}"),
  connection = "scm:"+git.remoteRepo.value))

developers := List(
  Developer(
    id="NicolasRouquette",
    name="Nicolas F. Rouquette",
    email="nicolas.f.rouquette@jpl.nasa.gov",
    url=url("https://github.com/NicolasRouquette")),
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

