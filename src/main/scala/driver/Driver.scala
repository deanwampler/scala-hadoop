package driver

import wordcount._
import org.apache.hadoop.util.ToolRunner

/**
 * Provides the "main()" that drives the other applications.
 * Currently just supports WordCount.
 */
object Driver {

	def error(message: String) = {
		println(message)
		println("usage: java driver.Driver application [app_args]");
		println("where \"application\" is WordCount, ...");
		sys.error("Error!");
	}

  def main(args: Array[String]) = args.toList match {
		case Nil => 
			error("Insufficient arguments")
		case "WordCount" :: tail =>
			sys.exit(ToolRunner.run(WordCount, tail.toArray));
		case head :: tail =>
			error("Unknown application: "+head);
	}
}
