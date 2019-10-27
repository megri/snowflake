val dottyVersion = "0.19.0-RC1"

lazy val root = project
  .in(file("."))
  .settings(
    name := "dotty-simple",
    version := "0.1.0",

    scalaVersion := dottyVersion,

    libraryDependencies ++= Seq(
      "com.lihaoyi" %% "requests" % "0.2.0",
      "com.lihaoyi" %% "upickle" % "0.8.0",
      "com.novocode" % "junit-interface" % "0.11" % "test",
    ).map(_.withDottyCompat(scalaVersion.value))
  )
