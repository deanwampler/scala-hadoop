package driver

import org.apache.hadoop.util.ToolRunner
import org.apache.hadoop.conf.Configuration
import wordcount._

/**
 * The "main()" method. Currently just supports the WordCount app.
 */
object Driver {

  def main(args: Array[String]): Unit = {
    ToolRunner.run(new Configuration, WordCount, args);
  }
}
