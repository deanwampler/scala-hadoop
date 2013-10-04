package secondarysort

import java.io.{ DataInput, DataOutput, IOException }
import org.apache.hadoop.io.{ FloatWritable, IntWritable, Text, WritableComparable }

case class YearYMDClose(yearW: IntWritable, ymdW: Text, closingPriceW: FloatWritable) extends WritableComparable[YearYMDClose] {

  def year: Int = yearW.get()
  def ymd: String = ymdW.toString()
  def closingPrice: Float = closingPriceW.get()

  /**
   * Convenience constructor.
   */
  def this(year: Int, ymd: String, closingPrice: Float) = 
    this(new IntWritable(year), new Text(ymd), new FloatWritable(closingPrice))

  /** 
   * You should not create an object in an invalid, e.g., uninitialized
   * state. Unfortunately, Hadoop requires a default constructor here.
   */
  def this() = this(0, "", 0.0f)

  override def write(out: DataOutput): Unit = {
    yearW.write(out);
    ymdW.write(out);
    closingPriceW.write(out);
  }

  override def readFields(in: DataInput): Unit = {
    yearW.readFields(in);
    ymdW.readFields(in);
    closingPriceW.readFields(in);
  }

  def compareTo(other: YearYMDClose): Int = {
    val diff1 = year - other.year
    if (diff1 != 0) {
      diff1
    } else {
      val diff2 = ymd.compareTo(other.ymd)
      if (diff2 != 0) {
        diff2
      } else {
        val diff3 = closingPrice - other.closingPrice
        if (diff3 > 0.000001) {
          1
        } else if (diff3 < -0.000001) {
          -1
        } else {
          0
        }
      }
    }
  }

  override def toString = s"YearYMDClose($year, $ymd, $closingPrice)($yearW, $ymdW, $closingPriceW)"
}

