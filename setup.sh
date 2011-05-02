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

function help {
		echo "usage: $0 [--hdfs-root=root]"
    echo "The HDFS root defaults to $HOME."
		exit 0
}

hdfs_root=
while [ $# -ne 0 ]
do
		case $1 in
				-h|--help)
						help
						;;
				--hdfs-root)
						shift
						hdfs_root=$1
						;;
				--hdfs-root=*)
						hdfs_root=${1#--hdfs-root=}
						;;
		esac
		shift
done
if [ "$hdfs_root" = "" ]
then
		if [ "$HOME" = "" ]
		then
				echo "$0: Can't determine where to write the HDFS files; must define \$HOME or specify the --hdfs-root option, where the 'word-count' directories will be created."
				exit 1
		fi
		hdfs_root=$HOME
fi

echo "Creating hdfs directory $hdfs_root/word-count:"
# Set up the data directories and input file in HDFS.
hadoop dfs -mkdir $hdfs_root/word-count
hadoop dfs -mkdir $hdfs_root/word-count/input

hadoop dfs -put all-shakespeare.txt $hdfs_root/word-count/input
