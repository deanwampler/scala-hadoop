import sbt._
import sbt.CompileOrder._
import de.element34.sbteclipsify._

class WordCountProject(info: ProjectInfo) extends DefaultProject(info)
    with IdeaProject with Eclipsify with Exec {

  lazy val EmbeddedRepo = MavenRepository("Embedded Repo", (info.projectPath / "embedded-repo").asURL.toString)
  lazy val LocalMavenRepo = MavenRepository("Local Maven Repo", (Path.userHome / ".m2" / "repository").asURL.toString)
  lazy val MavenRepo1 = MavenRepository("Maven Repo1", "http://repo1.maven.org/maven2/")
	lazy val ClouderaMavenRepo = MavenRepository("Cloudera Maven Repo", "https://repository.cloudera.com/content/repositories/releases")
  lazy val ScalaToolsRepo = MavenRepository("Scala Tools Repo", "http://nexus.scala-tools.org/content/repositories/hosted")
  lazy val DavScalaToolsRepo = MavenRepository("Dav Scala Tools", "http://dav.scala-tools.org/repo-releases/")

  override def repositories = Set(
		EmbeddedRepo, LocalMavenRepo, ClouderaMavenRepo, MavenRepo1,
		ScalaToolsRepo, DavScalaToolsRepo)

	lazy val scalaTestModuleConfig = ModuleConfiguration("org.scalatest",   ScalaToolsRepo)

	lazy val hadoopModuleConfig = ModuleConfiguration("com/cloudera/hadoop/", ClouderaMavenRepo)

  lazy val hadoopCore = "com.cloudera.hadoop" % "hadoop-core" % "0.20.2-737"
  lazy val scalaTest = "org.scalatest" % "scalatest" % "1.2" % "test"

  override def libraryDependencies = Set(hadoopCore, scalaTest)

  override def compileOptions = super.compileOptions ++
    Seq("-deprecation",
        "-unchecked",
        "-Xmigration",
        "-Xcheckinit",
        "-Xwarninit",
        "-encoding", "utf8")
        .map(x => CompileOption(x))

  override def mainClass = Some("wordcount.Driver")

//  override def runAction = task { runHadoop() } dependsOn(compile, setupHdfsData) describedAs("run hadoop.")

}
