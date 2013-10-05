package secondarysort

import org.apache.hadoop.conf.Configured
import org.apache.hadoop.fs.Path
import org.apache.hadoop.io.{ FloatWritable, NullWritable, Text}
import org.apache.hadoop.mapred.{ FileInputFormat, FileOutputFormat, JobClient, JobConf }
import org.apache.hadoop.util.{ GenericOptionsParser, Tool, ToolRunner }

/**
 * Implements a Secondary Sort used to output the date and closing price of the
 * user-specified stock, but sort by year ascending, followed by closing price
 * descending. Hence, the first record for a given year will be the maximum price
 * for that year.
 * See also the secondary sort example that comes with the Hadoop distribution, and 
 * discussions in "Hadoop: The Definitive Guide" 
 */
class SecondarySort extends Configured with Tool {
  import SecondarySort._

  override def run(args: Array[String]): Int = {
    val conf = new JobConf(classOf[SecondarySort])
    conf.setJobName("Stock Analysis")
    val optionsParser = new GenericOptionsParser(conf, args)
    
    val remainingArgs = optionsParser.getRemainingArgs()
    if (remainingArgs.length < 4) {
        usage("Must specify --symbol symbol input_path output_path.")
        return 1
    } 

    var symbol = ""
    var inputPath = ""
    var outputPath = ""
    var nextIsSymbol = false;
    for (arg <- remainingArgs) {
      if (arg.startsWith("--s")) {
        nextIsSymbol = true;
      } else {
        if (nextIsSymbol) {
          conf.set("symbol", arg);
          symbol = arg;
          nextIsSymbol = false;
        } else if (inputPath.isEmpty()) {
          inputPath = arg;
        } else if (outputPath.isEmpty()) {
          outputPath = arg;
        } else {
          usage("Too many arguments specified.");
          return 1;
        }
      }
    }
    if (symbol.isEmpty()) {
      usage("Must specify '--symbol symbol' argument!")
      return 1
    }
    println("Using Stock Symbol: "+symbol);
    if (inputPath.isEmpty()) {
      usage("Must specify an input path argument!")
      return 1
    }
    if (outputPath.isEmpty()) {
      usage("Must specify an output path argument!")
      return 1
    }
      
    // Because of type erasure, the intermediate Map output K-V types and
    // final K-V types can't be inferred from the types of the mapper and 
    // reducer.
    conf.setMapOutputKeyClass(classOf[YearYMDClose])
    conf.setMapOutputValueClass(classOf[NullWritable])
    conf.setOutputKeyClass(classOf[Text])
    conf.setOutputValueClass(classOf[FloatWritable])

    FileInputFormat.addInputPath(conf, new Path(inputPath))
    FileOutputFormat.setOutputPath(conf, new Path(outputPath))

    conf.setMapperClass(classOf[StockMapper])
    
    // Experiment with not setting the reducer class, which means the default
    // "Identity Reducer" is used. Then try setting the number of reducers to zero.
    // Next try setting the number of reducers to a number between 2 and 5, say.
    // How do these changes reflect the results? Note that the local-test.sh and
    // acceptance-test.sh scripts use the "time" command to print out the 
    // running time. (More accurate times are in the Job Tracker web console.)
    conf.setReducerClass(classOf[StockReducer])
    conf.setNumReduceTasks(2)

    // Would a combiner help? Not likely, because we won't have many identical key-value pairs!
    
    // Specify our custom partitioner, etc.
    conf.setPartitionerClass(classOf[PartitionByYear])
    conf.setOutputKeyComparatorClass(classOf[KeyComparator]) 
    conf.setOutputValueGroupingComparator(classOf[GroupComparator])

    // This code was copied from the TwitterIndexer exercise we'll do later.
    // It's here so you can experiment with Indexing using MapFileOutputFormat,
    // as discussed in the lecture notes and the README. Use the Hadoop Javadocs
    // to figure out what changes you would need to make. For example, you'll need
    // to import the correct classes! However, Eclipse can handle that for you...
    // The compression settings are optional and can be omitted.
    // conf.setOutputFormat(classOf[SequenceFileOutputFormat])
    // SequenceFileOutputFormat.setCompressOutput(conf, true);
    // SequenceFileOutputFormat.setOutputCompressorClass(conf, classOf[BZip2Codec]) 
    // SequenceFileOutputFormat.setOutputCompressionType(conf, CompressionType.BLOCK)

    JobClient.runJob(conf)
    0
  }
}

object SecondarySort extends Configured {

  def usage(message: String): Unit = {
    Console.err.println(message);
    Console.err.println("usage: java ...SecondarySort [generic_options] " + 
            "--symbol stock_symbol in_path out_path");
    ToolRunner.printGenericCommandUsage(Console.err);
  }

  def main(args: Array[String]): Unit = {
    val exitCode = ToolRunner.run(new SecondarySort(), args)
    sys.exit(exitCode)
  }
}
