package secondarysort

import org.apache.hadoop.io.{ WritableComparable, WritableComparator }

/**
 * Because our keys implement WritableComparable, we don't need a separate Comparator class
 * for the general case. However, in our specific case, we sort the keys in a particular way,
 * so specifying the comparator explicitly will tell Hadoop not to use the default comparison
 * function compareTo() that is implemented in the key class itself.
 */
class KeyComparator extends WritableComparator(classOf[YearYMDClose], true) {

  /**
   * We ignore the 2nd element of the tuple (the full date). We sort by the
   * year (1st element) ascending, then the price (3rd element) descending.
   */
  override def compare(w1: WritableComparable[_], w2: WritableComparable[_]): Int = {
    val t1 = w1.asInstanceOf[YearYMDClose]; 
    val t2 = w2.asInstanceOf[YearYMDClose]; 
    val cmp = t1.year.compareTo(t2.year); 
    if (cmp != 0) {
      cmp
    } else {
      -t1.closingPrice.compareTo(t2.closingPrice);  // REVERSE!
    }
  }
}