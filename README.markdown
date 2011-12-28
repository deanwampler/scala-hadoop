# Programming Hadoop with Scala

This is an experiment with Scala and Hadoop. Currently, it just uses the Standard Java APIs as is, specifically the Hadoop V0.20.20X `org.apache.hadoop.*.mapred` part of the API. Longer term, this project might evolve into a more idiomatic Scala binding or an alternative.

Some source and sample text data used here were adapted from [Data Intensive Text Processing with MapReduce](http://www.umiacs.umd.edu/~jimmylin/book.html) and [Cloudera's Tutorial](http://archive.cloudera.com/chd/3/hadoop-0.20.2+737/mapred_tutorial.html#Source+Code).

## Usage

I've tried to make it easy to get started. You'll need a recent version of Hadoop and Scala 2.9.1 installed. Note that the build tool `sbt` grabs the corresponding jars using `sbt's` Maven and Ivy integration, but you'll still need to install these tools separately to run the applications.

### Hadoop Setup

Install either the [Cloudera CDH3 distribution](http://www.cloudera.com/downloads/) or the [Apache distribution](http://hadoop.apache.org). Only the `hadoop-core-0.20.20X` or equivalent component is actually required. Note that `sbt` uses the `hadoop-core-0.20.205.0` Apache jar to compile the code.

Follow the directions provided on either site for configuring *pseudo-distributed* or *distributed* mode, if desired. However, you can also use *standalone* mode (without HDFS), if desired.

Several "driver" Bash scripts are provided. Assuming that *hadoop_home* is the directory where you installed Hadoop, do one of the following:

* Put `hadoop_home/bin` in your `PATH` or...
* Define the environment variable `HADOOP_HOME` to point to `hadoop_home`.

### Scala Setup

The easiest way to install Scala is to use the [Typesafe Stack](http://typesafe.com/stack). Follow the installation instructions.

### Run the setup.sh script to put the sample data into HDFS. 

If you are **not** using *pseudo-distributed* or *distributed* mode, then skip to the next section, [Build with SBT].

If *pseudo-distributed* or *distributed* mode, start your cluster (or at least start HDFS). For example, run `$HADOOP_HOME/bin/start-dfs.sh`.

Using a bash-compatible shell, run the `setup.sh` script to import the data files into HDFS. If you get an error, check that your `HADOOP_HOME` or `PATH` is set correctly. By default, it will create a `word-count` directory tree under your HDFS "home" directory, unless you specify a different root using the `--hdfs-root=root` option.

	./setup.sh -h   # help describing the arguments.
	./setup.sh [--hdfs-root=root]

If you don't have a bash shell, you can run the same *hadoop dfs* commands in that script from your command line.

### Build with SBT

*SBT* is a popular build tool for Scala. For convenience, the `sbt` jar file is included in the project's `lib` directory and a driver `sbt` script is included in the root directory. (Note that we recently upgraded to `sbt` v0.11.2.)

	./sbt

Next, run these commands at the `>` prompt. The `#...` are comments.

	update   # download dependencies    
	package  # compile everything and build the jar file
	quit     # exit sbt

The `package` step should contain a `[success] ...` message at the end of its output. (See also the *TODOs* below.)

### Run Hadoop!

Currently, a classic *Word Count* algorithm is implemented, with several variations of the *mapper*. The simplest mapper implementation outputs a separate `(word, 1)` pair for every occurrence of every word found, which uses network and disk resources inefficiently. Other variations improve this usage by caching counts and emit `(word, N)` pairs, etc. The details are explained in the table below.

The `run.sh` script lets you run each configuration. Use `run.sh -h` to see the possible options. Again, you can use *local/standalone*, *pseudo-distributed*, or *distributed* mode, as desired. (The default is *(pseudo-)distributed* mode; whichever you have configured.)

	./run.sh -h   # Help message that describes the options.

*Note:* If you used the `--hdfs-root=root` option with `setup.sh` above, use the same option here. In this case, the `input_directory` will be `root/input` and the `output_directory` will be `root/output`.

We'll discuss some of the options here. The only required argument specifies the kind of mapper to use:

| Flag Synonyms ||| Description ||
| :-: | :- | :- | :---------- |
| `1` | `no` | `no-buffer` | Do no buffering in the WordCount mapper; just emit a count of 1 for each word encountered, every time it is encountered. The input text is split using String.split("\s+"), then undesired characters (like punctuation) are removed. (This last step adds significant overhead!) |
| `2` | `not` | `no-buffer-use-tokenizer` | Do no buffering in the WordCount mapper, like the previous "no-buffer" case, but split the string using Java's StringTokenizer class. This version is roughly as efficient, but does a better job eliminating "garbage" words and characters. |
| `3` | `buffer` || In each mapper instance, buffer the total counts for each word and then emit the final counts when the mapper is "closed". (Uses the StringTokenizer approach, like "not".) |
| `4` | `buffer-flush` || Like "buffer", but also flushes and resets the count records if the number of words crosses a size threshold. |

See *Test Runs* below for a discussion of how these options impact performance.

If the `--use-combiner` option is specifed, the Reducer is used as a Combiner.

The `acceptance-test.sh` script exercises the different configuration options and verifies that they work as expected. It should finish with a `SUCCESS` message if all configurations work as expected. For each configuration, it runs the application, then compares the output, which is written to a `./word-count/output/...` directory to a corresponding *golden* file under `./word-count/golden/...`

**Note:** I've had trouble using the `$HADOOP_HOME/bin/hadoop` driver script with Scala code, possibly because the sequence Hadoop uses for loading jar files doesn't let it load the Scala standard library jar before it's actually needed. (I'm speculating here...) To avoid this problem, `run.sh` does *not* use the `hadoop` driver script. Instead, it uses the `scala` driver and manages some of the required environment setup itself (but not everything handled by `hadoop`!). Feedback welcome!!

### View the Results

If you're using HDFS, the following command will show you the results. The `$root` defaults to `/user/$USER` or whatever you specified for `--hdfs-root`. The `<kind>` placeholder corresponds to the mapper you used, one of `no-buffer`, `no-buffer-use-tokenizer`, `buffer`, `buffer-flush`. 

	hadoop dfs -cat $root/word-count/output/<kind>/part-00000 | more

## Files

A partial list of the contents of this project.

| Files/Directories | Description ||
| :---------------- | :---------- |
| `sbt` | Runs the *Simple Build Tool* (sbt) |
| `setup.sh` | Sets up the data and directories in HDFS (if used). |
| `run.sh` | Run the hadoop jobs (after building in `sbt`). |
| `lib` | "Unmanaged" support libraries, like the `sbt` jar. |
| `project` | `sbt` support directory tree. |
| `src` | Tree for all sources |
| `src/main/scala/WordCount.scala` | The "main" routine. |
| `src/main/scala/WordCountNoBuffering.scala` | The mapper with the most naive algorithm; it emits (word,1) for every occurrence of "word". |
| `src/main/scala/WordCountBuffering.scala` | The mapper that counts the words and then emits the (word,N) tuples at the end of its run. |
| `src/main/scala/WordCountBufferingFlushing.scala` | The buffering mapper that flushes and resets the accumulated counts once they cross a certain threshold. This change increases some packets set to the reducers, but reduces the unbounded memory required for the mapper! |
| `src/main/scala/WordCountReduce.scala` | The reducer used for all cases. |
| `target` | Where build products are written |
| `word-count/input` | Location of the input Shakespeare text file. |
| `word-count/golden` | Location of *golden* files, used for testing. |
| `word-count/output` | Created directory; location for output when running in *local* mode. |
| `empty-site.xml` | An "empty" Hadoop configuration file Created directory; location for output when running in *local* mode. |

## Test Runs

The different mappers apply different optimizations. The **no-buffer** cases simply writes a `(word, 1)` pair every time a word is encountered. This is the simplest algorithm for the mapper, which also has the lowest mapper memory overhead, but it generates the most overhead for the sort and shuffle phase and the largest network and disk IO when moving the data from the mappers to the reducers.

The **buffer** case saves each word as a key in a map and increments the count of occurrences as the corresponding map value. Then, when the mapper's `close` method is called, the words and counts are sent to the output collector. This mapper minimizes the overhead for the IO to the reducers and the sort and shuffle process, but it increases the memory requirements to store the word-count map. For very large documents, you could run out of memory!

The **buffer-flush** case addresses the potential problem that the **buffer** implementation could consume too much memory. The solution is to flush the map of data to the output collector when the map size crosses a size threshold (currently hard coded in the mapper class). So, it consumes less memory, but it slightly increases the network and disk overhead slightly, as more word-count pairs will be emitted, and the mapper implementation is more complex.

Note that using the **no-buffer** mapper with a combiner, i.e., by passing the `--use-combiner` option to `run.sh`, is a good compromise between implementation simplicity and resource optimization.

Here are some test results on my MacBookPro with an i7 CPU, SSD, and 4GB of RAM. Note that I have made some refinements since these tests were run, so they may be slightly out of date with the latest code, but still correct in terms of relative magnitudes.

#### No Buffering and Regular Expression String Splitting:

| Time (sec) | Run #1 | Run #2 | Run #3 | Ave. ||
| :--------- | -----: | -----: | -----: | ---: |
| Real | 6.851 | 6.841 | 6.841 | 6.844 |
| User | 9.554 | 9.512 | 9.563 | 9.543 |
| Sys  | 0.442 | 0.437 | 0.427 | 0.435 |

#### No Buffering and StringTokenizer String Splitting:

| Time (sec) | Run #1 | Run #2 | Run #3 | Ave. ||
| :--------- | -----: | -----: | -----: | ---: |
| Real | 5.885 | 5.857 | 5.885 | 5.875 |
| User | 7.525 | 7.557 | 7.607 | 7.563 |
| Sys  | 0.388 | 0.410 | 0.421 | 0.406 |

#### Buffering and Regular Expression String Splitting:

| Time (sec) | Run #1 | Run #2 | Run #3 | Ave. ||
| :--------- | -----: | -----: | -----: | ---: |
| Real | 5.835 | 5.835 | 5.838 | 5.836 |
| User | 7.884 | 7.872 | 7.885 | 7.880 |
| Sys  | 0.368 | 0.362 | 0.364 | 0.364 |

#### Buffering and StringTokenizer String Splitting:

| Time (sec) | Run #1 | Run #2 | Run #3 | Ave. ||
| :--------- | -----: | -----: | -----: | ---: |
| Real | 3.833 | 3.832 | 3.827 | 3.830 |
| User | 5.385 | 5.324 | 5.384 | 5.334 |
| Sys  | 0.317 | 0.311 | 0.310 | 0.312 |

#### Buffering with Flushing and Regular Expression String Splitting:

| Time (sec) | Run #1 | Run #2 | Run #3 | Ave. ||
| :--------- | -----: | -----: | -----: | ---: |
| Real | 5.836 | 5.835 | 5.831 | 5.834 |
| User | 8.672 | 8.674 | 8.706 | 8.684 |
| Sys  | 0.420 | 0.414 | 0.418 | 0.417 |

#### Buffering with Flushing and StringTokenizer String Splitting:

| Time (sec) | Run #1 | Run #2 | Run #3 | Ave. ||
| :--------- | -----: | -----: | -----: | ---: |
| Real | 4.830 | 4.838 | 4.858 | 4.842 |
| User | 6.367 | 6.469 | 6.391 | 6.409 |
| Sys  | 0.368 | 0.376 | 0.373 | 0.372 |

The flushing was set to flush every 1000 words, so the benefit of reduced memory usage was probably minimal and the extra IO hurt performance.

There is a significant performance improvement when using the `StringTokenizer` vs. the regular expression for splitting the text into words. Regular expression parsing is relatively slow and the implementation also removes non-alphanumeric characters from the words, after parsing. However, this implementation does a must better job isolating *true* words, e.g., "hello" and "hello!" become just "hello". Hence, we have the classic tradeoff between performance and quality ;)

Not shown are runs Using a combiner. This option was added after the test runs were made. Using a combiner with option 1 (no buffering) is slightly slower than using in-mapper buffering, because Hadoop writes the mapper output to disk and then reads it back for the combiner. However, using a combiner is a clean way to avoid the potentially excessive memory usage of in-mapper buffering. The performance impact of a combiner will also be more evident with larger data sets.

## Using Eclipse or IntelliJ IDEA

Project files for both IDEs are provided, but you'll need to edit them to fix the classpaths so they point to your `~/.ivy2` repo for the Hadoop "core" jar file.

Build the code with `sbt` first, so the jar file will be loaded into your repo, then edit the appropriate project files for your IDE.

## TODOs

* Add a custom `run` task to the `sbt` project configuration that can replace (or complement) `run.sh`.
* Add a `setup` action to the `sbt` project configuration that puts the data files into HDFS, replacing the `setup.sh` script.
* Unit TESTS! (but to be fair, the `acceptance-test.sh` provides pretty solid test coverage.)

## Possible Extensions

There is a lot that can be done to experiment with the code.

* Filter out "stop words", e.g., "the", "a", "an", etc.
* Implement other text processing algorithms, e.g., *Page Rank* and graph algorithms; see [Data Intensive Text Processing with MapReduce](http://www.umiacs.umd.edu/~jimmylin/book.html) for ideas).

