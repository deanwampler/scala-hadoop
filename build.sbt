name := "scala-hadoop"

version := "1.1"

scalaVersion := "2.9.1"

scalacOptions ++= Seq("-deprecation", "-unchecked")

javacOptions  ++= Seq("-Xlint:unchecked", "-Xlint:deprecation")

resolvers ++= Seq(
	"snapshots" at "http://scala-tools.org/repo-snapshots",
	"releases"  at "http://scala-tools.org/repo-releases")

mainClass in packageBin := Some("wordcount.WordCount")

libraryDependencies ++= Seq(
	"org.apache.hadoop"       %  "hadoop-core"        % "0.20.205.0",
	"org.scala-tools.testing" %% "scalacheck"         % "1.9"   % "test",
  "org.scala-tools.testing" %  "test-interface"     % "0.5"   % "test",
  "org.specs2"              %% "specs2-scalaz-core" % "6.0.1" % "test",
  "org.specs2"              %% "specs2"             % "1.6.1" % "test",
  "org.hamcrest"            %  "hamcrest-all"       % "1.1"   % "test",
  "org.mockito"             %  "mockito-all"        % "1.8.5" % "test",
  "junit"                   %  "junit"              % "4.10"  % "test",
  "org.pegdown"             %  "pegdown"            % "1.0.2" % "test"
)
           
