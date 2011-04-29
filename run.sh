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

function help {
		echo "usage: $0 which_mapper input_directory output_directory"
		echo "where which_mapper is one of the following options:"
		echo "  1 | no | no-buffer   Simplest algorithm, but least efficient."
		echo "  2 | not | no-buffer-use-tokenizer  Like 'no', but uses a less efficient StringTokenizer, which yields more accurate results."
		echo "  3 | buffer           Buffer the counts and emit just one key-count pair for each work key."
		echo "  4 | buffer-flush     Like 'buffer', but flushes data more often to limit memory usage."
}

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
		-h|--help)
				help
				exit 0
				;;
		1|no|no-buffer)
				map_kind=no-buffer
				;;
		2|not|no-buffer-use-tokenizer)
				map_kind=no-buffer-tokens
				;;
		3|buffer)
				map_kind=buffer
				;;
		4|buffer-flush)
				map_kind=buffer-flush
				;;
		*)
				help
				exit 1
				;;
esac

input=$HOME/word-count/input
output=$HOME/word-count/output-$map_kind
echo "Using input  directory: $input"
echo "Using output directory: $output"

test_and_delete_output $output

export HADOOP_CLASSPATH=target/scala_2.8.1/classes:project/boot/scala-2.8.1/lib/scala-library.jar

time hadoop wordcount.WordCount "$@" $input $output

