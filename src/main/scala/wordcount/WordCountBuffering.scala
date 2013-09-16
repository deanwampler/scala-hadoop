package wordcount

import org.apache.hadoop.io.{IntWritable, LongWritable, Text}
import org.apache.hadoop.mapred.{MapReduceBase, Mapper, Reducer, OutputCollector, Reporter}
import java.util.StringTokenizer

/**
 * Buffer the counts and then emit them at the end, reducing the pairs emitted, and hence
 * the sort and shuffle overhead. 
 */
object WordCountBuffering {

  class Map extends MapReduceBase with Mapper[LongWritable, Text, Text, IntWritable] {
    
    val words = new scala.collection.mutable.HashMap[String,Int]
    // Save the output collector so we can use it in close. Is this safe??
    var outputCollector: OutputCollector[Text, IntWritable] = _

    def map(key: LongWritable, valueDocContents: Text, output: OutputCollector[Text, IntWritable], reporter: Reporter):Unit = {
      outputCollector = output
      val tokenizer = new StringTokenizer(valueDocContents.toString, " \t\n\r\f.,:;?!-@()[]&'\"")
      while (tokenizer.hasMoreTokens) {
        val wordString = tokenizer.nextToken
        if (wordString.length > 0) {
          increment(wordString.toLowerCase)
        }
      }
    }
    
    override def close() = {
      val word  = new Text()
      val count = new IntWritable(1)
      words foreach { kv => 
        word.set(kv._1)
        count.set(kv._2)
        outputCollector.collect(word, count)
      }
    }

    protected def increment(wordString: String) = words.get(wordString) match {
      case Some(count) => words.put(wordString, count+1)
      case None =>  words.put(wordString, 1)
    }
  }
}
