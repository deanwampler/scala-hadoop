#!/usr/bin/env bash

test "$HADOOP_HOME" != "" || ( echo "Must define $HADOOP_HOME"; exit 1 )

# Set up the data directories and input file in HDFS.
hadoop dfs -mkdir /word-count
hadoop dfs -mkdir /word-count/input

hadoop dfs -put all-shakespeare.txt /word-count/input
