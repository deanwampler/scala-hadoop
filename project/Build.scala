import sbt._
import sbt.Keys._

object BuildSettings {

  val Name          = "ScalaHadoop"
  val Organization  = "concurrentthought.com"
  val Version       = "2.0.0"
  val ScalaVersion  = "2.10.2"
  val HadoopVersion = "1.1.2"
  val Description   = "Example MapReduce jobs written with Scala using the low-level Java API"
  val ScalacOptions = Seq("-deprecation", "-encoding", "UTF-8", "-unchecked", "-feature") //, "-explaintypes")
  val JavacOptions  = Seq("-Xlint:unchecked", "-Xlint:deprecation")

  val basicSettings = Defaults.defaultSettings ++ Seq (
    name          := Name,
    organization  := Organization,
    version       := Version,
    scalaVersion  := ScalaVersion,
    description   := Description,
    scalacOptions := ScalacOptions,
    shellPrompt   := ShellPrompt.Prompt
  )

  // sbt-assembly settings for building a fat jar.
  // Adapted from https://github.com/snowplow/scalding-example-project
  import sbtassembly.Plugin._
  import AssemblyKeys._
  lazy val sbtAssemblySettings = assemblySettings ++ Seq(

    // Slightly cleaner jar name
    jarName in assembly <<= (name, version) { (name, version) => name + "-" + version + ".jar" },
    
    // Drop most of the jars from the assembly!
    excludedJars in assembly <<= (fullClasspath in assembly) map { cp =>
      val excludes = Set(
        "scala-compiler.jar",
        "ant-1.6.5.jar",
        "jsp-api-2.1-6.1.14.jar",
        "jsp-2.1-6.1.14.jar",
        "stax-api-1.0.1.jar",
        "stax-api-1.0-2.jar",
        "jasper-compiler-5.5.12.jar",
        "minlog-1.2.jar", // Otherwise causes conflicts with Kyro (which bundles it)
        "janino-2.5.16.jar", // Janino includes a broken signature, and is not needed anyway
        "commons-beanutils-core-1.8.0.jar", // Clash with each other and with commons-collections
        "commons-beanutils-1.7.0.jar",
        "hadoop-core-%s.jar".format(BuildSettings.HadoopVersion)
      ) 
      cp filter { jar => excludes(jar.data.getName) }
    },
    
    mergeStrategy in assembly <<= (mergeStrategy in assembly) {
      (old) => {
        case "project.clj" => MergeStrategy.discard // Leiningen build files
        case x => old(x)
      }
    }
  )

  lazy val buildSettings = basicSettings ++ sbtAssemblySettings
}

// Shell prompt which show the current project,
// git branch and build version
object ShellPrompt {
  object devnull extends ProcessLogger {
    def info (s: => String) {}
    def error (s: => String) { }
    def buffer[T] (f: => T): T = f
  }
  def currBranch = (
    ("git status -sb" lines_! devnull headOption)
      getOrElse "-" stripPrefix "## "
  )

  val Prompt = {
    (state: State) => {
      val currProject = Project.extract (state).currentProject.id
      "%s:%s:%s> ".format (
        currProject, currBranch, BuildSettings.Version
      )
    }
  }
}

object Resolvers {
  val typesafe = "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"
  val sonatype =  "Sonatype Release" at "https://oss.sonatype.org/content/repositories/releases"
  val mvnrepository = "MVN Repo" at "http://mvnrepository.com/artifact"

  val allResolvers = Seq(typesafe, sonatype, mvnrepository)
}

object Dependency {

  // Include the Scala compiler itself for reification and evaluation of expressions. 
  val scalaCompiler = "org.scala-lang"    % "scala-compiler"  % BuildSettings.ScalaVersion
	val hadoopCore    = "org.apache.hadoop" % "hadoop-core"     % BuildSettings.HadoopVersion
  val scalaTest     = "org.scalatest"     % "scalatest_2.10"  % "2.0.M5b"  % "test"
  val jUnit         = "junit"             % "junit"           % "4.10"     % "test" 
  val mrUnit        = "org.apache.mrunit" % "mrunit"          % "1.0.0"    % "test" classifier "hadoop1" 
}

object Dependencies {
  import Dependency._

  val scalaHadoop = Seq(
    hadoopCore, scalaCompiler, scalaTest, jUnit, mrUnit)
}           

object ScalaHadoopBuild extends Build {
  import Resolvers._
  import Dependencies._
  import BuildSettings._

  lazy val scalaHadoop = Project(
    id = BuildSettings.Name,
    base = file("."),
    settings = buildSettings ++ Seq(
      resolvers := allResolvers,
      libraryDependencies ++= Dependencies.scalaHadoop)
  )
}
