package wordcount

import org.apache.hadoop.io.{IntWritable, LongWritable, Text}
import org.apache.hadoop.mapred.{MapReduceBase, Mapper, Reducer, OutputCollector, Reporter}
import java.util.StringTokenizer

/**
 * Simple word count mapper based on WordCountNoBuffering, but it uses StringTokenizer to
 * split the string into words. Note that the results will probably be different, because the splitting algorithm is different.
 */
object WordCountNoBufferingTokenization {

  val one  = new IntWritable(1)
  val word = new Text     // Value will be set in a non-thread-safe way!

  class Map extends MapReduceBase with Mapper[LongWritable, Text, Text, IntWritable] {
    
    def map(key: LongWritable, valueDocContents: Text, output: OutputCollector[Text, IntWritable], reporter: Reporter):Unit = {
      val tokenizer = new StringTokenizer(valueDocContents.toString, " \t\n\r\f.,:;?!-@()[]&'\"")
      while (tokenizer.hasMoreTokens) {
        val wordString = tokenizer.nextToken
        if (wordString.length > 0) {
          word.set(wordString.toLowerCase)
          output.collect(word, one)
        }
      }
    }
  }
}
