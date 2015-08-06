name := "EmailListener"

version := "1.0.0"

scalaVersion := "2.11.6"

mainClass := Some("com.pamu_nagarjuna.add2cal.Main")

resolvers += "Akka Snapshot Repository" at "http://repo.akka.io/snapshots/"

libraryDependencies ++= Seq("javax.mail" % "mail" % "1.4.5", "com.typesafe.akka" %% "akka-actor" % "2.4-SNAPSHOT")
