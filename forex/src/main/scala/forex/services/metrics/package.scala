package forex.services

package object metrics {
  final lazy val host = java.net.InetAddress.getLocalHost.getHostName.replace(".", "-")
  final lazy val application = "forex"
  final lazy val prefix = s"$application.$host"
  final lazy val biz = s"$prefix.biz"
  final lazy val tech = s"$prefix.tech"
}
