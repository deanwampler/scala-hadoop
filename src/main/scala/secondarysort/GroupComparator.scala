package secondarysort

import org.apache.hadoop.io.{ WritableComparable, WritableComparator }

/**
 * We just consider the <em>full date</em> when grouping in the reducer.
 * If you just consider the year, you end up with just one key-value pair per year 
 * in the final output and probably not the maximum!!
 */
class GroupComparator extends WritableComparator(classOf[YearYMDClose], true) {
  
  override def compare(w1: WritableComparable[_], w2: WritableComparable[_]): Int = {
    val t1 = w1.asInstanceOf[YearYMDClose]; 
    val t2 = w2.asInstanceOf[YearYMDClose]; 
    t1.ymd.compareTo(t2.ymd); // compare the 2nd field. 
  } 
}