lazy val root = (project in file(".")).
  settings(
    organization := "io.mediachain",
    name := "multihash",
    version := "0.1.0",
    scalaVersion := "2.11.7",
    libraryDependencies ++= Seq(
        "org.typelevel" %% "cats" % "0.4.1",
        "org.specs2" %% "specs2-core" % "3.7" % "test",
        "org.specs2" %% "specs2-junit" % "3.7" % "test"
        ),

    // Maven settings
    publishMavenStyle := true,
    publishArtifact in Test := false,
    publishTo := {
      val nexus = "https://oss.sonatype.org/"
      if (isSnapshot.value)
        Some("snapshots" at nexus + "content/repositories/snapshots")
      else
        Some("releases"  at nexus + "service/local/staging/deploy/maven2")
    },
    pomIncludeRepository := { _ => false },
    licenses := Seq("MIT" -> url("https://raw.githubusercontent.com/mediachain/scala-multihash/master/LICENSE")),
    homepage := Some(url("https://github.com/mediachain/scala-multihash")),
    pomExtra :=
      <scm>
        <url>git@github.com/mediachain/scala-multihash.git</url>
        <connection>scm:git:git@github.com/mediachain/scala-multihash.git</connection>
      </scm>
        <developers>
          <developer>
            <id>yusefnapora</id>
            <name>Yusef Napora</name>
            <url>http://napora.org</url>
          </developer>
        </developers>
  )
