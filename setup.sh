#!/usr/bin/env bash

which hadoop
if [ $? != 0 ]
then
	if [ "$HADOOP_HOME" = "" ]
  then
    echo "$0: Must define HADOOP_HOME or have the hadoop command on your path"
    exit 1
  fi
  export PATH=$HADOOP_HOME/bin:$PATH
fi
if [ "$HOME" = "" ]
then
  echo "$0: Must define HOME, where the 'word-count' directories will be created."
  exit 1
fi

echo "Creating hdfs directory $HOME/word-count:"
# Set up the data directories and input file in HDFS.
hadoop dfs -mkdir $HOME/word-count
hadoop dfs -mkdir $HOME/word-count/input

hadoop dfs -put all-shakespeare.txt $HOME/word-count/input
