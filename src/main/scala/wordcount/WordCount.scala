package wordcount

import org.apache.hadoop.fs.Path
import org.apache.hadoop.io.{IntWritable, Text}
import org.apache.hadoop.mapred.{FileInputFormat, FileOutputFormat, JobConf, JobClient}
import org.apache.hadoop.conf.Configured
import org.apache.hadoop.util.{GenericOptionsParser, Tool, ToolRunner}

// Enable existential types, which we use below in several places:
import scala.language.existentials

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

	def help(message: String = "") = {
		message match {
		  case "" => 
		  case _  => println(message)
		}
		println(HELP)
		ToolRunner.printGenericCommandUsage(Console.out)
	}

	override def run(args: Array[String]): Int = {
		val conf = new JobConf(this.getClass)
		conf.setJobName("Word Count")

		conf.setJarByClass(this.getClass)

		val optionsParser = new GenericOptionsParser(conf, args);

		val (mapper, useCombiner, inputPath, outputPath) = 
			parseArgs(optionsParser.getRemainingArgs.toList) match {
				case Right((m, useC, in, out)) => (m, useC, in, out)
				case Left(0) => sys.exit(0)
				case Left(_) => sys.error("Invalid settings returned by parseArgs for input args: "+args)
			}

		FileInputFormat.addInputPath(conf, new Path(inputPath))
		FileOutputFormat.setOutputPath(conf, new Path(outputPath))

		conf.setMapperClass(mapper)
		conf.setReducerClass(classOf[WordCountReducer])
		if (useCombiner)
			conf.setCombinerClass(classOf[WordCountReducer])

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

	private def parseArgs(args: List[String]): Either[Int,(MapperClass,Boolean,String,String)] = {
		args match {
			case ("-h" | "--help") :: tail => 
				help()
				Left(0)
			case _ if (args.length < 3) =>
				help(s"Insufficient number of input arguments: $args")
				Left(1)
			case _ => // continue
		}

		def parse(a: List[String], settings: Settings): Either[Int,Settings] = a match {
			case Nil => Right(settings)
			case head :: tail => head match {
				case "WordCount" =>  // should be first arg; this class name!
					parse(tail, settings)
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
						help(s"Unrecognized argument '$s' in input arguments: $args")
						Left(1)
					}
			}
		}
		parse(args, Settings(None, false, None, None)) match {
			case Right(Settings(None, _, _, _)) => help("Must specify a mapper."); Left(1)
			case Right(Settings(_, _, None, _)) => help("Must specify an input path."); Left(1)
			case Right(Settings(_, _, _, None)) => help("Must specify an output path."); Left(1)
			case Right(Settings(Some(m), useC, Some(in), Some(out))) => Right((m, useC, in, out))
			case Left(x) => Left(x)
		}
  }
}
