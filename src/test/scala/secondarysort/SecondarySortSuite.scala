package secondarysort

import org.scalatest.junit.JUnitSuite
import org.junit.Test
import org.junit.Assert.assertEquals
import java.util.{ List => JList, ArrayList => JArrayList }

import org.apache.hadoop.io.{ FloatWritable, LongWritable, NullWritable, Text }
import org.apache.hadoop.mrunit.{ MapDriver, ReduceDriver }
import org.apache.hadoop.mrunit.types.{ Pair => MRPair }

/** 
 * Even though the full program sorts the tuples using a custom KeyComparator,
 * we can't configure it's use in the test, so the output will be sorted according 
 * the YearYMDClose's own comparison implementation. 
 */
class SecondarySortSuite extends JUnitSuite {

	@Test def `StockMapper parses records, filters by symbol, and outputs (YearYMDClose,NullWritable) pairs`() {
		val inputs1 = List( 
			"NASDAQ,ABXA,2011-02-02,5.0,5.1,5.2,5.3,2000,5.25",
			"NASDAQ,AAPL,2011-02-02,300.0,301.0,302.0,303.0,10000,302.0",
			"NASDAQ,ABXA,2011-02-03,5.25,5.3,5.4,5.5,1000,5.5",
			"NASDAQ,AAPL,2011-02-03,302.0,303.0,304.0,305.0,20000,304.0",
			"NASDAQ,ABXA,2012-01-02,1.0,1.1,1.2,1.3,1000,1.25",
			"NASDAQ,AAPL,2012-01-02,400.0,401.0,402.0,403.0,10000,402.0", 
			"NASDAQ,ABXA,2012-01-03,1.25,1.3,1.4,1.5,1000,1.5",
			"NASDAQ,AAPL,2012-01-03,402.0,403.0,404.0,405.0,20000,404.0")
	  .zipWithIndex
	  .map {
	  	case (line, offset) => new MRPair(new LongWritable(offset), new Text(line))
	  }

		val outputs1 = List(
			new YearYMDClose(2011, "2011-02-02", 302.0f),
			new YearYMDClose(2011, "2011-02-03", 304.0f),
			new YearYMDClose(2012, "2012-01-02", 402.0f),
			new YearYMDClose(2012, "2012-01-03", 404.0f))
		.map(y => new MRPair(y, NullWritable.get()))
		
		import scala.collection.JavaConversions._

		val inputs:  JList[MRPair[LongWritable, Text]]         = inputs1
		val outputs: JList[MRPair[YearYMDClose, NullWritable]] = outputs1

		// The mapper keys are actually wrong, but they are discarded.
		val mapper = new StockMapper()
		val mapDriver = new MapDriver[LongWritable, Text, YearYMDClose, NullWritable](mapper)
		mapper.setSymbol("AAPL")
		mapDriver.withAll(inputs).withAllOutput(outputs).runTest()
	}

	@Test def `StockReducer converts presorted (YearYMDClose,NullWritable) pairs into (ymd, closingPrice)`() {

		val nullList: JList[NullWritable] = new JArrayList[NullWritable]() 
		nullList.add(NullWritable.get()) 
		
		val inputs1 = List(
			new YearYMDClose(2011, "2011-02-02", 302.0f),
			new YearYMDClose(2011, "2011-02-03", 304.0f),
			new YearYMDClose(2012, "2012-01-02", 402.0f),
			new YearYMDClose(2012, "2012-01-03", 404.0f))
		.map(y => new MRPair(y, nullList))
		
		val outputs1 = List(
			("2011-02-02", 302.0f),
			("2011-02-03", 304.0f),
			("2012-01-02", 402.0f),
			("2012-01-03", 404.0f))
		.map {
			case (ymd, price) => new MRPair(new Text(ymd), new FloatWritable(price))
		}
		
		import scala.collection.JavaConversions._

		val inputs:  JList[MRPair[YearYMDClose, JList[NullWritable]]] = inputs1
		val outputs: JList[MRPair[Text, FloatWritable]] = outputs1
		val reducer = new StockReducer()
		val reduceDriver = new ReduceDriver[YearYMDClose, NullWritable, Text, FloatWritable](reducer)
		reduceDriver.withAll(inputs).withAllOutput(outputs).runTest()
	}
}
