package wordcount

import org.apache.hadoop.fs.Path
import org.apache.hadoop.io.{IntWritable, Text}
import org.apache.hadoop.mapred.{FileInputFormat, FileOutputFormat, JobConf, JobClient}

object WordCount {

	// The "--hdfs-root" option applies to the driver script.
	val HELP =
"""Usage: WordCount *which_mapper* [-c | --use-combiner] input_directory output_directory
where *which_mapper* is one of the following options:
  1 | no | no-buffer   Simplest algorithm, but least efficient.
  2 | not | no-buffer-use-tokenizer  Like 'no', but uses a less efficient StringTokenizer, which yields more accurate results.
  3 | buffer           Buffer the counts and emit just one key-count pair for each work key.
  4 | buffer-flush     Like 'buffer', but flushes data more often to limit memory usage.
and
  -c | --use-combiner  Use the reducer is used as a combiner."""

	def main(args: Array[String]) {

		val (mapper, useCombiner, inputPath, outputPath) = parseArgs(args.toList) match {
			case Settings(Some(m), useC, Some(in), Some(out)) => (m, useC, in, out)
			case _ => error("Invalid settings returned by parseArgs for input args: "+args)
		}
		println(mapper.getClass.getName)

		val conf = new JobConf(this.getClass)
		conf.setJobName("Word Count without Buffering")

		FileInputFormat.addInputPath(conf, new Path(inputPath))
		FileOutputFormat.setOutputPath(conf, new Path(outputPath))

		conf.setMapperClass(mapper)
		conf.setReducerClass(classOf[Reduce])
		if (useCombiner)
			conf.setCombinerClass(classOf[Reduce])

		conf.setOutputKeyClass(classOf[Text])
		conf.setOutputValueClass(classOf[IntWritable])

		JobClient.runJob(conf)
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
			  exit(0)
			case _ if (args.length < 3) =>
				error("Input arguments: "+args+"\n"+HELP)
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
				case "-c" | "--use-combiner" => 
					parse(tail, settings.copy(useCombiner = true))
				case s => 
					if (settings.inputPath == None)
					  parse(tail, settings.copy(inputPath = Some(s)))
				  else if (settings.outputPath == None)
						parse(tail, settings.copy(outputPath = Some(s)))
				  else {
						println("Unrecognized argument '" + s + "' in input arguments: "+args+"\n"+HELP)
  					exit (1)
					}
			}
		}
		parse(args, Settings(None, false, None, None)) match {
			case Settings(None, _, _, _) => error ("Must specify a mapper.\n"+HELP)
			case Settings(_, _, None, _) => error ("Must specify an input path.\n"+HELP)
			case Settings(_, _, _, None) => error ("Must specify an output path.\n"+HELP)
			case settings => settings
		}
  }
}
