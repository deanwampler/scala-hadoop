package wordcount

import org.apache.hadoop.fs.Path
import org.apache.hadoop.io.{IntWritable, Text}
import org.apache.hadoop.mapred.{FileInputFormat, FileOutputFormat, JobConf, JobClient}

object WordCount {

	def main(args: Array[String]): Unit = {
		assert (args.length == 3, "Usage: WordCount (1|no-buffer|2|buffer|3|buffer-flush) input_file output_file")

		val mapper = args(0) match {
			case "1" | "no" | "no-buffer" => println("Using WordCountNoBuffering.Map"); classOf[WordCountNoBuffering.Map]
			case "2" | "not" | "no-buffer-tokens" => println("Using WordCountNoBufferingTokenization.Map"); classOf[WordCountNoBufferingTokenization.Map]
			case "3" | "buffer"           => println("Using WordCountBuffering.Map"); classOf[WordCountBuffering.Map]
			case "4" | "buffer-flush"     => println("Using WordCountBufferingFlushing.Map"); classOf[WordCountBufferingFlushing.Map]
			case s => 
				println("Unrecognized argument for the kind of mapper. Must be 'no-buffer' (or just 'no'), 'no-buffer-tokens' (or just 'not'),")
			  println("  'buffer', or 'buffer-flush' or you can use 1, 2, 3, or 4, respectively.")
  			exit (1)
		}
		println(mapper.getClass.getName)
		val conf = new JobConf(this.getClass)
		conf.setJobName("Word Count without Buffering")

		FileInputFormat.addInputPath(conf, new Path(args(1)))
		FileOutputFormat.setOutputPath(conf, new Path(args(2)))

		conf.setMapperClass(mapper)
		conf.setReducerClass(classOf[Reduce])

		conf.setOutputKeyClass(classOf[Text])
		conf.setOutputValueClass(classOf[IntWritable])

		JobClient.runJob(conf)
	}

}
