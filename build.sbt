lazy val root = (project in file(".")).
  settings(
    name := "multihash",
    version := "0.1",
    scalaVersion := "2.11.7",
    libraryDependencies ++= Seq(
        "org.typelevel" %% "cats" % "0.4.1",
        "org.specs2" %% "specs2-core" % "3.7" % "test",
        "org.specs2" %% "specs2-junit" % "3.7" % "test"
        )
  )
