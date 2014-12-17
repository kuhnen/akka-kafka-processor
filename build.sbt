homepage := Some(url("https://github.com/kuhnen/akka-kafka-processor"))

name := "processor"

startYear := Some(2014)

maintainer := "Andre Kuhnen <kuhnenm@gmail.com>"

dockerExposedPorts in Docker := Seq(1600)

dockerEntrypoint in Docker := Seq("sh", "-c", "CLUSTER_IP=`/sbin/ifconfig eth0 | grep 'inet addr:' | cut -d: -f2 | awk '{ print $1 }'` bin/processor $*")

dockerRepository := Some("kuhnen")

enablePlugins(JavaAppPackaging)
