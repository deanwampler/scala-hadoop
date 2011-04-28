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

# Set up the data directories and input file in HDFS.
hadoop dfs -mkdir /word-count
hadoop dfs -mkdir /word-count/input

hadoop dfs -put all-shakespeare.txt /word-count/input
