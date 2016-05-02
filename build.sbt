lazy val root = (project in file(".")).
  settings(
    name := "multihash",
    version := "0.1",
    scalaVersion := "2.11.7",
    libraryDependencies += "org.typelevel" %% "cats" % "0.4.1"
  )
