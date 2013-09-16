package wordcount

import org.apache.hadoop.io.{IntWritable, LongWritable, Text}
import org.apache.hadoop.mapred.{MapReduceBase, Mapper, OutputCollector, Reporter}
import java.util.StringTokenizer

/**
 * Buffer the counts and then emit them periodically to reduce the size of the map, which
 * could exceed available memory for very large input data sets. Hence the sort and shuffle
 * overhead is a little larger than for WordCountBufferingFlushing, because there will be
 * more intermediate word-count pairs emitted. However, as implemented, the optimization
 * has no effect on small data sets, because the intermediate flushing is never invoked.
 */
object WordCountBufferingFlushing {

  class Map extends MapReduceBase with Mapper[LongWritable, Text, Text, IntWritable] {
    
    val MAX_SIZE = 1000
    var count = 0
    val words = new scala.collection.mutable.HashMap[String,Int]
    // Save the output collector so we can use it in close. Is this safe??
    var outputCollector: OutputCollector[Text, IntWritable] = _;

    def map(key: LongWritable, valueDocContents: Text, output: OutputCollector[Text, IntWritable], reporter: Reporter):Unit = {
      outputCollector = output
      val tokenizer = new StringTokenizer(valueDocContents.toString, " \t\n\r\f.,:;?!-@()[]&'\"")
      while (tokenizer.hasMoreTokens) {
        val wordString = tokenizer.nextToken
        if (wordString.length > 0) {
          increment(wordString.toLowerCase)
          count = flushIfLargerThan(count, MAX_SIZE)
        }
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
