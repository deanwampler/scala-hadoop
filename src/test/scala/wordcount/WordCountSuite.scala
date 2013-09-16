package wordcount

import org.scalatest.junit.JUnitSuite
import org.junit.Assert._
import org.junit.Test
import java.util.{ List => JList, ArrayList => JArrayList }
import org.apache.hadoop.io.{ IntWritable, LongWritable, Text }
import org.apache.hadoop.mapred.{ Mapper, Reducer }
import org.apache.hadoop.mrunit.{ MapDriver, ReduceDriver }
import org.apache.hadoop.mrunit.types.{ Pair => MRPair }

/** 
 * Tests the WordCount examples.
 * MRUnit is used, which is a JUnit plugin. Hence, we use ScalaTest's JUnit integration.
 */
class WordCountSuite extends JUnitSuite {

  /**
   * Tests the simplest WordCount Mapper, which does no buffering of output (word,1) pairs.
   */
  @Test def `WordCountNoBuffering.Map outputs unbuffered (word,1) pairs`() {
    val mapper: Mapper[LongWritable, Text, Text, IntWritable] =
      new WordCountNoBuffering.Map();
    val mapDriver: MapDriver[LongWritable, Text, Text, IntWritable] = 
      new MapDriver[LongWritable, Text, Text, IntWritable](mapper);

    val one  = new IntWritable(1);
    val zero = new LongWritable(0L);
    mapDriver.withInput(zero, new Text("Did the Buffalo buffalo Buffalo, NY?"))
      .withOutput(new Text("did"),      one)
      .withOutput(new Text("the"),      one)
      .withOutput(new Text("buffalo"),  one)
      .withOutput(new Text("buffalo"),  one)
      .withOutput(new Text("buffalo,"), one)
      .withOutput(new Text("ny?"),      one)
      .runTest();
  }

  /**
   * Tests the simplest WordCount Mapper enhanced with better string tokenization.
   * Still outputs individual (word,1) pairs.
   */
  @Test def `WordCountNoBufferingTokenization.Map outputs unbuffered (word,1) pairs`() {
    val mapper: Mapper[LongWritable, Text, Text, IntWritable] =
      new WordCountNoBufferingTokenization.Map();
    val mapDriver: MapDriver[LongWritable, Text, Text, IntWritable] = 
      new MapDriver[LongWritable, Text, Text, IntWritable](mapper);

    val one  = new IntWritable(1);
    val zero = new LongWritable(0L);
    mapDriver.withInput(zero, new Text("Did the Buffalo buffalo Buffalo, NY?"))
      .withOutput(new Text("did"),      one)
      .withOutput(new Text("the"),      one)
      .withOutput(new Text("buffalo"),  one)
      .withOutput(new Text("buffalo"),  one)
      .withOutput(new Text("buffalo"),  one)
      .withOutput(new Text("ny"),       one)
      .runTest();
  }

  /** 
   * Tests the buffered WordCount example, which does holds the key-value pairs,
   * aggregating together the pairs for a given word. The close method outputs
   * the final (word,N) pairs. Note that this version also used the improved
   * tokenization of WordCountNoBufferingTokenization.Map.
   * MRUnit is used, which is a JUnit plugin. Hence, we use ScalaTest's JUnit integration.
   * In this case, because MRUnit doesn't run the full task API, we have to explicitly
   * call close, then check the results ourselves.
   */
  @Test def `WordCountBuffering.Map outputs buffered (word,N) pairs`() {
    val mapper: Mapper[LongWritable, Text, Text, IntWritable] =
      new WordCountBuffering.Map();
    val mapDriver: MapDriver[LongWritable, Text, Text, IntWritable] = 
      new MapDriver[LongWritable, Text, Text, IntWritable](mapper);

    val zero = new LongWritable(0L);
    val one  = new IntWritable(1);
    val two  = new IntWritable(2);
    val results: JList[MRPair[Text, IntWritable]] = 
      mapDriver.withInput(zero, new Text("Did the Buffalo buffalo Buffalo, NY?")).run()
    mapper.close()

    // Note the actual order received!!
    assert("ny"       === results.get(0).getFirst().toString())
    assert("did"      === results.get(1).getFirst().toString())
    assert("the"      === results.get(2).getFirst().toString())
    assert("buffalo"  === results.get(3).getFirst().toString())
    assert(1          === results.get(0).getSecond().get())
    assert(1          === results.get(1).getSecond().get())
    assert(1          === results.get(2).getSecond().get())
    assert(3          === results.get(3).getSecond().get())
  }

  /** 
   * Tests the buffered WordCount example that also flushes its buffers periodically,
   * a technique to avoid exhausting the Heap. As for the WordCountBuffering.Map test,
   * we have to use MRUnit carefully...
   */
  @Test def `WordCountBufferingFlushing.Map outputs buffered (word,N) pairs that are occasionally flushed`() {
    val mapper: Mapper[LongWritable, Text, Text, IntWritable] =
      new WordCountBufferingFlushing.Map();
    val mapDriver: MapDriver[LongWritable, Text, Text, IntWritable] = 
      new MapDriver[LongWritable, Text, Text, IntWritable](mapper);

    val zero = new LongWritable(0L);
    val one  = new IntWritable(1);
    val two  = new IntWritable(2);
    val results: JList[MRPair[Text, IntWritable]] = 
      mapDriver.withInput(zero, new Text("Did the Buffalo buffalo Buffalo, NY?")).run()
    mapper.close()

    // Note the actual order received!!
    assert("ny"       === results.get(0).getFirst().toString())
    assert("did"      === results.get(1).getFirst().toString())
    assert("the"      === results.get(2).getFirst().toString())
    assert("buffalo"  === results.get(3).getFirst().toString())
    assert(1          === results.get(0).getSecond().get())
    assert(1          === results.get(1).getSecond().get())
    assert(1          === results.get(2).getSecond().get())
    assert(3          === results.get(3).getSecond().get())
  }

  /**
   * Tests the Reducer used by all the examples.
   */
  @Test def `WordCountReducer aggregates word counts`() {
    val reducer: Reducer[Text, IntWritable, Text, IntWritable] = 
      new WordCountReducer();
    val reduceDriver: ReduceDriver[Text, IntWritable, Text, IntWritable] = 
      new ReduceDriver[Text, IntWritable, Text, IntWritable](reducer);
    
    val buffalo = new JArrayList[IntWritable]();
    val one = new IntWritable(1);
    buffalo.add(one);
    buffalo.add(one);
    buffalo.add(one);
    
    reduceDriver
      .withInput(new Text("buffalo"), buffalo)
      .withOutput(new Text("buffalo"), new IntWritable(3))
      .runTest();
  }
}
