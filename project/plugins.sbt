
addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "3.8")

addSbtPlugin("com.github.gseitz" % "sbt-release" % "1.0.10")

//addSbtPlugin("org.scalariform" % "sbt-scalariform" % "1.8.0")

addSbtPlugin("com.jsuereth" % "sbt-pgp" % "2.0.0")

resolvers += Resolver.sonatypeRepo("public")

