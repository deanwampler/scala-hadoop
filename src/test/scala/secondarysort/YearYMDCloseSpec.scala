package secondarysort

import org.scalatest.FunSpec
import org.apache.hadoop.io.{ FloatWritable, IntWritable, Text }

class YearYMDCloseSpec extends FunSpec {

  describe("YearYMDClose") {

    describe("primary constructor") {
      it("accepts Writable arguments") {
        val y = new YearYMDClose(new IntWritable(2011), new Text("2011-02-02"), new FloatWritable(302.0f))
        assert(2011 === y.year)
        assert("2011-02-02" === y.ymd)
        assert(302.0f === y.closingPrice)
      }
    }

    describe("secondary constructor") {
      it("accepts unwrapped arguments") {
        val y = new YearYMDClose(2011, "2011-02-02", 302.0f)
        assert(2011 === y.year)
        assert("2011-02-02" === y.ymd)
        assert(302.0f === y.closingPrice)
      }
    } 

    describe("default constructor") {
      it("uses 0 and \"\", as appropriate, for the values") {
        val y = new YearYMDClose()
        assert(0 === y.year)
        assert("" === y.ymd)
        assert(0.0f === y.closingPrice)
      }
    } 

    describe("compareTo sorts lexicographically, ascending") {
        val year1 = new YearYMDClose(2010, "2011-02-02", 302.0f)
        val year2 = new YearYMDClose(2011, "2011-02-02", 302.0f)
        val ymd1  = new YearYMDClose(2011, "2011-02-01", 302.0f)
        val ymd2  = new YearYMDClose(2011, "2011-02-02", 302.0f)
        val cp1   = new YearYMDClose(2011, "2011-02-02", 301.0f)
        val cp2   = new YearYMDClose(2011, "2011-02-02", 302.0f)

        assert(year1.compareTo(year1) == 0)
        assert(year1.compareTo(year2) <  0)
        assert(year2.compareTo(year1) >  0)

        assert(ymd1.compareTo(ymd1) == 0)
        assert(ymd1.compareTo(ymd2) <  0)
        assert(ymd2.compareTo(ymd1) >  0)

        assert(cp1.compareTo(cp1) == 0)
        assert(cp1.compareTo(cp2) <  0)
        assert(cp2.compareTo(cp1) >  0)
    }
  }
}
