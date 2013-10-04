package secondarysort

import org.apache.hadoop.io.{ LongWritable, NullWritable, Text }
import org.apache.hadoop.mapred.{ JobConf, InvalidJobConfException, MapReduceBase, Mapper, OutputCollector, Reporter }

/**
 * The mapper parses the record, extracting the date, the year from the date, and the closing price. 
 * The input records have this format:
 *   exchange,symbol,yyyy-mm-dd,opening_price,high_price,low_price,closing_price,volume,adjusted_closing_price.
 * The mapper extracts and uses the symbol (for filtering), the yyyy-mm-dd (from which it also extracts the year),
 * and the adjusted closing price.
 */
class StockMapper extends MapReduceBase with
  Mapper[LongWritable, Text, YearYMDClose, NullWritable] {

  var symbol: Option[String] = None

  def setSymbol(newSymbol: String): Unit = {
    symbol = newSymbol match {
      case null => None
      case _ => 
        println(s"Using Stock Symbol: $newSymbol")
        Some(newSymbol)
    }
  }
  
  override def configure(jobConf: JobConf): Unit = {
    jobConf.get("symbol") match {
      case null =>  /* do nothing */
      case sym  => setSymbol(sym)
    }
  }

  override def map(
    key: LongWritable,  // offset in the file 
    line: Text,         // record on a line
    collector: OutputCollector[YearYMDClose, NullWritable], 
    reporter: Reporter): Unit = {

      val stockSymbol: String = symbol match {
        case None => 
          reporter.incrCounter(RecordFormatRecords.NO_SYMBOL_SPECIFIED, 1)
          throw new InvalidJobConfException("No stock symbol was specified!")
        case Some(s) => s
      }

      try {
        reporter.incrCounter(RecordFormatRecords.MAP_RECORDS_SEEN, 1);

        val fields = line.toString().split(",");  // CSV
        val sym = fields(1)
        if (sym == stockSymbol) { // filter!
          val date = fields(2)
          val ymd = date.split("-")
          val year = ymd(0).toInt
          // Actually use the "adjusted" close, which is the last field in the record.
          val closing = fields(fields.length-1).toFloat
          val outputTuple = new YearYMDClose(year, date, closing)
          collector.collect(outputTuple, NullWritable.get())
        }
      } catch {
        case nfe: NumberFormatException =>
          // Gets directed to the task's user syserr log.
          // You can more carefully control how messages are logged, e.g., the severity level, 
          // by using Apache Commons Logging. Log4J is actually used. The relevant log4j 
          // appender is called TLA, for "Task Log Appender", in $HADOOP_HOME/conf/log4j.properties.
          Console.err.println(nfe+": ("+key.get()+", "+line+")")

          // Example of a Counter.
          reporter.incrCounter(RecordFormatRecords.RECORD_FORMAT_ERRORS, 1)
      }
  }
}
