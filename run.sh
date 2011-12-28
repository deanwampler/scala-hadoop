#!/usr/bin/env bash
#--------------------------------------------
# Run one of the hadoop sample jobs.
#   ./run.sh -h 
# for help on the options.
# Because of hassles setting Hadoop's classpath correctly with the Scala 
# runtime library jar, etc. we bypass $HADOOP_HOME/bin/hadoop and drive
# hadoop ourselves. However, this isn't a full-featured replacement. 
# For example, security is ignored.
# A workaround is to copy the scala-library.jar file to $HADOOP_HOME/lib.
#
# Hint: To see the scala command that would be invoked without actually
# invoking it, run this script as follows:
#   NOOP=echo ./run.sh [args]

export APP_JAR=
for jar in target/scala-2.9*/scala-hadoop_2.9*.jar
do 
		APP_JAR=$jar
done

root_dir=$(dirname $0)
[ "$root_dir" = "." ] && root_dir="$PWD"
which hadoop > /dev/null 2>&1
if [ $? != 0 ]
then
		if [ "$HADOOP_HOME" = "" ]
		then
				echo "$0: Must define HADOOP_HOME or have the hadoop command in your path"
				exit 1
		fi
else
		HADOOP_HOME="$(dirname $(dirname $(which hadoop)))"
fi

# Hack, as described at the top of this file, we run Hadoop ourselves without
# the normal $HADOOP_HOME/bin/hadoop script.
run_wordcount() {
		if [ -f "$HADOOP_HOME/bin/hadoop-config.sh" ]
		then
				. "$HADOOP_HOME/bin/hadoop-config.sh"
		fi
    if [ -f "$HADOOP_CONF_DIR/hadoop-env.sh" ]
		then 
				. "$HADOOP_CONF_DIR/hadoop-env.sh"
		fi
		SCALA_OPTS="$SCALA_OPTS -J-Xmx512m"
		cp=.
		for f in $HADOOP_HOME/hadoop-core-*.jar $HADOOP_HOME/lib/*.jar
		do
				cp="$cp:$f"
		done
		echo "running: scala -classpath \"...:$APP_JAR\" driver.Driver WordCount $@"
		$NOOP scala -classpath "$cp:$APP_JAR" driver.Driver WordCount "$@"
}


help() {
cat <<EOF
usage:run.sh [options] which_mapper
where the options include the following:
  -h | --help        Show this help and exit.
  --local | --hdfs   Use local/standalone or (pseudo-)distributed (default) mode.
                     Builds up the appropriate path with a "file:" or "hdfs:" prefix.
  --hdfs-root=root   Root directory for HDFS (or local).
                     NOTE: use this argument AFTER --local or --hdfs.
                     For HDFS, defaults to "/user/$USER.
                     For local, defaults to "file:$PWD.
  --input=input      Input to process. Primarily for testing; see acceptance-test.sh.
                     Defaults to $hdfs_root/word-count/input
  --output=outdir    Where output goes. Primarily for testing; see acceptance-test.sh.
                     Defaults to $hdfs_root/word-count/output/$which_mapper/YYYYMMDD-hhmmss
  --use-combiner     Use the reducer as a combiner.
The following arguments correspond to "which_mapper":
  1 | no | no-buffer  Use the most naive mapper that emits "(word,1)" pairs.
  2 | not | no-buffer-use-tokenizer   Use the naive mapper, but do better text tokenization.
  3 | buffer          Use buffered counts of "(word,N)" pairs.
  4|buffer-flush      Use buffered word counts with periodic flushing to conserve memory.

Any other arguments are passed to the process. The following Hadoop "generic" 
options are also supported:
  -conf config_file          Use this configuration file.
  -D property=value          Use the given value for the property.
  -fs local|namenode:port    Specify the name node.
  -jt local|jobtracker:port  Specify the job tracker.
  -files file1,file2,...
     Copy the files to the MapReduce cluster.
  -libjars jar1,jar2,...
     Include the jar files in the classpath.
  -archives zip1,targz2,...
     Copy and unarchive each file on every node.
EOF
}

map_kind=
use_combiner=
using_combiner=false
hdfs_root=hdfs://localhost/user/$USER/
using_local_mode=false
other_args=()
while [ $# -ne 0 ]
do
		case $1 in
				-h|--help)
						help
						exit 0
						;;
				1|no|no-buffer)
						map_kind=no-buffer
						;;
				2|not|no-buffer-use-tokenizer)
						map_kind=no-buffer-use-tokenizer
						;;
				3|buffer)
						map_kind=buffer
						;;
				4|buffer-flush)
						map_kind=buffer-flush
						;;
				--hdfs)
						hdfs_root="hdfs://localhost/user/$USER/"
						using_local_mode=false
						;;
				--local)
						hdfs_root="file://$PWD/"
						using_local_mode=true
						;;
				--input)
						shift
						input=$1
						;;
				--input=*)
						input=${1#--input=}
						;;
				--output)
						shift
						output=$1
						;;
				--output=*)
						output=${1#--output=}
						;;
				--hdfs-root)
						shift
						hdfs_root=$1
						;;
				--hdfs-root=*)
						hdfs_root=${1#--hdfs-root=}
						;;
				--use-combiner)
						use_combiner=--use-combiner
						using_combiner=true
						;;
				*)
						other_args[${#other_args[@]}]=$1
						;;
		esac
		shift
done
if [ -z "$map_kind" ]
then
		echo "Must specify a mapper:"
		help
		exit 1
fi

# If not empty, append a '/'
[ -n "$hdfs_root" -a "${hdfs_root%/}" = "$hdfs_root" ] && hdfs_root="$hdfs_root/"

: ${input:=${hdfs_root}word-count/input}
: ${output:=${hdfs_root}word-count/output/$map_kind/$(date +'%Y%m%d-%H%M%S')}

echo "Using:"
echo "  Mapper:           $map_kind"
echo "  Combiner?         $using_combiner"
echo "  Input  directory: $input"
echo "  Output directory: $output"
echo "  Local mode?       $using_local_mode"
echo "  Other args:       ${other_args[@]}"

run_wordcount "${other_args[@]}" "$map_kind" $use_combiner "$input" "$output"

