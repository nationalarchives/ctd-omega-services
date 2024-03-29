import com.typesafe.sbt.packager.linux.LinuxSymlink

val CatsEffectVersion = "3.5.2"
val Log4CatsVersion = "2.6.0"
val PureConfigVersion = "0.17.4"
val Jms4SVersion = "0.5.0-TNA-OMG-0.2.0"
val CirceVersion = "0.14.6"
val EnumeratumVersion = "1.7.2"
val JenaVersion = "3.17.0"
val AwsSdkVersion = "2.21.4"

ThisBuild / versionScheme := Some("semver-spec")
ThisBuild / scalacOptions ++= Seq("-unchecked", "-deprecation")

lazy val root = Project("ctd-omega-services", file("."))
  .enablePlugins(AutomateHeaderPlugin)
  .enablePlugins(BuildInfoPlugin)
  .enablePlugins(JavaServerAppPackaging)
  .enablePlugins(LinuxPlugin)
  .enablePlugins(RpmPlugin)
  .enablePlugins(SystemdPlugin)
  .configs(IntegrationTest)
  .settings(
    Defaults.itSettings,
    organization := "uk.gov.nationalarchives",
    name := "ctd-omega-services",
    Compile / mainClass := Some("uk.gov.nationalarchives.omega.api.ApiServiceApp"),
    version := "0.1.0-SNAPSHOT",
    scalaVersion := "2.13.10",
    licenses := Seq("MIT" -> url("https://opensource.org/licenses/MIT")),
    homepage := Some(
      url("https://github.com/nationalarchives/ctd-omega-services")
    ),
    startYear := Some(2023),
    description := "Omega API Service",
    organizationName := "The National Archives",
    organizationHomepage := Some(url("https://www.nationalarchives.gov.uk")),
    maintainer := "cataloguingtaxonomyanddata@nationalarchives.gov.uk",
    packageSummary := "Omega Services API Package",
    packageDescription := "Services API for Project Omega",
    rpmVendor := "The National Archives",
    rpmLicense := Some("MIT License"),
    scmInfo := Some(
      ScmInfo(
        url("https://github.com/nationalarchives/ctd-omega-services"),
        "scm:git@github.com:nationalarchives/ctd-omega-services.git"
      )
    ),
    developers := List(
      Developer(
        id = "rwalpole",
        name = "Rob Walpole",
        email = "rob.walpole@devexe.co.uk",
        url = url("http://www.devexe.co.uk")
      ),
      Developer(
        id = "adamretter",
        name = "Adam Retter",
        email = "adam@evolvedbinary.com",
        url = url("https://www.evolvedbinary.com")
      )
    ),
    name := "ctd-omega-services",
    libraryDependencies ++= Seq(
      // "core" module - IO, IOApp, schedulers
      // This pulls in the kernel and std modules automatically.
      "org.typelevel" %% "cats-core"   % "2.9.0",
      "org.typelevel" %% "cats-effect" % CatsEffectVersion,
      // concurrency abstractions and primitives (Concurrent, Sync, Async etc.)
      "org.typelevel" %% "cats-effect-kernel" % CatsEffectVersion,
      // standard "effect" library (Queues, Console, Random etc.)
      "org.typelevel"                                 %% "cats-effect-std"             % CatsEffectVersion,
      "uk.gov.nationalarchives.thirdparty.dev.fpinbo" %% "jms4s"                       % Jms4SVersion,
      "uk.gov.nationalarchives.thirdparty.dev.fpinbo" %% "jms4s-simple-queue-service"  % Jms4SVersion,
      "com.github.pureconfig"                         %% "pureconfig-core"             % PureConfigVersion,
      "com.github.pureconfig"                         %% "pureconfig-generic"          % PureConfigVersion,
      "com.github.pureconfig"                         %% "pureconfig-generic-base"     % PureConfigVersion,
      "com.fasterxml.uuid"                             % "java-uuid-generator"         % "4.3.0",
      "com.beachape"                                  %% "enumeratum"                  % EnumeratumVersion,
      "com.beachape"                                  %% "enumeratum-circe"            % EnumeratumVersion,
      "org.typelevel"                                 %% "log4cats-core"               % Log4CatsVersion,
      "org.typelevel"                                 %% "log4cats-slf4j"              % Log4CatsVersion,
      "com.chuusai"                                   %% "shapeless"                   % "2.3.10",
      "org.apache.commons"                             % "commons-lang3"               % "3.13.0",
      "io.circe"                                      %% "circe-core"                  % CirceVersion,
      "io.circe"                                      %% "circe-parser"                % CirceVersion,
      "io.circe"                                      %% "circe-generic"               % CirceVersion,
      "com.amazonaws"                                  % "amazon-neptune-sigv4-signer" % "2.4.0",
      "org.apache.httpcomponents"                      % "httpclient"                  % "4.5.13",
      "org.apache.httpcomponents"                      % "httpcore"                    % "4.4.13",
      "org.phenoscape"                                %% "sparql-utils"                % "1.3.1",
      "org.apache.jena"                                % "jena-core"                   % JenaVersion,
      "org.apache.jena"                                % "jena-arq"                    % JenaVersion,
      "com.propensive"                                %% "magnolia"                    % "0.17.0",
      "com.propensive"                                %% "mercator"                    % "0.2.1",
      "ch.qos.logback" % "logback-classic" % "1.3.11" % Runtime, // Java 8 compatible
      "org.codehaus.janino" % "janino" % "3.1.10" % Runtime, // NOTE(AR) required for conditions in `logback-classic`
      "net.logstash.logback" % "logstash-logback-encoder" % "7.4" % Runtime, // NOTE(AR) required for JSON log files via `logback-classic`
      "org.scalatest"         %% "scalatest"                     % "3.2.17"      % "it,test",
      "org.typelevel"         %% "cats-effect-testing-scalatest" % "1.5.0"       % "it,test",
      "com.vladsch.flexmark"   % "flexmark-profile-pegdown"      % "0.62.2"      % "it,test", // Java 8 compatible
      "org.mockito"           %% "mockito-scala-scalatest"       % "1.17.25"     % "it,test",
      "software.amazon.awssdk" % "auth"                          % AwsSdkVersion % "it",
      "software.amazon.awssdk" % "regions"                       % AwsSdkVersion % "it",
      "software.amazon.awssdk" % "sqs"                           % AwsSdkVersion % "it",
      "com.amazonaws" % "amazon-sqs-java-messaging-lib" % "2.0.2" % "it", // NOTE(AR) this must be the same version as depended on by `jms4s-simple-queue-service`

      // better monadic for compiler plugin as suggested by documentation
      compilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1")
    )
  )

// Generate the `logback-test.properties` file (later used by `logback-test.xml`)
Test / resourceGenerators += createLogbackTestProperties(Test).taskValue
IntegrationTest / resourceGenerators += createLogbackTestProperties(IntegrationTest).taskValue

Universal / mappings ++= Seq(
  file("LICENSE")                        -> "LICENSE",
  file("README.md")                      -> "README.md",
  file("src/main/package/settings.conf") -> "etc/settings.conf",
  file("src/main/package/logback.xml")   -> "etc/logback.xml"
)
Universal / packageZipTarball / universalArchiveOptions := Seq(
  "--exclude",
  "*.bat"
) ++ (Universal / packageZipTarball / universalArchiveOptions).value
bashScriptExtraDefines ++= Seq(
  """addJava "-Dconfig.file=${app_home}/../etc/settings.conf"""",
  """addJava "-Dmessage-store-base-dir=${app_home}/../"""",
  """addJava "-Dlogback.custom.targetPath=${app_home}/.."""",
  """addJava "-Dlogback.configurationFile=${app_home}/../etc/logback.xml""""
)
batScriptExtraDefines ++= Seq(
  """call :add_java "-Dconfig.file=%APP_HOME%\..\etc\settings.conf"""",
  """call :add_java "-Dmessage-store-base-dir=%APP_HOME%\..\"""",
  """call :add_java "-Dlogback.custom.targetPath=%APP_HOME%\.."""",
  """call :add_java "-Dlogback.configurationFile=%APP_HOME%\..\etc\logback.xml""""
)

Linux / daemonUser := "ctd-omega-services-api"
Linux / daemonGroup := "ctd-omega-services-api"
Linux / serviceAutostart := false
// change the symlink `<install>/logs` to `<install>/log`
Linux / linuxPackageSymlinks := {
  val pkg = packageName.value
  // the `logs` symlink we want to replace
  val logsLink = defaultLinuxInstallLocation.value + "/" + pkg + "/logs"
  val currentLinuxPackageSymLinks = linuxPackageSymlinks.value
  currentLinuxPackageSymLinks.map {
    case LinuxSymlink(link, destination) if logsLink.equals(link) =>
      // the `log` symlink we want instead of the `logs` symlink
      val logLink = defaultLinuxInstallLocation.value + "/" + pkg + "/log"
      LinuxSymlink(logLink, destination)
    case linuxSymLink: LinuxSymlink =>
      // preserve any other symlink
      linuxSymLink
  }
}
// add the symlink `<install>/spool` to `/var/spool/<pkg>`
Linux / linuxPackageMappings += packageTemplateMapping(s"/var/spool/${(Linux / packageName).value}")()
  .withUser((Linux / daemonUser).value)
  .withGroup((Linux / daemonGroup).value)
  .withPerms("750")
Linux / linuxPackageSymlinks += LinuxSymlink(
  (Linux / defaultLinuxInstallLocation).value + "/" + (Linux / packageName).value + "/spool",
  "/var/spool/" + (Linux / packageName).value
)

Test / testOptions += Tests.Argument(TestFrameworks.ScalaTest, "-oD", "-h", "target/test-reports")
IntegrationTest / testOptions += Tests.Argument(TestFrameworks.ScalaTest, "-h", "target/it-reports")
IntegrationTest / fork := true

coverageEnabled := sys.props.getOrElse("coverageEnabled", "true").toBoolean

/** Task to create a `logback-test.properties` file.
  *
  * @param configuration
  *   A build configuration
  *
  * @return
  *   the task
  */
def createLogbackTestProperties(configuration: Configuration) = Def.task {
  val testTargetPath = (configuration / target).value
  val logBackTestProperties = (configuration / resourceManaged).value / "logback-test.properties"
  val contents =
    "name=%s\nversion=%s\nlogback.custom.targetPath=%s".format(name.value, version.value, testTargetPath.getPath)
  IO.write(logBackTestProperties, contents)
  Seq(logBackTestProperties)
}
