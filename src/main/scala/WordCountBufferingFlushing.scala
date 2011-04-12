package wordcount

import org.apache.hadoop.io.{IntWritable, LongWritable, Text}
import org.apache.hadoop.mapred.{MapReduceBase, Mapper, OutputCollector, Reporter}
import java.util.StringTokenizer

/**
 * Buffer the counts and then emit them at the end, reducing the pairs emitted, and hence the sort and shuffle overhead.
 * Also flushes if the count map grows beyond a certain size. However, as implemented, this has no effect
 * if only one input split is sent to this object!
 * The method <tt>mapWithRegex</tt> was used as <tt>map</tt> to for a one-time measurement of the performance with that
 * parsing option.
 */
object WordCountBufferingFlushing {

	class Map extends MapReduceBase with Mapper[LongWritable, Text, Text, IntWritable] {
		
		val MAX_SIZE = 1000
		var count = 0
		val words = new scala.collection.mutable.HashMap[String,Int]
		// Save the output collector so we can use it in close. Is this safe??
		var outputCollector: OutputCollector[Text, IntWritable] = _;

		def map2(key: LongWritable, valueDocContents: Text, output: OutputCollector[Text, IntWritable], reporter: Reporter):Unit = {
			outputCollector = output
			val tokenizer = new StringTokenizer(valueDocContents.toString, " \t\n\r\f.,:;?!-'\"")
			while (tokenizer.hasMoreTokens) {
				val wordString = tokenizer.nextToken
				if (wordString.length > 0) {
					increment(wordString)
					count = flushIfLargerThan(count, MAX_SIZE)
				}
			}
		}
		
//		def mapWithRegex(key: LongWritable, valueDocContents: Text, output: OutputCollector[Text, IntWritable], reporter: Reporter):Unit = {
		def map(key: LongWritable, valueDocContents: Text, output: OutputCollector[Text, IntWritable], reporter: Reporter):Unit = {
			outputCollector = output
			for {
				// In the Shakespeare text, there are also expressions like 
				//   As both of you--God pardon it!--have done.
				// So we also use "--" as a separator.
				wordString1 <- valueDocContents.toString.split("(\\s+|--)")  
        wordString  =  wordString1.replaceAll("[.,:;?!'\"]+", "")  // also strip out punctuation, etc.
			} {
				increment(wordString);
				count = flushIfLargerThan(count, MAX_SIZE)
			}
		}
		
		override def close() = flushIfLargerThan(1,0)
		
		protected def increment(wordString: String) = words.get(wordString) match {
			case Some(count) => words.put(wordString, count+1)
			case None =>  words.put(wordString, 1)
		}

		protected def flushIfLargerThan(count: Int, threshold: Int): Int = if (count < threshold) {
			count + 1
		} else {
			val word  = new Text()
			val count = new IntWritable(1)
			words foreach { kv => 
				word.set(kv._1)
				count.set(kv._2)
				outputCollector.collect(word, count)
			}
			words.clear
			0
		}
	}
}
