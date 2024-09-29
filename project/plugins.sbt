
addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "3.11.3")

addSbtPlugin("com.github.gseitz" % "sbt-release" % "1.0.10")

//addSbtPlugin("org.scalariform" % "sbt-scalariform" % "1.8.0")

addSbtPlugin("com.github.sbt" % "sbt-pgp" % "2.2.1")

resolvers += Resolver.sonatypeRepo("public")

