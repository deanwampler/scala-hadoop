h1. Programming Hadoop with Scala

This is a project to experiment with writing Hadoop jobs in Scala. Currently, it just uses the Java APIs, as is. Longer term, it might evolve into a more idiomatic Scala binding.

h2. Usage

h3. Install Hadoop

You'll need to install the Hadoop distribution somewhere and define the environment *HADOOP_HOME* to point to the installation directory. A good place to get Hadoop is at the Cloudera download site, "http://www.cloudera.com/downloads/":http://www.cloudera.com/downloads/.

*Note:* at the time of this writing, only the hadoop-core-X.Y.Z-A.jar is required.

h3. Run the *setup.sh* script to put the sample data into HDFS.

Using a bash-compatible shell, run the *setup.sh* script to import the data files into HDFS.

h3. Build with SBT

*SBT* is a popular build tool for Scala. For convenience, the sbt jar file is included in the "lib" directory and a driver script is included in the root directory:

    ./sbt

h3. Run SBT Commands

After starting sbt, run these commands at the '>' prompt. The '#...' are comments.

    update   # download dependencies    
    compile  # build everything
    quit     # exit sbt

You'll get some warnings about deprecated types and methods. The examples use the pre 0.20 Hadoop API, since that's what most of the available Hadoop documentation uses.

See also the *TODOs* below.

h3. Run Hadoop!

    ./run.sh

It will prompt you if the output directory already exists in HDFS; Hadoop won't write to it if it already exists. Hit return (default) to delete the directory.

You can also specify an argument to indicate the kind of mapper to use:

| Flag | Description |
| *1*, *no*, or *no-buffer* | Do no buffering in the WordCount mapper; just emit a count of 1 for each word encountered, every time it is encountered. |
| *2*, *buffer* | In each mapper instance, buffer the total counts for each word and then emit the final counts when the mapper is "closed". |
| *3*, *buffer-flush* | Like "buffer", but also flushes and resets the count records if the number of words crosses a size threshold. |

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
| target | Where build products are written |

h2. TODOs

# Add a run action to the sbt project that properly invokes hadoop.
# Add a setup action to the sbt project that puts the data files into HDFS.
# TESTS!

h3. Test Runs

The different mappers apply different optimizations. The "no-buffer" case simply writes a word-1 pair every time a word is encountered. This is the simplest algorithm, for the mapper, but it generates the most IO to the reducers and the largest sort and shuffle overhead.

The "buffer" case saves each word as a key in a map and increments the count of occurrences as the corresponding map value. Then, when the mapper's *close* method is called, the words and counts are sent to the output collector. This mapper minimizes the overhead for the IO to the reducers and the sort and shuffle process, but it increases the memory requirements to store the word-count map.

The "buffer-flush" case addresses the potential problem that the word-count map could consume too much memory. It flushes the map to the output collector when the map size crosses a threshold (currently hard coded in the mapper class). So, it consumes less memory, but slightly increases the overhead, as more word-count pairs will be emitted.

Here are some test results on my MacBookPro with an i7 process, SSD, and 4GB of RAM.

h4. No Buffering:

| Time (sec) | Run #1 | Run #2 | Run #3 |
| Real | 6.851 | 6.841 | 6.841 |
| User | 9.554 | 9.512 | 9.563 |
| Sys  | 0.442 | 0.437 | 0.427 |

h4. Buffering:

| Time (sec) | Run #1 | Run #2 | Run #3 |
| Real | 5.835 | 5.835 | 5.838 | 
| User | 7.884 | 7.872 | 7.885 |
| Sys  | 0.368 | 0.362 | 0.364 |

h4. Buffering and Flushing:

| Time (sec) | Run #1 | Run #2 | Run #3 |
| Real | 5.834 | 5.834 | 5.830 | 
| User | 7.870 | 7.869 | 7.891 | 
| Sys  | 0.357 | 0.362 | 0.370 | 


The flushing had no real affect because there was probably only one input split. The flushing is only performed between splits.
