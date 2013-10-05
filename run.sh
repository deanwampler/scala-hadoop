#!/usr/bin/env bash
#--------------------------------------------
# Run one of the hadoop sample jobs.
#   ./run.sh -h 
# for help on the options.
# Hint: To see the hadoop command that would be invoked without actually
# invoking it, run this script as follows:
#   NOOP=echo ./run.sh [args]

root_dir=$(dirname $0)

jars=($(echo $root_dir/target/ScalaHadoop*.jar))
if [ ${#jars[@]} -ne 1 ]
then
	echo "You have no jar assembly OR too many version of it in the target directory:"
	echo "  ${jars[@]}"
	echo "Delete the oldest jar(s) or run 'sbt clean assembly'"
	exit 1
fi
export APP_JAR=${jars[0]}

root_dir=$(dirname $0)
[ "$root_dir" = "." ] && root_dir="$PWD"

help() {
cat <<EOF
usage: run.sh job [options] [which_mapper]

where the options include the following:
  job                One of the implemented MR jobs. Currently:
                       WordCount
                       SecondarySort
  -h | --help        Show this help and exit.
  --local            Use local/standalone mode.
                     This is converted into corresponding Hadoop arguments:
                       -fs file:/// -jt localhost
                     See also --hdfs
  --hdfs             Use pseudo-distributed (default) or distributed mode, depending on
                     how your installation is configured. See also the "generic" opts below.
  --input=input      Input to process. Defaults to data/<job>/input, where "<job>" will 
                     be the input job argument converted to lower case (e.g., "wordcount" 
                     from "WordCount"). (See notes in the README on input data.)
  --output=outdir    Where output goes. Defaults to 
                     data/<job>/output/$which_mapper/YYYYMMDD-hhmmss. (see --input for <job>...)
  --use-combiner     Use the reducer as a combiner. 
When running WordCount, the following values can be used for "which_mapper":
  1 | no | no-buffer  Use the most naive mapper that emits "(word,1)" pairs.
  2 | not | no-buffer-use-tokenizer   
                      Use the naive mapper, but do better text tokenization.
  3 | buffer          Use buffered counts of "(word,N)" pairs.
  4 | buffer-flush    Use buffered word counts with periodic flushing to conserve memory.
For other jobs, this option is ignored.
When running SecondarySort, you must specify a stock symbol to select for (even if your
data has data for only one symbol...)
  --symbol symbol

Any other arguments are expected to be Hadoop "generic" options, which are the following
(see also the Hadoop javadocs for "GenericOptionsParser"):
  -conf config_file          Use this configuration file.
  -D property=value          Use the given value for the property.
  -fs local|namenode:port    Specify the name node. (inferred by --local | --hdfs above)
  -jt local|jobtracker:port  Specify the job tracker. (inferred by --local | --hdfs above)
  -files file1,file2,...
     Copy the files to the MapReduce cluster (e.g., support data files).
  -libjars jar1,jar2,...
     Include the jar files in the classpath.
  -archives zip1,targz2,...
     Copy and unarchive each file on every node.
Hint: To see the hadoop command that would be invoked without actually invoking it,
run this script as follows:
  NOOP=echo $root_dir/run.sh [args]
EOF
}

job=
map_kind=
use_combiner=
stock_symbol_arg=
hdfs_root=hdfs://localhost/user/$USER/
other_args=()
fs_jt=
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
				fs_jt=
				;;
		--local)
				fs_jt="-fs file:/// -jt localhost"
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
    --use-combiner)
        use_combiner=--use-combiner
        ;;
    --symbol)
        shift
        stock_symbol=$1
        stock_symbol_arg="--symbol $1"
        ;;
		*)
      if [[ -z $job ]] 
      then
        job=$1
      else
        other_args[${#other_args[@]}]=$1
      fi
				;;
	esac
	shift
done

if [[ -z $job ]]
then
    echo "Must specify a job:"
    help
    exit 1
elif [[ $job = "WordCount" ]] && [[ -z $map_kind ]]
then
	echo "Must specify a mapper for the WordCount job:"
	help
	exit 1
else 
  job2="$(echo $job | tr [A-Z] [a-z]).$job"
fi

job_lc=$(echo $job | tr [A-Z] [a-z])
: ${input:=data/$job_lc/input}
: ${output:=data/$job_lc/output/$map_kind/$(date +'%Y%m%d-%H%M%S')}

echo "Using:"
echo "  App. Jar:     $APP_JAR"
echo "  Job:          $job"
echo "  Mapper:       $map_kind"
echo "  Combiner?     $(if [[ -n $use_combiner ]]; then echo yes; else echo no; fi)"
echo "  Input:        $input"
echo "  Output:       $output"
echo "  Local mode?   $(if [[ -n $fs_jt ]]; then echo yes; else echo no; fi)"
if [[ -z $symbol ]]
then
  echo "  Stock Symbol: $stock_symbol"
fi
echo "  Other args:   ${other_args[@]} $fs_jt"
echo "  Running:     hadoop jar $APP_JAR $job2 $fs_jt ${other_args[@]} $map_kind $use_combiner $input $output" $stock_symbol_arg
[[ -z $NOOP ]] && hadoop jar $APP_JAR $fs_jt $job2 ${other_args[@]} $map_kind $use_combiner "$input" "$output" $stock_symbol_arg

