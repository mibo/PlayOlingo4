name := """play-java-stream"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayJava)

scalaVersion := "2.11.6"

resolvers += Resolver.mavenLocal

libraryDependencies ++= Seq(
  javaJdbc,
  cache,
  javaWs
)

lazy val olingo = "4.3.0"
//lazy val olingo = "4.2.0-SNAPSHOT"

//libraryDependencies += "org.apache.derby" % "derby" % "10.4.1.3"
libraryDependencies += "org.apache.olingo" % "odata-server-api" % olingo
libraryDependencies += "org.apache.olingo" % "odata-server-core" % olingo
libraryDependencies += "org.apache.olingo" % "odata-commons-api" % olingo
libraryDependencies += "org.apache.olingo" % "odata-commons-core" % olingo

// Play provides two styles of routers, one expects its actions to be injected, the
// other, legacy style, accesses its actions statically.
routesGenerator := InjectedRoutesGenerator
