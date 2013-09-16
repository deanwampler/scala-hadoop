#!/usr/bin/env bash
#--------------------------------------------------------------------------------
# A simple "acceptance test" that verifies that the variations run successfully.
# Usage:
#   acceptance-test.sh [--local | --hdfs]
# Defaults to --local mode, but you can run in HDFS mode, too. However,
# you must copy the data files to HDFS first; See the README.

export root=$(dirname $0)
file_root="$root/"
mode="--local"
while [ $# -ne 0 ]
do
	case $1 in
		--loc*)
			mode="--local"
			file_root="$root/"
			;;
		--hd*)
			mode="--hdfs"
			file_root=
			;;
	esac
	shift
done

do_test() {
	kind=$1
	input=${file_root}word-count/input
	output=${file_root}word-count/output/$kind/$(date +'%Y%m%d-%H%M%S')
	golden=${file_root}word-count/golden/$kind/part-00000	
	$root/run.sh $kind $mode --input=$input --output=$output
	diff -q $golden $output/part-00000
	if [ $? = 0 ]
	then
		echo "SUCCESS: Test for $kind passed."
	else
		echo "ERROR: Test for $kind failed!"
		echo "Run this command to see the differences:"
		echo "diff $golden $output/part-00000"
		exit 1
	fi
}

for kind in no-buffer no-buffer-use-tokenizer buffer buffer-flush
do
	do_test $kind
done
