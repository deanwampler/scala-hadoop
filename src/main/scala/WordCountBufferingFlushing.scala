package wordcount

import org.apache.hadoop.io.{IntWritable, LongWritable, Text}
import org.apache.hadoop.mapred.{MapReduceBase, Mapper, OutputCollector, Reporter}

// Flush if the count map grows beyond a certain size. Note that, as implemented, this has no effect
// if only one call is made to this object!
object WordCountBufferingFlushing {

	class Map extends MapReduceBase with Mapper[LongWritable, Text, Text, IntWritable] {
		
		val MAX_SIZE = 100000
		val words = new scala.collection.mutable.HashMap[String,Int]
		// Save the output collector so we can use it in close. Is this safe??
		var outputCollector: OutputCollector[Text, IntWritable] = _;

		def map(key: LongWritable, valueDocContents: Text, output: OutputCollector[Text, IntWritable], reporter: Reporter):Unit = {
			outputCollector = output
			for {
				// In the Shakespeare text, there are also expressions like 
				//   As both of you--God pardon it!--have done.
				// So we also use "--" as a separator.
				wordString1 <- valueDocContents.toString.split("(\\s+|--)")  
        wordString  =  wordString1.replaceAll("[.,:;?!'\"]+", "")  // also strip out punctuation, etc.
			} increment(wordString);
			flushIfLargerThan(MAX_SIZE)
		}
		
		override def close() = flushIfLargerThan(0)
		
		protected def increment(wordString: String) = words.get(wordString) match {
			case Some(count) => words.put(wordString, count+1)
			case None =>  words.put(wordString, 1)
		}

		protected def flushIfLargerThan(threshold: Int): Unit = if (words.size > threshold) {
			val word  = new Text()
			val count = new IntWritable(1)
			words foreach { kv => 
				word.set(kv._1)
				count.set(kv._2)
				outputCollector.collect(word, count)
			}
			words.clear
		}
	}
}
