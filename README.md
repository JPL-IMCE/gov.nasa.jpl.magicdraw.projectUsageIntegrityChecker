# Project Usage Integrity Checker for MagicDraw 18

[![Build Status](https://travis-ci.org/JPL-IMCE/gov.nasa.jpl.magicdraw.projectUsageIntegrityChecker.svg?branch=md18_0_sp6)](https://travis-ci.org/JPL-IMCE/gov.nasa.jpl.magicdraw.projectUsageIntegrityChecker)
[ ![Download](https://api.bintray.com/packages/jpl-imce/gov.nasa.jpl.imce/gov.nasa.jpl.magicdraw.projectUsageIntegrityChecker/images/download.svg) ](https://bintray.com/jpl-imce/gov.nasa.jpl.imce/gov.nasa.jpl.magicdraw.projectUsageIntegrityChecker/_latestVersion)
 
## Documentation

- [PUIC Presentation (May 2013)](https://github.jpl.nasa.gov/secae/gov.nasa.jpl.magicdraw.projectUsageIntegrityChecker/blob/cae_md18_0_sp5/projectUsageIntegrityChecker/doc/PUIC-May2013.pptx)
  (with PUIC installed, available in `<md.install.dir>/manual/ProjectUsageIntegrityChecker/PUIC-May2013.pptx`)

- [PUIC Local Migration Supoprt (Dec 2014)](https://github.jpl.nasa.gov/secae/gov.nasa.jpl.magicdraw.projectUsageIntegrityChecker/blob/cae_md18_0_sp5/projectUsageIntegrityChecker/doc/PUICLocalMigrationSupport-Dec2014.pptx)
  (with PUIC installed, available in `<md.install.dir>/manual/ProjectUsageIntegrityChecker/PUICLocalMigrationSupport-Dec2014.pptx`)

## Build Process

See item 9 [here](https://github.jpl.nasa.gov/imce/gov.nasa.jpl.imce.vm/blob/master/doc/buildProcessAndArtifacts.md#9-puic)

## Build Artifacts

- PUIC Installable Resource

  Maven group=gov.nasa.jpl.cae.magicdraw.plugins
  Maven name=cae_md18_0_sp5_puic_2.11

- PUIC Package Bundle

  Maven group=gov.nasa.jpl.cae.magicdraw.packages
  Maven nameA=cae_md18_0_sp5_puic_2.11

## Test plan

### 1 PUIC Installable Resource or PUIC Package Bundle

#### 1.1 PUIC Installable Resource

- Download the PUIC installable resource
- Start MD: [cae.md.aspectj_scala](https://github.jpl.nasa.gov/imce/gov.nasa.jpl.imce.vm/blob/master/doc/buildProcessAndArtifacts.md#6-caemdaspectj_scala-branch-cae_md18_0_sp5)
- MD install the PUIC resource
- Restart MD

#### 1.2 PUIC Package Bundle

- Download the PUIC package bundle
- Start MD: [cae.md18_0sp5.puic](https://github.jpl.nasa.gov/imce/gov.nasa.jpl.imce.vm/blob/master/doc/buildProcessAndArtifacts.md#9-puic)

### 2 PUIC Tests

Perform the following tests with 1.1 and 1.2 above.

#### 2.1 Test that PUIC is installed properly

- PUIC toolbar should be visible by default
- MD Option | Environment should have PUIC properties
- Configure the PUIC 'dot' location

#### 2.2 Test that PUIC profile is not mounted by default

- Make sure the PUIC toolbar ON/OFF button is "ON"
- Open `<md.install.dir>/samples/SysML/Introduction to SysML.mdzip`

  - Check that the PUIC is RED.
  - Check that the PUIC profile is not mounted.
  - Check the `$HOME/.magicdraw-puic-<version>/18.0/magicdraw.log` shows something like this:

      ```
      2016-01-21 17:28:11,096 [AWT-EventQueue-0] ERROR PLUGINS - CAE Project Usage Integrity Checker - Error - ProjectUsage graph is invalid
      Project classification: project (private data; nothing shared)
      'Introduction to SysML'
      SSCAEProjectUsageGraph(Vertices=14, Edges=24, Diagrams=59)
         OK: no illegal teamwork transactions detected
      ERROR: this project should have the System/Standard Profile flag set
         OK: no local modules with teamwork project IDs
      ERROR: this project is missing 3 direct ProjectUsage mount attachments
         OK: no unresolved ProjectUsage relationships
         OK: no proxies detected
         OK: all projects are available
         OK: all local projects have SSP flag
         OK: all projects have no missing shares
      ERROR: 1 projects used from MD's install folder do not have the Standard/System Profile flag set
      WARNING: 1 SSP profiles have non-unique names
         OK: all user profiles have unique names
         OK: all profiles have unique URIs
         OK: all packages have unique URIs
         OK: project usage mount relationships are acyclic
      ERROR: 13 project usage mount relationships are inconsistent
      ERROR: 3 projects are used inconsistently
      ERROR: 10 project usage mount relationships are invalid
         OK: all shared package usage constraints are consistent
         OK: all modules & project have consistent shared package classifications
      ```

  - Check that there is no MD validation window open with any PUIC validation errors (because there is no PUIC profile!)


#### 2.3 Test that PUIC properly detects errors

- Make sure the PUIC toolbar ON/OFF button is "ON"
- Open `<md.install.dir>/samples/ProjectUsageIntegrityChecker/INCONSISTENT.mdzip`

  - Check that the PUIC is RED.
  - Click the PUIC Status button.
  - Check the `$HOME/.magicdraw-puic-<version>/18.0/magicdraw.log` shows something like this:

      ```
      2016-01-21 17:32:10,446 [AWT-EventQueue-0] ERROR PLUGINS - CAE Project Usage Integrity Checker - Error - ProjectUsage graph is invalid
      Project classification: project/module hybrid (private data; shared packages)
      'INCONSISTENT'
      SSCAEProjectUsageGraph(Vertices=3, Edges=2, Diagrams=3)
         OK: no illegal teamwork transactions detected
         OK: no local modules with teamwork project IDs
         OK: no missing direct ProjectUsage mount attachments
         OK: no unresolved ProjectUsage relationships
         OK: no proxies detected
         OK: all projects are available
         OK: all local projects have SSP flag
         OK: all projects have no missing shares
         OK: all local projects used from MD's install folder have the Standard/System Profile flag set
         OK: all SSP profiles have unique names
         OK: all user profiles have unique names
         OK: all profiles have unique URIs
         OK: all packages have unique URIs
         OK: project usage mount relationships are acyclic
         OK: project usage mount relationships are consistent
         OK: all projects are used consistently
         OK: project usage mount relationships are valid
         OK: all shared package usage constraints are consistent
         OK: all DEPRECATED shared packages are used consistently
      ERROR: 1 modules/project with shared packages inconsistently classified
      ```

  - Check that the MD Message Window shows something like this:

      ```
      [2016.01.21::17:32:08]
      = Project: file:.../cae.md18_0sp5.puic-<version>/samples/ProjectUsageIntegrityChecker/INCONSISTENT.mdzip ==========================================
      [2016.01.21::17:32:08]
      ERROR: 1 modules/project with INCONSISTENT shared packages classifications
      [2016.01.21::17:32:08]
      => P3 is inconsistently classified within module/project 'INCONSISTENT'
      [2016.01.21::17:32:08]
      => P2 is inconsistently classified within module/project 'INCONSISTENT'
      [2016.01.21::17:32:08]
      => P1 is inconsistently classified within module/project 'INCONSISTENT'
      [2016.01.21::17:32:08]
      ===========================================
      ```

  - Check that there is an MD validation window titled: "SSCAE ProjectUsage Validation Results"
  - Check that there is exactly 1 validation error: "Apply <<SSCAEProjectModel>> to 'Data'"

#### 2.4 Test that PUIC properly detects valid propjects

- Make sure the PUIC toolbar ON/OFF button is "ON"
- Open `<md.install.dir>/samples/ProjectUsageIntegrityChecker/Supplier-Client-Example.mdzip`

  - Check that the PUIC is Green.
  - Click the PUIC Status button.
  - Check the `$HOME/.magicdraw-puic-<version>/18.0/magicdraw.log` shows something like this:

      ```
      2016-01-21 17:42:34,176 [AWT-EventQueue-0] INFO  PLUGINS - CAE Project Usage Integrity Checker - OK - ProjectUsage graph is valid
      Project classification: project (private data; nothing shared)
      'Supplier-Client-Example'
      SSCAEProjectUsageGraph(Vertices=3, Edges=2, Diagrams=8)
         OK: no illegal teamwork transactions detected
         OK: no local modules with teamwork project IDs
         OK: no missing direct ProjectUsage mount attachments
         OK: no unresolved ProjectUsage relationships
         OK: no proxies detected
         OK: all projects are available
         OK: all local projects have SSP flag
         OK: all projects have no missing shares
         OK: all local projects used from MD's install folder have the Standard/System Profile flag set
         OK: all SSP profiles have unique names
         OK: all user profiles have unique names
         OK: all profiles have unique URIs
         OK: all packages have unique URIs
         OK: project usage mount relationships are acyclic
         OK: project usage mount relationships are consistent
         OK: all projects are used consistently
         OK: project usage mount relationships are valid
         OK: all shared package usage constraints are consistent
         OK: all modules & project have consistent shared package classifications
      ```

  - Check that the MD Message Window shows something like this:

      ```
      [2016.01.21::17:42:34] = Project: file:.../samples/ProjectUsageIntegrityChecker/Supplier-Client-Example.mdzip ==========================================
      [2016.01.21::17:42:34] =========================================
      ```

  - Check that there is no MD validation window titled: "SSCAE ProjectUsage Validation Results"