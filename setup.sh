#!/usr/bin/env bash

root_dir=$(dirname $0)

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
    echo "The HDFS root defaults to the HDFS convention: /user/$USER."
		exit 0
}

hdfs_root=hdfs://localhost/user/$USER/
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
# If not empty, append a '/'
[ -n "$hdfs_root" -a "${hdfs_root%/}" = "$hdfs_root" ] && hdfs_root="$hdfs_root/"

echo "Creating hdfs directory ${hdfs_root}word-count:"
# Set up the data directories and input file in HDFS.
hadoop dfs -mkdir ${hdfs_root}word-count
hadoop dfs -mkdir ${hdfs_root}word-count/input

hadoop dfs -put   word-count/input/all-shakespeare.txt ${hdfs_root}word-count/input

# For hdfs testing:
hadoop dfs -mkdir ${hdfs_root}word-count/output/
hadoop dfs -mkdir ${hdfs_root}word-count/output/buffer
hadoop dfs -mkdir ${hdfs_root}word-count/output/buffer-flush
hadoop dfs -mkdir ${hdfs_root}word-count/output/no-buffer
hadoop dfs -mkdir ${hdfs_root}word-count/output/no-buffer-use-tokenizer

# For local-mode testing:
mkdir ${root_dir}/word-count/output/
mkdir ${root_dir}/word-count/output/buffer
mkdir ${root_dir}/word-count/output/buffer-flush
mkdir ${root_dir}/word-count/output/no-buffer
mkdir ${root_dir}/word-count/output/no-buffer-use-tokenizer

