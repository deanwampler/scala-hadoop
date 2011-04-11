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

See the *TODOs* below.

h3. Run Hadoop!

    ./run.sh

It will prompt you if the output directory already exists in HDFS; Hadoop won't write to it if it already exists. Hit return (default) to delete the directory.

h4. View the Results

    hadoop dfs -cat /word-count/output/part-00000 | head -20

The output will be thousands of lines. Here, I just view the first 20 with the *head* command.

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
