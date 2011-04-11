package wordcount

import org.apache.hadoop.fs.Path
import org.apache.hadoop.io.{IntWritable, LongWritable, Text}
import org.apache.hadoop.mapred.{MapReduceBase, Mapper, Reducer, OutputCollector, Reporter, 
																 FileInputFormat, FileOutputFormat, JobConf, JobClient}

object WordCountNoBuffering {

	val one  = new IntWritable(1)
	val word = new Text;    // Value will be set in a non-thread-safe way!

	class Map extends MapReduceBase with Mapper[LongWritable, Text, Text, IntWritable] {
		
		def map(key: LongWritable, valueDocContents: Text, output: OutputCollector[Text, IntWritable], reporter: Reporter):Unit = {
			for {
				// In the Shakespeare text, there are also expressions like 
				//   As both of you--God pardon it!--have done.
				// So we also use "--" as a separator.
				wordString1 <- valueDocContents.toString.split("""(\s+|--)""")  
        wordString  =  wordString1.replaceAll("""[.,:;?!'"]+""", "")  // also strip out punctuation, etc.
			} {
				word.set(wordString)
				output.collect(word, one);
			}
		}
	}

	class Reduce extends MapReduceBase with Reducer[Text, IntWritable, Text, IntWritable] {

		def reduce(keyWord: Text, valuesCounts: java.util.Iterator[IntWritable], output: OutputCollector[Text, IntWritable], reporter: Reporter):Unit = {
			var totalCount = 0
			while (valuesCounts.hasNext) {
				totalCount += valuesCounts.next.get
			}
			output.collect(keyWord, new IntWritable(totalCount))
		}
	}

	def main(args: Array[String]): Unit = {
		assert (args.length == 2, "Usage: WordCountNoBuffering input_file output_file")

		val conf = new JobConf(this.getClass) // classOf[WordCountNoBuffering]
		conf.setJobName("Word Count without Buffering")

		FileInputFormat.addInputPath(conf, new Path(args(0)))
		FileOutputFormat.setOutputPath(conf, new Path(args(1)))

		conf.setMapperClass(classOf[Map])
		conf.setReducerClass(classOf[Reduce])

		conf.setOutputKeyClass(classOf[Text])
		conf.setOutputValueClass(classOf[IntWritable])

		JobClient.runJob(conf)
	}
}
