scalaVersion := "2.13.5"
libraryDependencies ++= Seq(
  "org.scalikejdbc" %% "scalikejdbc"        % "3.5.+",
  "com.h2database"  %  "h2"                 % "1.4.+",
  "org.postgresql"  %  "postgresql"         % "42.2.10",
  "ch.qos.logback"  %  "logback-classic"    % "1.2.+",

  "net.ruippeixotog" %% "scala-scraper" % "2.2.0",
  "com.github.daddykotex" %% "courier" % "3.0.0-M2a",
  "org.scala-lang.modules" %% "scala-parallel-collections" % "1.0.1",

)
