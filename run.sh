#!/usr/bin/env bash

test "$HADOOP_HOME" != "" || ( echo "Must define HADOOP_HOME"; exit 1 )

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

case $1 in
		""|1|no|no-buffer)
				map_kind=no-buffer
				;;
		2|not|no-buffer-tokens)
				map_kind=no-buffer-tokens
				;;
		3|buffer)
				map_kind=buffer
				;;
		4|buffer-flush)
				map_kind=buffer-flush
				;;
		*)
				echo "Unrecognized map specification: '1|no|no-buffer', '2|not|no-buffer-tokens', '3|buffer', or '4|buffer-flush'"
				exit 1
				;;
esac

output=/word-count/output-$map_kind
echo "Using output directory: $output"

test_and_delete_output $output

export HADOOP_CLASSPATH=target/scala_2.8.1/classes:project/boot/scala-2.8.1/lib/scala-library.jar

hadoop wordcount.WordCount "$@" /word-count/input $output

