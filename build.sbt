val CatsEffectVersion = "3.4.5"
val PureConfigVersion = "0.17.2"

ThisBuild / versionScheme := Some("semver-spec")

ThisBuild / githubTokenSource.withRank(KeyRanks.Invisible) := TokenSource.Or(
  TokenSource.Environment("GITHUB_TOKEN"),
  TokenSource.GitConfig("github.token")
)

lazy val root = Project("ctd-omega-services", file("."))
  .enablePlugins(AutomateHeaderPlugin)
  .enablePlugins(BuildInfoPlugin)
  .enablePlugins(JavaAppPackaging)
  .settings(
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
      "org.typelevel" %% "cats-effect" % CatsEffectVersion,
      // concurrency abstractions and primitives (Concurrent, Sync, Async etc.)
      "org.typelevel" %% "cats-effect-kernel" % CatsEffectVersion,
      // standard "effect" library (Queues, Console, Random etc.)
      "org.typelevel"         %% "cats-effect-std"               % CatsEffectVersion,
      "dev.fpinbo"            %% "jms4s-simple-queue-service"    % "0.0.1-53518bb-SNAPSHOT",
      "com.github.pureconfig" %% "pureconfig-core"               % PureConfigVersion,
      "com.github.pureconfig" %% "pureconfig-generic"            % PureConfigVersion,
      "com.github.pureconfig" %% "pureconfig-generic-base"       % PureConfigVersion,
      "com.fasterxml.uuid"     % "java-uuid-generator"           % "4.1.0",
      "com.monovore"          %% "decline"                       % "2.4.1",
      "com.beachape"          %% "enumeratum"                    % "1.7.2",
      "org.slf4j"              % "slf4j-simple"                  % "2.0.5",
      "org.scalatest"         %% "scalatest"                     % "3.2.15"  % Test,
      "org.typelevel"         %% "cats-effect-testing-scalatest" % "1.5.0"   % Test,
      "org.mockito"           %% "mockito-scala-scalatest"       % "1.17.12" % Test,
      // better monadic for compiler plugin as suggested by documentation
      compilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1")
    ),
    resolvers += Resolver.githubPackages("rwalpole")
  )
