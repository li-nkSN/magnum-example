ThisBuild / organization := "org.li_nk.magnum.example"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "3.6.4"

val munitVersion = "1.1.0"
val postgresDriverVersion = "42.7.4"

// Root project that aggregates all modules
lazy val root = project
  .in(file("."))
  .settings(
    name := "magnum-example",
    publish / skip := true,
  scalacOptions ++= Seq(
    "-explain", "-deprecation", "-feature", "-unchecked"
  ),
      libraryDependencies ++= Seq(
      "ch.qos.logback" % "logback-classic" % "1.5.18",
      "org.slf4j" % "slf4j-api" % "2.0.17",

      // Core Magnum library
      "com.augustnagro" %% "magnum" % "2.0.0-M1",
      
      // Postgres extension module for Magnum
      "com.augustnagro" %% "magnumpg" % "2.0.0-M1",
      "com.augustnagro" %% "magnumzio" % "2.0.0-M1", 
      "dev.zio" %% "zio" % "2.1.17",
      "org.scalameta" %% "munit" % munitVersion % Test,
      "org.postgresql" % "postgresql" % postgresDriverVersion % Test,
      "com.dimafeng" %% "testcontainers-scala-scalatest" % "0.43.0" ,
      "com.dimafeng" %% "testcontainers-scala-postgresql" % "0.43.0" ,
      "com.dimafeng" %% "testcontainers-scala-munit" %  "0.43.0" % Test,
      "org.scalameta" %% "munit" % munitVersion % Test,

      )
)