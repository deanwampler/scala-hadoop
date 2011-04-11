#!/usr/bin/env bash

test "$HADOOP_HOME" != "" || ( echo "Must define $HADOOP_HOME"; exit 1 )

function test_and_delete_output {
		output=$1
		hadoop dfs -test -d $output 2> /dev/null
		if [ $? = 0 ]
		then 
				echo "$output already exists. Delete? [Yn]"
				read answer
				case $answer in
						n) 
 								exit 1
								;;
						*)
								hadoop dfs -rmr $output
								;;
				esac
		fi
}

test_and_delete_output /word-count/output

export HADOOP_CLASSPATH=target/scala_2.8.1/classes:project/boot/scala-2.8.1/lib/scala-library.jar

hadoop wordcount.WordCountNoBuffering /word-count/input /word-count/output

