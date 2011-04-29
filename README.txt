h1. Programming Hadoop with Scala

This is a project to experiment with writing Hadoop jobs in Scala. Currently, it just uses the Java APIs, as is. Longer term, it might evolve into a more idiomatic Scala binding.

Some source and sample text data adapted from "http://archive.cloudera.com/chd/3/hadoop-0.20.2+737/mapred_tutorial.html#Source+Code":http://archive.cloudera.com/chd/3/hadoop-0.20.2+737/mapred_tutorial.html#Source+Code, also available from the Cloudera free training materials on GitHub, "https://github.com/cloudera/cloudera-training":https://github.com/cloudera/cloudera-training.

h2. Usage

h3. Install Hadoop

You'll need to install the Hadoop distribution. A good place to get Hadoop is at the Cloudera download site, "http://www.cloudera.com/downloads/":http://www.cloudera.com/downloads/. Then, do one of the following, assuming *hadoop_home* is where you installed Hadoop:

* Put *hadoop_home/bin* on your *PATH* or...
* Define the environment *HADOOP_HOME* to point to *hadoop_home*.

*Note:* at the time of this writing, only the hadoop-core-X.Y.Z-A.jar is required.

h3. Run the *setup.sh* script to put the sample data into HDFS. 

Using a bash-compatible shell, run the *setup.sh* script to import the data files into HDFS. If you get an error, check that your *HADOOP_HOME* or *PATH* is set correctly.

If you don't have a bash shell, you can run the same *hadoop dfs* commands from your windows shell.

h3. Build with SBT

*SBT* is a popular build tool for Scala. For convenience, the sbt jar file is included in the "lib" directory and a driver script is included in the root directory:

    ./sbt

h3. Run SBT Commands

After starting sbt, run these commands at the '>' prompt. The '#...' are comments.

    update   # download dependencies    
    compile  # build everything
    quit     # exit sbt

The compile step will produce several warnings about deprecated types and methods. The examples use the pre 0.20 Hadoop API, since that's what most of the available Hadoop documentation uses. The compile step should contain a "[success] Successful" message near the end of its output.

See also the *TODOs* below.

h3. Run Hadoop!

    ./run.sh

It will prompt you if the output directory already exists in HDFS; Hadoop won't write to it if it already exists. Hit return (default) to delete the directory.

You can also specify an argument to indicate the kind of mapper to use:

| Flag | Description |
| *1*, *no*, or *no-buffer* | Do no buffering in the WordCount mapper; just emit a count of 1 for each word encountered, every time it is encountered. The input text is split using String.split("\s+"), then undesired characters (like punctuation) are removed. (This last step adds significant overhead!) |
| *2*, *not*, or *no-buffer-use-tokenizer* | Do no buffering in the WordCount mapper, like the previous "no-buffer" case, but split the string using Java's StringTokenizer class. This version is roughly as efficient, but does a better job eliminating "garbage" words and characters. |
| *3*, *buffer* | In each mapper instance, buffer the total counts for each word and then emit the final counts when the mapper is "closed". (Uses the StringTokenizer approach, like "not".) |
| *4*, *buffer-flush* | Like "buffer", but also flushes and resets the count records if the number of words crosses a size threshold. |

See *Test Runs* below for a discussion of what these options mean.

h4. View the Results

    hadoop dfs -cat /word-count/output-*/part-00000 | head -20

The output will be thousands of lines. Here, I just view the first 20 with the *head* command. Note that you'll have to pick the output directory that corresponds to the kind of mapper used. The suffix after the "output-" will be one of "no-buffer", "buffer", "buffer-flush".

h2. Files

A partial list of the contents.

| Files/Directories | Description |
| all-shakespeare.txt | The text of Shakespeare's plays (from the "Cloudera training materials":https://github.com/cloudera/cloudera-training).|
| sbt | Run the Simple Build Tool (sbt) |
| setup.sh | Setup the data in HDFS |
| run.sh | Run the hadoop jobs (after building in sbt) |
| lib | Support libraries, like the sbt jar |
| lib_managed | Dependency jars |
| src | Tree for all sources |
| src/main/scala/WordCount.scala | The "main" routine. |
| src/main/scala/WordCountNoBuffering.scala | The mapper with the most naive algorithm; it emits (word,1) for every occurrence of "word". |
| src/main/scala/WordCountBuffering.scala | The mapper that counts the words and then emits the (word,N) tuples at the end of its run. |
| src/main/scala/WordCountBufferingFlushing.scala | The buffering mapper that flushes and resets the accumulated counts once they cross a certain threshold. This change increases some packets set to the reducers, but reduces the unbounded memory required for the mapper! |
| src/main/scala/WordCountReduce.scala | The reducer used for all cases. |
| target | Where build products are written |

h2. TODOs

# Add a run action to the sbt project that properly invokes hadoop.
# Add a setup action to the sbt project that puts the data files into HDFS.
# Add the use of a Combiner.
# TESTS!

h3. Test Runs

The different mappers apply different optimizations. The "no-buffer" cases simply writes a word-1 pair every time a word is encountered. This is the simplest algorithm, for the mapper, but it generates the most IO to the reducers and the largest sort and shuffle overhead.

The "buffer" case saves each word as a key in a map and increments the count of occurrences as the corresponding map value. Then, when the mapper's *close* method is called, the words and counts are sent to the output collector. This mapper minimizes the overhead for the IO to the reducers and the sort and shuffle process, but it increases the memory requirements to store the word-count map.

The "buffer-flush" case addresses the potential problem that the word-count map could consume too much memory. The solution is to flush the map of data to the output collector when the map size crosses threshold (currently hard coded in the mapper class). So, it consumes less memory, but slightly increases the overhead, as more word-count pairs will be emitted.

Here are some test results on my MacBookPro with an i7 process, SSD, and 4GB of RAM. Note that I have made some refinements since these tests were run, so they may be slightly out of date, but still correct in terms of relative magnitudes.

h4. No Buffering and Regular Expression String Splitting:

| Time (sec) | Run #1 | Run #2 | Run #3 | Ave. |
| Real | 6.851 | 6.841 | 6.841 | 6.844 |
| User | 9.554 | 9.512 | 9.563 | 9.543 |
| Sys  | 0.442 | 0.437 | 0.427 | 0.435 |

h4. No Buffering and StringTokenizer String Splitting:

| Time (sec) | Run #1 | Run #2 | Run #3 | Ave. |
| Real | 5.885 | 5.857 | 5.885 | 5.875 |
| User | 7.525 | 7.557 | 7.607 | 7.563 |
| Sys  | 0.388 | 0.410 | 0.421 | 0.406 |

h4. Buffering and Regular Expression String Splitting:

| Time (sec) | Run #1 | Run #2 | Run #3 | Ave. |
| Real | 5.835 | 5.835 | 5.838 | 5.836 |
| User | 7.884 | 7.872 | 7.885 | 7.880 |
| Sys  | 0.368 | 0.362 | 0.364 | 0.364 |

h4. Buffering and StringTokenizer String Splitting:

| Time (sec) | Run #1 | Run #2 | Run #3 | Ave. |
| Real | 3.833 | 3.832 | 3.827 | 3.830 |
| User | 5.385 | 5.324 | 5.384 | 5.334 |
| Sys  | 0.317 | 0.311 | 0.310 | 0.312 |

h4. Buffering with Flushing and Regular Expression String Splitting:

| Time (sec) | Run #1 | Run #2 | Run #3 | Ave. |
| Real | 5.836 | 5.835 | 5.831 | 5.834 |
| User | 8.672 | 8.674 | 8.706 | 8.684 |
| Sys  | 0.420 | 0.414 | 0.418 | 0.417 |

h4. Buffering with Flushing and StringTokenizer String Splitting:

| Time (sec) | Run #1 | Run #2 | Run #3 | Ave. |
| Real | 4.830 | 4.838 | 4.858 | 4.842 |
| User | 6.367 | 6.469 | 6.391 | 6.409 |
| Sys  | 0.368 | 0.376 | 0.373 | 0.372 |

The flushing was set to flush every 1000 words, so the benefit of reduced memory usage was probably minimal and the extra IO hurt performance.

There is a significant performance improving when using the StringTokenizer vs. the regular expression for splitting the text into words. This may be due in part to the fact that the regex approach parses the strings into words and then further removes non-alphanumeric characters. However, this approach does a better job isolating true words, e.g., "hello" and "hello!" become just "hello". 

h2. Notes

