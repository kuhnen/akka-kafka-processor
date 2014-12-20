import com.typesafe.sbt.SbtMultiJvm
import com.typesafe.sbt.SbtMultiJvm.MultiJvmKeys.MultiJvm
import com.typesafe.sbt.packager.archetypes.JavaAppPackaging

homepage := Some(url("https://github.com/kuhnen/akka-kafka-processor"))

startYear := Some(2014)

maintainer := "Andre Kuhnen <kuhnenm@gmail.com>"

dockerExposedPorts in Docker := Seq(1600)

dockerEntrypoint in Docker := Seq("sh", "-c", "CLUSTER_IP=`/sbin/ifconfig eth0 | grep 'inet addr:' | cut -d: -f2 | awk '{ print $1 }'` bin/processor $*")

dockerRepository := Some("kuhnen")

compile in MultiJvm <<= (compile in MultiJvm) triggeredBy (compile in Test)
    // disable parallel tests
parallelExecution in Test := false
    // make sure that MultiJvm tests are executed by the default test target,
    // and combine the results from ordinary test and multi-jvm tests
executeTests in Test <<= (executeTests in Test, executeTests in MultiJvm) map {
      case (testResults, multiNodeResults)  =>
        val overall =
          if (testResults.overall.id < multiNodeResults.overall.id)
            multiNodeResults.overall
          else
            testResults.overall
        Tests.Output(overall,
          testResults.events ++ multiNodeResults.events,
          testResults.summaries ++ multiNodeResults.summaries)
} //config (MultiJvm)

enablePlugins(JavaAppPackaging)
