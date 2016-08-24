name := "rwa-quiz"

version := "1.0"

scalaVersion := "2.11.7"

lazy val `rwa-quiz` = (project in file(".")).enablePlugins(PlayScala)

routesGenerator := InjectedRoutesGenerator

com.typesafe.sbt.SbtScalariform.scalariformSettings

routesImport += "binders.PathBinders._"

routesImport += "binders.QueryStringBinders._"

libraryDependencies += filters
