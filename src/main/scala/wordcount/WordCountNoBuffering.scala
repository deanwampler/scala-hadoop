package wordcount

import org.apache.hadoop.io.{IntWritable, LongWritable, Text}
import org.apache.hadoop.mapred.{MapReduceBase, Mapper, Reducer, OutputCollector, Reporter}
import java.util.StringTokenizer

/**
 * Simple word count mapper. It emits the key-value pair (word,1) for each word.
 * The method <tt>mapWithRegex</tt> was used as <tt>map</tt> to for a one-time
 * measurement of the performance with that parsing option.
 */
object WordCountNoBuffering {

  val one  = new IntWritable(1)
  val word = new Text     // Value will be set in a non-thread-safe way!

  class Map extends MapReduceBase with Mapper[LongWritable, Text, Text, IntWritable] {
    
    def map(key: LongWritable, valueDocContents: Text, output: OutputCollector[Text, IntWritable], reporter: Reporter):Unit = {
      val tokens = valueDocContents.toString.split("\\s+")
      for (wordString <- tokens) {
        if (wordString.length > 0) {
          word.set(wordString.toLowerCase)
          output.collect(word, one)
        }
      }
    }
    
    /**
     * This method was used temporarily as <tt>map</tt> for a one-time measurement of
     * the performance with the Regex splitting option.
     */
    def mapWithRegex(key: LongWritable, valueDocContents: Text, output: OutputCollector[Text, IntWritable], reporter: Reporter):Unit = {
      for {
        // In the Shakespeare text, there are also expressions like 
        //   As both of you--God pardon it!--have done.
        // So we also use "--" as a separator.
        wordString1 <- valueDocContents.toString.split("(\\s+|--)")  
        wordString  =  wordString1.replaceAll("[.,:;?!'\"]+", "")  // also strip out punctuation, etc.
      } {
        word.set(wordString)
        output.collect(word, one)
      }
    }
  }
}
