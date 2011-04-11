import sbt._
import sbt.CompileOrder._

class WordCountProject(info: ProjectInfo) extends DefaultProject(info) {

  lazy val EmbeddedRepo = MavenRepository("Embedded Repo", (info.projectPath / "embedded-repo").asURL.toString)
  lazy val LocalMavenRepo = MavenRepository("Local Maven Repo", (Path.userHome / ".m2" / "repository").asURL.toString)
	lazy val ClouderaMavenRepo = MavenRepository("Cloudera Maven Repo", "https://repository.cloudera.com/content/repositories/releases")

	lazy val hadoopModuleConfig = ModuleConfiguration("com/cloudera/hadoop/", ClouderaMavenRepo)

  override def repositories = Set(EmbeddedRepo, LocalMavenRepo, ClouderaMavenRepo)

  lazy val hadoopCore = "com.cloudera.hadoop" % "hadoop-core" % "0.20.2-737"

  override def libraryDependencies = Set(hadoopCore)

  override def compileOptions = super.compileOptions ++
    Seq("-deprecation",
        "-unchecked",
        "-Xmigration",
        "-Xcheckinit",
        "-Xwarninit",
        "-encoding", "utf8")
        .map(x => CompileOption(x))

  override def mainClass = Some("wordcount.Driver")
}
