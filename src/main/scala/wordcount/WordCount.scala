package wordcount

import org.apache.hadoop.fs.Path
import org.apache.hadoop.io.{IntWritable, Text}
import org.apache.hadoop.mapred.{FileInputFormat, FileOutputFormat, JobConf, JobClient}
import org.apache.hadoop.conf.Configured
import org.apache.hadoop.util.{GenericOptionsParser, Tool, ToolRunner}

object WordCount extends Configured with Tool {

	val HELP =
"""Usage: WordCount *which_mapper* [--use-combiner] input_directory output_directory
where *which_mapper* is one of the following options:
  1 | no | no-buffer   Simplest algorithm, but least efficient.
  2 | not | no-buffer-use-tokenizer  Like 'no', but uses a less efficient StringTokenizer, which yields more accurate results.
  3 | buffer           Buffer the counts and emit just one key-count pair for each work key. (Uses StringTokenizer.)
  4 | buffer-flush     Like 'buffer', but flushes data more often to limit memory usage.
and
  --use-combiner  Use the reducer as a combiner."""

	def main(args: Array[String]) {
		sys.exit(ToolRunner.run(WordCount, args));
	}

	override def run(args: Array[String]): Int = {
		val conf = new JobConf(this.getClass)
		conf.setJobName("Word Count without Buffering")

		// Expects run.sh to set the these environment variables.
		val scala_version = System.getenv.get("SCALA_VERSION") match {
			case null => 
				println("environment variable SCALA_VERSION not defined, using 2.9.1")
			  "2.9.1"
			case version => version
		}
		val project_version = System.getenv.get("PROJECT_VERSION") match {
			case null => 
				println("environment variable PROJECT_VERSION not defined, using 1.1")
				"1.1"
			case version => version
		}

		conf.setJar(String.format("%s/target/scala-%s/scala-hadoop_%s-%s.jar",
															System.getProperty("user.dir"),
															scala_version,
															scala_version,
															project_version))															

		val optionsParser = new GenericOptionsParser(conf, args);

		val (mapper, useCombiner, inputPath, outputPath) = 
			parseArgs(optionsParser.getRemainingArgs.toList) match {
				case Settings(Some(m), useC, Some(in), Some(out)) => (m, useC, in, out)
				case _ => sys.error("Invalid settings returned by parseArgs for input args: "+args)
			}

		FileInputFormat.addInputPath(conf, new Path(inputPath))
		FileOutputFormat.setOutputPath(conf, new Path(outputPath))

		conf.setMapperClass(mapper)
		conf.setReducerClass(classOf[Reduce])
		if (useCombiner)
			conf.setCombinerClass(classOf[Reduce])

		conf.setOutputKeyClass(classOf[Text])
		conf.setOutputValueClass(classOf[IntWritable])

		JobClient.runJob(conf)
		0
	}

	private type MapperClass = Class[_ <: org.apache.hadoop.mapred.Mapper[_, _, _, _]]
	private case class Settings(
		mapperClass: Option[MapperClass],
		useCombiner: Boolean,
		inputPath:   Option[String],
		outputPath:  Option[String])

	private def parseArgs(args: List[String]) = {
		args match {
			case ("-h" | "--help") :: tail => 
				println(HELP)
			  ToolRunner.printGenericCommandUsage(Console.out);
			  sys.exit(0)
			case _ if (args.length < 3) =>
				sys.error("Input arguments: "+args+"\n"+HELP)
			case _ => // continue
		}

		def parse(a: List[String], settings: Settings): Settings = a match {
			case Nil => settings
			case head :: tail => head match {
				case "1" | "no" | "no-buffer" => 
					parse(tail, settings.copy(mapperClass = Some(classOf[WordCountNoBuffering.Map])))
				case "2" | "not" | "no-buffer-use-tokenizer" => 
					parse(tail, settings.copy(mapperClass = Some(classOf[WordCountNoBufferingTokenization.Map])))
				case "3" | "buffer" => 
					parse(tail, settings.copy(mapperClass = Some(classOf[WordCountBuffering.Map])))
				case "4" | "buffer-flush" => 
					parse(tail, settings.copy(mapperClass = Some(classOf[WordCountBufferingFlushing.Map])))
				case "--use-combiner" => 
					parse(tail, settings.copy(useCombiner = true))
				case s => 
					if (settings.inputPath == None)
					  parse(tail, settings.copy(inputPath = Some(s)))
				  else if (settings.outputPath == None)
						parse(tail, settings.copy(outputPath = Some(s)))
				  else {
						println("Unrecognized argument '" + s + "' in input arguments: "+args+"\n"+HELP)
  					sys.exit (1)
					}
			}
		}
		parse(args, Settings(None, false, None, None)) match {
			case Settings(None, _, _, _) => sys.error ("Must specify a mapper.\n"+HELP)
			case Settings(_, _, None, _) => sys.error ("Must specify an input path.\n"+HELP)
			case Settings(_, _, _, None) => sys.error ("Must specify an output path.\n"+HELP)
			case settings => settings
		}
  }
}
