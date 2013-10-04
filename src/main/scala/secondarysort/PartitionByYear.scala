package secondarysort

import org.apache.hadoop.io.NullWritable
import org.apache.hadoop.mapred.{ JobConf, Partitioner }

/**
 * When determining how to send key-value pairs to reducers, consider ONLY the year!
 */
class PartitionByYear extends Partitioner[YearYMDClose, NullWritable] {
    
    override def configure(job: JobConf): Unit = {}
    
    override def getPartition(
        key: YearYMDClose, 
        value: NullWritable, 
        numPartitions: Int): Int = math.abs(key.year) % numPartitions;
}
