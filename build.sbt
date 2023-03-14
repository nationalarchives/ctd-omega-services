val CatsEffectVersion = "3.4.8"
val Log4CatsVersion = "2.5.0"
val PureConfigVersion = "0.17.2"
val Jms4SVersion = "0.0.1-53518bb-SNAPSHOT"
//val AwsJavaSdkVersion = "1.12.196"
val AwsJavaSdkVersion = "2.18.1"
val CirceVersion = "0.14.1"

ThisBuild / versionScheme := Some("semver-spec")
ThisBuild / scalacOptions ++= Seq("-unchecked", "-deprecation")

lazy val root = Project("ctd-omega-services", file("."))
  .enablePlugins(AutomateHeaderPlugin)
  .enablePlugins(BuildInfoPlugin)
  .enablePlugins(JavaAppPackaging)
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
    organizationHomepage := Some(url("http://nationalarchives.gov.uk")),
    maintainer := "webmaster@nationalarchives.gov.uk",
    githubOwner := "nationalarchives",
    githubRepository := "ctd-omega-services",
    githubTokenSource := TokenSource.Or(
      TokenSource.Environment("GITHUB_TOKEN"),
      TokenSource.GitConfig("github.token")
    ),
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
        name = "Adam Retetr",
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
      "org.typelevel"         %% "cats-effect-std"               % CatsEffectVersion,
      "dev.fpinbo"            %% "jms4s"                         % Jms4SVersion,
      "dev.fpinbo"            %% "jms4s-simple-queue-service"    % Jms4SVersion,
      "com.github.pureconfig" %% "pureconfig-core"               % PureConfigVersion,
      "com.github.pureconfig" %% "pureconfig-generic"            % PureConfigVersion,
      "com.github.pureconfig" %% "pureconfig-generic-base"       % PureConfigVersion,
      "com.fasterxml.uuid"     % "java-uuid-generator"           % "4.1.0",
      "com.beachape"          %% "enumeratum"                    % "1.7.2",
      "org.typelevel"         %% "log4cats-core"                 % Log4CatsVersion,
      "org.typelevel"         %% "log4cats-slf4j"                % Log4CatsVersion,
      "ch.qos.logback"         % "logback-classic"               % "1.3.5"           % Runtime, // Java 8 compatible
      "net.logstash.logback"   % "logstash-logback-encoder"      % "7.3"             % Runtime,
      "io.circe"              %% "circe-core"                    % CirceVersion      % "it,test",
      "io.circe"              %% "circe-generic"                 % CirceVersion      % "it,test",
      "io.circe"              %% "circe-parser"                  % CirceVersion      % "it,test",
      "org.scalatest"         %% "scalatest"                     % "3.2.15"          % "it,test",
      "org.typelevel"         %% "cats-effect-testing-scalatest" % "1.5.0"           % "it,test",
      "org.mockito"           %% "mockito-scala-scalatest"       % "1.17.12"         % Test,
      "com.vladsch.flexmark"   % "flexmark-profile-pegdown"      % "0.62.2"          % Test, // Java 8 compatible
      "software.amazon.awssdk" % "auth"                          % AwsJavaSdkVersion % Test,
      "software.amazon.awssdk" % "sqs"                           % AwsJavaSdkVersion % Test,
//      "com.amazonaws"          % "aws-java-sdk-core"             % AwsJavaSdkVersion % Test,
//      "com.amazonaws"          % "aws-java-sdk-sqs"              % AwsJavaSdkVersion % Test,
      "com.amazonaws" % "amazon-sqs-java-messaging-lib" % "2.0.2" % Test,

      // better monadic for compiler plugin as suggested by documentation
      compilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1")
    ),
    resolvers += Resolver.githubPackages("rwalpole")
  )
Test / testOptions += Tests.Argument(TestFrameworks.ScalaTest, "-h", "target/test-reports")
