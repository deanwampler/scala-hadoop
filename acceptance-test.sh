#!/usr/bin/env bash
#--------------------------------------------
# A simple "acceptance test" that verifies
# that the variations run successfully.
# Takes no arguments.
# Requires ./setup.sh to have been run first (just once)
# and "sbt package".

export root=$(dirname $0)

do_test() {
		kind=$1
		input=$root/word-count/input/
		output=$root/word-count/output/$kind/$(date +'%Y%m%d-%H%M%S')/part-00000
		golden=$root/word-count/golden/$kind/part-00000	
		run.sh $kind $input $output
		diff -q $golden $output
		if [ $? = 0 ]
		then
				echo "SUCCESS: Test for $kind passed."
		else
				echo "ERROR: Test for $kind failed!"
				echo "Run this command to see the differences:"
				echo "diff $golden $output"
				exit 1
		fi
}

for kind in no-buffer no-buffer-use-tokenizer buffer buffer-flush
do
		do_test $kind
done
