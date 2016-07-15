import scala.util.Properties

object Version {
  def either(environmentVariable: String, default: String): String =
    Properties.envOrElse(environmentVariable, default)

  val version = "0.1.1"

  val geotrellis  = "0.10.0"
  val scala       = either("SCALA_VERSION", "2.10.6")
  lazy val hadoop = either("SPARK_HADOOP_VERSION", "2.6.0")
  lazy val spark  = either("SPARK_VERSION", "1.6.1")
}
