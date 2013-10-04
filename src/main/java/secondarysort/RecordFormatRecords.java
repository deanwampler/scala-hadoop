package secondarysort;

/**
 * An example of a counter. We'll simply report the number of records we process 
 * in the mapper and reducer tasks. This is actually redundant information; Hadoop already
 * provides it.
 * We have to declare a Java enumeration, not a Scala enumeration.
 */
public enum RecordFormatRecords {
  NO_SYMBOL_SPECIFIED,
	RECORD_FORMAT_ERRORS,
	MAP_RECORDS_SEEN,
	REDUCE_RECORDS_SEEN
}