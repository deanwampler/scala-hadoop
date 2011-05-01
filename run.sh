#!/usr/bin/env bash
# Run one of the hadoop sample jobs.
#   ./run.sh -h 
# for help on the options.

which hadoop > /dev/null 2>&1
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

export HADOOP_CLASSPATH=target/scala_2.8.1/classes:project/boot/scala-2.8.1/lib/scala-library.jar


function help {
		hadoop wordcount.WordCount -h
		exit 0
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

map_kind=
while [ $# -ne 0 ]
do
		case $1 in
				-h|--help)
						help
						;;
				1|no|no-buffer)
						map_kind=no-buffer
						break
						;;
				2|not|no-buffer-use-tokenizer)
						map_kind=no-buffer-tokens
						break
						;;
				3|buffer)
						map_kind=buffer
						break
						;;
				4|buffer-flush)
						map_kind=buffer-flush
						break
						;;
		esac
		shift
done
if [ "$map_kind" = "" ]
then
		echo "Must specify a mapper:"
		help
fi
		
input=$HOME/word-count/input
output=$HOME/word-count/output-$map_kind
echo "Using input  directory: $input"
echo "Using output directory: $output"

test_and_delete_output $output

echo "running: hadoop wordcount.WordCount $@ $input $output"
time hadoop wordcount.WordCount "$@" $input $output

