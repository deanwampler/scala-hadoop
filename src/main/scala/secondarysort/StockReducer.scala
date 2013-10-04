package secondarysort

import java.util.Iterator
import org.apache.hadoop.io.{ FloatWritable, NullWritable, Text }
import org.apache.hadoop.mapred.{ MapReduceBase, OutputCollector, Reducer, Reporter }
import RecordFormatRecords._


/**
 * Reducer that converts the keys into tab-delimited date, closing-price values. 
 * If we didn't care about the output format, we could use the default Identity reducer!
 */
class StockReducer extends MapReduceBase with
    Reducer[YearYMDClose, NullWritable, Text, FloatWritable] {

  override def reduce(key: YearYMDClose, 
    ignore: Iterator[NullWritable],
    output: OutputCollector[Text, FloatWritable], 
    reporter: Reporter) = {
    reporter.incrCounter(REDUCE_RECORDS_SEEN, 1)
    output.collect(new Text(key.ymd), new FloatWritable(key.closingPrice))
  }
}