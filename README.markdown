# Programming Hadoop with Scala

[Dean Wampler](mailto:dean@concurrentthought.com)

This is an experiment using Scala and Hadoop's low-level Java APIs. It uses the Hadoop V0.20.20X/V1.X `org.apache.hadoop.*.mapred` part of the API, not the slightly newer, but incomplete `org.apache.hadoop.*.mapreduce`. Longer term, this project may support Hadoop V2.

Working with the low level API is quite tedious and offers the developer low productive, while depending specialized expertise to map even conceptually-straightforward algorithms to the restrictive MapReduce programming model. Beyond the complexity of MapReduce programming itself, the Hadoop API uses abstractions with mixed results. On the plus side, for example, it hides background processing like sort and shuffle, while allowing some customization of them. On the negative side, for example, you must use custom serialized types, rather than primitive and reference types, leaving the API to do the wrapping for you. Furthermore, while there are some inherent optimizations in Hadoop, like forking map tasks close to file blocks, there is no built-in "flow planner" to globally optimize the data flow, etc. 

Hence the Hadoop API is the *assembly language* of Hadoop. If you like Scala, you are far better off adopting [Scalding](https://github.com/twitter/scalding) for your general-purpose Hadoop programming and dropping to the low-level API only when necessary. Seriously, start there first. See my [Scalding-Workshop](https://github.com/deanwampler/scalding-workshop) for a quick guide to Scalding. If you need a Java toolkit, try [Cascading](http://cascading.org), on which Scalding is based. If you like Clojure, [Cascalog](http://cascalog.org) encapsulates Cascading and adds a logic-based query model.

Some source and sample text data used here were adapted from [Data Intensive Text Processing with MapReduce](http://www.umiacs.umd.edu/~jimmylin/book.html) and [Cloudera's Tutorial](http://archive.cloudera.com/chd/3/hadoop-0.20.2+737/mapred_tutorial.html#Source+Code). All code that I wrote is Apache 2 licensed; see the `LICENSE` file. See also [Hadoop: The Definitive Guide](http://shop.oreilly.com/product/0636920021773.do) and the *secondary sort* example that comes with Hadoop distributions.

## Getting Started

I've tried to make it easy to get started. You'll need a recent version of Hadoop installed to actually run the code, although the build pulls down all the dependences needed to build the application.

### Hadoop Setup

The easiest way to work with Hadoop is to install a virtual machine runner, like the one from VMWare, and download a VM from [Cloudera](http://www.cloudera.com/content/cloudera-content/cloudera-docs/DemoVMs/Cloudera-QuickStart-VM/cloudera_quickstart_vm.html), [Hortonworks](http://hortonworks.com/products/hortonworks-sandbox/), or [MapR](http://www.mapr.com/doc/display/MapR/Installing+the+MapR+Virtual+Machine). If you are on Windows, Microsoft now has a Hadoop port, but it may require Windows Server or Azure.

Alternatively, on *nix systems, you can install Hadoop directly, such as a distribution from [Apache](http://hadoop.apache.org), [Cloudera](http://www.cloudera.com/downloads/), [Hortonworks](http://hortonworks.com/download/), and [MapR](http://www.mapr.com/products/download). Note that the Hadoop YARN and V2 MapReduce API have not been tested with this project and probably don't work (TODO).

If you're installing Hadoop yourself (as opposed to using a VM), follow the directions provided with the download for configuring *pseudo-distributed* mode (single machine) or *distributed* mode (for clusters). Or, just us the "local" *standalone* mode (without HDFS), which is easiest.

### Data Setup

To avoid any potential copyright and redistribution issues, you'll need to download some data sets yourself to try these examples. 

For the Word Count example, the plays of Shakespeare are a good data set. See, e.g., [this site](http://sydney.edu.au/engineering/it/~matty/Shakespeare/). Copy one or more files to a `data/wordcount` directory (which you'll need to create), which is where the `run.sh` script (discussed below) will look for them, by default.

The Secondary Sort example assumes you have downloaded daily stock records from Yahoo!. You can also get them from Infochimps.com. Here is the [link for NASDAQ data](http://www.infochimps.com/datasets/nasdaq-exchange-daily-1970-2010-open-close-high-low-and-volume/downloads/178465). Copy one or more of the CSV files to a `data/secondarysort` directory.

Finally, if you want to test in HDFS, copy the `data` directory to HDFS (Hadoop Distributed File System), e.g.,:

	hadoop fs -put data data

If you're not familiar with HDFS, the last `data` argument actually expands to a subdirectory under your home directory within HDFS, `hdfs://<namenode:port>/user/$USER/data`, where `$USER` is your user name and `namenode:port` are the server name and port address you have configured in your `$HADOOP_HOME/conf/core-site.xml` for the `fs.default.name` property. For standalone/local-mode setups, it defaults to `localhost:9020` (or sometimes the port will be something else). 

The `data` directory, either locally or in HDFS, is where the `run.sh` script discussed below expects to find data, but you can actually put the data somewhere else, then specify `--input` and/or `--output` arguments to the `run.sh` script.

### SBT Setup

SBT is the *de facto* standard build tool for Scala applications. You'll need to install the [sbt launcher](http://www.scala-sbt.org/). The project build file (`project/Build.scala`) will pull the rest of the dependencies, including Scala 2.10.X.

The build file uses the `hadoop-core-1.1.2` Apache jar to compile the code. This could mean that subtle differences between this version and your installed Hadoop could cause strange errors when you run the application. If so, change the definition of `hadoopCore` in `project/Build.scala` to point to an appropriate Maven repo for your Hadoop distribution.

An alternative is to delete (or comment out) this line and *copy* the Hadoop core jar file from your distribution to the `lib` directory (you'll need to create `lib` first). SBT will automatically use such jars as "unmanaged" dependencies.

### Scala Setup

You *don't* need to install Scala to use this project, but if you want to explore it some more, the easiest way to install Scala is to use the [Typesafe Stack](http://typesafe.com/stack). Follow the installation instructions there.

## Building and Running the Applications

First, you'll need to compile, test, and assemble a "fat" jar that contains all the dependencies, including the Scala runtime. (The Hadoop jar files and its dependencies are not included in this jar...).

### Build with SBT

The `sbt` command should be on your path, after installing it. So, run `sbt` at your command line to put you into the interactive shell. 

Now, run these commands at the `sbt` prompt. The `#...` are comments. Expect the first command to take a **long** time the first time you run it, because it will pull down all the dependencies, including a huge number of jars that Hadoop wants. This is a one-time event.

	test      # Compile, then run the unit tests (will download dependencies first)    
	assembly  # Build the all-inclusive (or mostly...) jar file
	quit      # exit sbt

Each step should contain a `[success] ...` message at the end of its output.

### Run Hadoop!

Currently, two algorithms are implemented, the classic *Word Count* algorithm, with several variations of the *mapper*, and *Secondary Sort*, which shows how a simple SQL query becomes a nightmare of Hadoop API code. 

#### Word Count

Several variants are implemented by plugging in different mappers. They all result in the same logical behavior, but implement different optimizations (or none).

The simplest mapper outputs a separate `(word, 1)` pair for every occurrence of every word found. This uses network and disk resources inefficiently, but its simplicity means that bugs are less likely. The other mapper variants improve this usage by caching counts and emit `(word, N)` pairs, etc. The details are explained in the table below.

The `run.sh` script lets you run each configuration. Use `run.sh -h` to see the possible options. Again, you can use *local/standalone*, *pseudo-distributed*, or *distributed* mode, as desired. (The default is *pseudo-distributed* or *distributed* mode; whichever you have configured.)

	./run.sh -h   # Help message that describes the options.

We'll discuss some of the options here. The only required arguments are the job name `WordCount` and a flag that specifies the kind of mapper to use, one of the following:

| Flag (no dashes, - or --) | Description
| :--- | :----------
| `1`, `no` or `no-buffer` | Do no buffering in the WordCount mapper; just emit a count of 1 for each word encountered, every time it is encountered. The input text is split using `String.split("\s+")`, then undesired characters (like punctuation) are removed. (This last step adds significant overhead!)
| `2`, `not` or `no-buffer-use-tokenizer` | Do no buffering in the WordCount mapper, like the previous "no-buffer" case, but split the string using Java's `StringTokenizer` class. This version is roughly as efficient, but does a better job eliminating "garbage" words and characters.
| `3` or `buffer` | In each mapper instance, buffer the total counts for each word and then emit the final counts when the mapper is "closed". (Uses the `StringTokenizer` approach, like "not".)
| `4` or `buffer-flush` | Like `buffer`, but also flushes and resets the count records if the number of words crosses a size threshold.

See the **Test Runs** below for a discussion of how these options impact performance.

Other interesting options are the following:

| Options | Description
| :------ | :----------
| `--input path` | If you're not using the default `data` path discussed above.
| `--output path` | If you're not using the default `data` path discussed above or you want the output to go somewhere else.
| `--local` | Run in *local* mode, bypassing HDFS and the JobTracker. The easiest way to run these applications.
| `--use-combiner` | tells the application to use the Reducer class as a Combiner, too.

Here is an example invocation that uses the simplest mapper (argument `1`), running "locally" on your workstation:

	./run.sh WordCount --local 1

(The case of the driver class `WordCount` matters.) The input defaults to `data/wordcount/input` and the output defaults to `data/wordcount/output`. Drop the `--local` option for running in Hadoop using HDFS:

	./run.sh WordCount --local 1

If you're using HDFS, the following command will show you the results. 

	hadoop dfs -cat data/word-count/output/<kind>/part-00000 | more

Here, the `<kind>` placeholder corresponds to the mapper you used, one of `no-buffer`, `no-buffer-use-tokenizer`, `buffer`, `buffer-flush`. The parent `word-count` directory is actually in your HDFS home directory, `/user/$USER`. 

#### Secondary Sort

"Secondary sort" is what the following SQL query does:

	SELECT last_name, first_name FROM customers
	ORDER BY last_name ASC, first_name ASC

I.e., sort by one field (or other criterion) and then by a second field.

While the SQL query is conceptually straightforward, the Hadoop API implementation is not. Tools like Hive and Pig encapsulate such details very well, providing much more concise and intuitive ways to express such concepts.

When you look at the code, what you'll find is that the mapper and reducer are actually very simple, essentially just reformatting data. All the real work is by three other components plugged into the job. The goal is to achieve the secondary sort by exploiting the sort-shuffle process *between* the map and reduce steps.

* A *partitioner*, which determines which key-value pairs go to which reducers.
* A *key comparator*, which determines how to sort the keys.
* A *group comparator*, which determines how values are collected with a single key and presented to the reduce() function. (Normally, the partitioning implies that each output key corresponding to unique keys input to the reducers.)

See [Data-Intensive Text Processing with MapReduce](http://beowulf.csail.mit.edu/18.337-2012/MapReduce-book-final.pdf) and [Hadoop: The Definitive Guide](shop.oreilly.com/product/0636920021773.do) for the details of this algorithm.

Run this example with the following command:

	./run.sh SecondarySort --local --symbol AAPL

(Unlike for the Word Count example, there is only one Mapper, so no additional argument is required.)

The input defaults to `data/secondarysort/input` and the output defaults to `data/secondarysort/output`. Drop the `--local` option for running in Hadoop using HDFS:

	./run.sh SecondarySort --symbol AAPL

Use whatever stock symbol for the data you installed.

## Files

A partial list of the contents of this project:

* `run.sh`: Run the hadoop jobs (after building in `sbt`). 
* `project`: Where `sbt` build files and some support files go.
* `src`: Tree for all source files.
* `target`: Where build products are written, including the project jar file.

## Using Eclipse or IntelliJ IDEA

Project files for both IDEs can be generated by adding the corresponding SBT plugins. Add one or both of the following two lines to your `project/plugins.sbt` file, or if you don't want to modify this file or you want to use these plugins for *all* SBT projects, put them in `~/sbt/plugins/plugins.sbt`

	addSbtPlugin("com.typesafe.sbteclipse" % "sbteclipse-plugin" % "2.1.2")

	addSbtPlugin("com.github.mpeltonen" % "sbt-idea" % "1.2.0")

**SBT requires blank lines before and after each such line.**

Then, when you start `sbt`, you can generate project files with one of the following "tasks":

	gen-idea   # Generate IntelliJ IDEA project files
	eclipse    # Generate Eclipse project files

## Test Runs

This section discusses test runs of `Word Count` using the different mapper implementations.

The different mappers apply different optimizations. The **no-buffer** cases simply writes a `(word, 1)` pair every time a word is encountered. This is the simplest algorithm for the mapper, which also has the lowest mapper memory overhead, but it generates the most overhead for the sort and shuffle phase and the largest network and disk IO when moving the data from the mappers to the reducers.

The **buffer** case saves each word as a key in a map and increments the count of occurrences as the corresponding map value. Then, when the mapper's `close` method is called, the words and counts are sent to the output collector. This mapper minimizes the overhead for the IO to the reducers and the sort and shuffle process, but it increases the memory requirements to store the word-count map. For very large documents, you could run out of memory!

The **buffer-flush** case addresses the potential problem that the **buffer** implementation could consume too much memory. The solution is to flush the map of data to the output collector when the map size crosses a size threshold (currently hard coded in the mapper class). So, it consumes less memory, but it slightly increases the network and disk overhead slightly, as more word-count pairs will be emitted, and the mapper implementation is more complex.

Note that using the **no-buffer** mapper with a combiner, i.e., by passing the `--use-combiner` option to `run.sh`, is a good compromise between implementation simplicity and resource optimization.

Here are some test results on my MacBookPro with an i7 CPU, SSD, and 4GB of RAM with an early version of application. (Subsequent refinements may have affected the results.)

#### No Buffering and Regular Expression String Splitting:

| Time (sec) | Run #1 | Run #2 | Run #3 | Avg. 
| :--------- | -----: | -----: | -----: | ---:
| Real | 6.851 | 6.841 | 6.841 | 6.844
| User | 9.554 | 9.512 | 9.563 | 9.543
| Sys  | 0.442 | 0.437 | 0.427 | 0.435

#### No Buffering and StringTokenizer String Splitting:

| Time (sec) | Run #1 | Run #2 | Run #3 | Avg.
| :--------- | -----: | -----: | -----: | ---:
| Real | 5.885 | 5.857 | 5.885 | 5.875
| User | 7.525 | 7.557 | 7.607 | 7.563
| Sys  | 0.388 | 0.410 | 0.421 | 0.406

#### Buffering and Regular Expression String Splitting:

| Time (sec) | Run #1 | Run #2 | Run #3 | Avg.
| :--------- | -----: | -----: | -----: | ---:
| Real | 5.835 | 5.835 | 5.838 | 5.836
| User | 7.884 | 7.872 | 7.885 | 7.880
| Sys  | 0.368 | 0.362 | 0.364 | 0.364

#### Buffering and StringTokenizer String Splitting:

| Time (sec) | Run #1 | Run #2 | Run #3 | Avg.
| :--------- | -----: | -----: | -----: | ---:
| Real | 3.833 | 3.832 | 3.827 | 3.830
| User | 5.385 | 5.324 | 5.384 | 5.334
| Sys  | 0.317 | 0.311 | 0.310 | 0.312

#### Buffering with Flushing and Regular Expression String Splitting:

| Time (sec) | Run #1 | Run #2 | Run #3 | Avg.
| :--------- | -----: | -----: | -----: | ---:
| Real | 5.836 | 5.835 | 5.831 | 5.834
| User | 8.672 | 8.674 | 8.706 | 8.684
| Sys  | 0.420 | 0.414 | 0.418 | 0.417

#### Buffering with Flushing and StringTokenizer String Splitting:

| Time (sec) | Run #1 | Run #2 | Run #3 | Avg.
| :--------- | -----: | -----: | -----: | ---:
| Real | 4.830 | 4.838 | 4.858 | 4.842
| User | 6.367 | 6.469 | 6.391 | 6.409
| Sys  | 0.368 | 0.376 | 0.373 | 0.372

The flushing was set to flush every 1000 words, so the benefit of reduced memory usage was probably minimal and the extra IO hurt performance.

There is a significant performance improvement when using the `StringTokenizer` vs. the regular expression for splitting the text into words. Regular expression parsing is relatively slow and the implementation also removes non-alphanumeric characters from the words, after parsing. However, this implementation does a must better job isolating *true* words, e.g., "hello" and "hello!" become just "hello". Hence, we have the classic tradeoff between performance and quality ;)

Not shown are runs Using a combiner. This option was added after the test runs were made. Using a combiner with option 1 (no buffering) is slightly slower than using in-mapper buffering, because Hadoop writes the mapper output to disk and then reads it back for the combiner. However, using a combiner is a clean way to avoid the potentially excessive memory usage of in-mapper buffering. The performance impact of a combiner will also be more evident with larger data sets.

## TODOs

* Add a custom `run` task to the `sbt` project configuration that can replace (or complement) `run.sh`.
