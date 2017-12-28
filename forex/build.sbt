name := "forex"
version := "1.0.0"

scalaVersion := "2.12.4"
scalacOptions ++= Seq(
  "-deprecation",
  "-encoding",
  "UTF-8",
  "-feature",
  "-language:existentials",
  "-language:higherKinds",
  "-Ypartial-unification",
  "-language:experimental.macros",
  "-language:implicitConversions"
)

resolvers +=
  "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
resolvers +=
  "bintray-backline-open-source-releases" at "https://dl.bintray.com/backline/open-source"
resolvers +=
  Resolver.jcenterRepo

libraryDependencies ++= Seq(
  "com.github.pureconfig"      %% "pureconfig"           % "0.7.2",
  "com.softwaremill.quicklens" %% "quicklens"            % "1.4.11",
  "com.typesafe.akka"          %% "akka-actor"           % "2.4.19",
  "com.typesafe.akka"          %% "akka-http"            % "10.0.10",
  "de.heikoseeberger"          %% "akka-http-circe"      % "1.18.1",
  "backline"                   %% "akka-http-metrics"    % "1.0.0",
  "io.circe"                   %% "circe-core"           % "0.8.0",
  "io.circe"                   %% "circe-generic"        % "0.8.0",
  "io.circe"                   %% "circe-generic-extras" % "0.8.0",
  "io.circe"                   %% "circe-java8"          % "0.8.0",
  "io.circe"                   %% "circe-jawn"           % "0.8.0",
  "org.atnos"                  %% "eff"                  % "4.5.0",
  "org.atnos"                  %% "eff-monix"            % "4.5.0",
  "org.typelevel"              %% "cats-core"            % "0.9.0",
  "org.zalando"                %% "grafter"              % "2.3.0",
  "ch.qos.logback"             % "logback-classic"       % "1.2.3",
  "com.typesafe.scala-logging" %% "scala-logging"        % "3.7.2",
  "nl.grons"                   %% "metrics-scala"        % "3.5.9_a2.4",
  "com.readytalk"              % "metrics3-statsd"       % "4.2.0",
  "org.scalatest"              % "scalatest_2.12"        % "3.0.4" % Test,
  "org.scalamock"              %% "scalamock"            % "4.0.0" % Test,
  compilerPlugin("org.spire-math"  %% "kind-projector" % "0.9.4"),
  compilerPlugin("org.scalamacros" %% "paradise"       % "2.1.1" cross CrossVersion.full)
)
