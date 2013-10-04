resolvers += Classpaths.typesafeResolver

resolvers += {
  val typesafeRepoUrl = new java.net.URL("http://repo.typesafe.com/typesafe/ivy-releases")
  val pattern = Patterns(false, "[organisation]/[module]/[sbtversion]/[revision]/[type]s/[module](-[classifier])-[revision].[ext]")
  Resolver.url("Typesafe Ivy Snapshot Repository", typesafeRepoUrl)(pattern)
}

resolvers += Resolver.url("artifactory", url("http://scalasbt.artifactoryonline.com/scalasbt/sbt-plugin-releases"))(Resolver.ivyStylePatterns)

resolvers ++= Seq(
  "jgit-repo" at "http://download.eclipse.org/jgit/maven",
  "sonatype-releases"  at "http://oss.sonatype.org/content/repositories/releases"
)

addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.7.4")

addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.8.5")
