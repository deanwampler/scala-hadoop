package wordcount

import org.apache.hadoop.io.{IntWritable, Text}
import org.apache.hadoop.mapred.{MapReduceBase, Reducer, OutputCollector, Reporter}

class WordCountReducer extends MapReduceBase with Reducer[Text, IntWritable, Text, IntWritable] {

  def reduce(keyWord: Text, valuesCounts: java.util.Iterator[IntWritable], output: OutputCollector[Text, IntWritable], reporter: Reporter):Unit = {
    var totalCount = 0
    while (valuesCounts.hasNext) {
      totalCount += valuesCounts.next.get
    }
    output.collect(keyWord, new IntWritable(totalCount))
  }
}
