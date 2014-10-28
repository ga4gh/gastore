Global Alliance Read Store Example
==================================

*This is a toy project* to inform a discussion on content-addressable storage at the Global Alliance. Be warned: it is alpha quality at best.

## Getting the self-executing jar file

You can just [download the latest jar file](https://github.com/massie/gastore/releases) and skip to the section on how to run, if you like; otherwise, here are the steps to building from source yourself.

1. Install [Apache Maven](http://maven.apache.org), if you haven't already.
2. Clone this repo: `git clone https://github.com/massie/gastore.git`
3. Change into the repo directory: `cd gastore`
4. Compile, run the tests and build the uber jar file by running `mvn package`
5. Run the program and check that you can see the help: `java -jar target/gastore-0.1-SNAPSHOT.jar --help`

## How to run

To run, just use `java -jar gastore-0.1-SNAPSHOT.jar [options]`, e.g.,

```
$ java -jar target/gastore-0.1-SNAPSHOT.jar --help
Usage: gastore [options]

  -i <file> | --input <file>
        The sam/bam file to convert and compute a digest on
  -s <path> | --ga_readstore_dir <path>
        Path to the Global Alliance read store data
  --help
        prints this usage text
```


This program does one thing: adds SAM or BAM files to an example Global Alliance repository. A GA repo is nothing more than a specially-organized directory.

## Algorithm

This program defines a digest for a `GAReadGroup` using the following algorithm:
* a RG's digest is a SHA1 of:
  * the header fields of the RG (name, description, creation date, sample id)
  * the `CRCAccumulator()` of all the arrays of sequence data in the RG
  * the `CRCAccumulator()` of all the arrays of quality data in the RG
  * the `CRCAccumulator()` of all alignment positions in the RG
* a `CRCAccumulator()` is a 32-bit checksum for a set of arrays
  * it's built by XOR'ing together the CRC32 for each of the arrays.
  * it's sensitive to the order of objects in each array, but not to the order in which it processes the arrays

## Tutorial

You can walk through this tutorial from the command-line if you like.

We'll add `HG00096` and `HG00097` to a GA repo. The former BAM has three read groups and the latter has two, e.g.

```
$ samtools view -H HG00096.chrom20.ILLUMINA.bwa.GBR.low_coverage.20120522.bam  | grep @RG
@RG     ID:SRR062634    LB:2845856850   SM:HG00096      PI:206  CN:WUGSC        PL:ILLUMINA     DS:SRP001294
@RG     ID:SRR062635    LB:2845856850   SM:HG00096      PI:206  CN:WUGSC        PL:ILLUMINA     DS:SRP001294
@RG     ID:SRR062641    LB:2845856850   SM:HG00096      PI:206  CN:WUGSC        PL:ILLUMINA     DS:SRP001294
$ samtools view -H HG00097.chrom20.ILLUMINA.bwa.GBR.low_coverage.20130415.bam  | grep @RG
@RG     ID:SRR741384    LB:IWG_IND-TG.HG00097-4_1pA     SM:HG00097      PI:297  CN:BCM  PL:ILLUMINA     DS:SRP001294
@RG     ID:SRR741385    LB:IWG_IND-TG.HG00097-4_1pA     SM:HG00097      PI:297  CN:BCM  PL:ILLUMINA     DS:SRP001294
```

Let's create an empty directory for our repo and save `HG00096` to it.

```
$ mkdir ga_repo
$ java -jar gastore-0.1-SNAPSHOT.jar --input HG00096.chrom20.ILLUMINA.bwa.GBR.low_coverage.20120522.bam --ga_readstore_dir ga_repo
```

A minute of two later, you should see the following output similar to...

```
Oct 24, 2014 2:37:53 PM INFO: parquet.hadoop.ColumnChunkPageWriteStore: written 19,488,810B for [alignedSequence] BINARY: 968,804 values, 100,766,368B raw, 19,135,531B comp, 1536 pages, encodings: [RLE, PLAIN, BIT_PACKED]
Oct 24, 2014 2:37:53 PM INFO: parquet.hadoop.ColumnChunkPageWriteStore: written 25,395B for [alignedQuality, array] INT32: 2,906,412 values, 1,104,491B raw, 18,589B comp, 184 pages, encodings: [RLE, PLAIN_DICTIONARY], dic { 3 entries, 12B raw, 3B comp}
Oct 24, 2014 2:37:53 PM INFO: parquet.hadoop.ColumnChunkPageWriteStore: written 47B for [nextMatePosition, referenceName] BINARY: 968,804 values, 8B raw, 28B comp, 1 pages, encodings: [RLE, PLAIN, BIT_PACKED]
Oct 24, 2014 2:37:53 PM INFO: parquet.hadoop.ColumnChunkPageWriteStore: written 47B for [nextMatePosition, position] INT64: 968,804 values, 8B raw, 28B comp, 1 pages, encodings: [RLE, PLAIN, BIT_PACKED]
Oct 24, 2014 2:37:53 PM INFO: parquet.hadoop.ColumnChunkPageWriteStore: written 47B for [nextMatePosition, reverseStrand] BOOLEAN: 968,804 values, 8B raw, 28B comp, 1 pages, encodings: [RLE, PLAIN, BIT_PACKED]
Oct 24, 2014 2:37:53 PM INFO: parquet.hadoop.ColumnChunkPageWriteStore: written 50B for [info, map, key] BINARY: 968,804 values, 16B raw, 31B comp, 1 pages, encodings: [RLE, PLAIN]
Oct 24, 2014 2:37:53 PM INFO: parquet.hadoop.ColumnChunkPageWriteStore: written 50B for [info, map, value, array] BINARY: 968,804 values, 16B raw, 31B comp, 1 pages, encodings: [RLE, PLAIN]
```

This is Parquet output showing the compression techniques used and there effect on data size (Parquet files are lossless and smaller than BAM files).

Looking in the GA repo, we find...

```
$ find ga_repo/
ga_repo/
ga_repo//readGroups
ga_repo//readGroups/1D
ga_repo//readGroups/1D/F0C66B14C6F0FE13A63395580EAB8BDE8B17FF
ga_repo//readGroups/1D/F0C66B14C6F0FE13A63395580EAB8BDE8B17FF/.reads.crc
ga_repo//readGroups/1D/F0C66B14C6F0FE13A63395580EAB8BDE8B17FF/readGroupInfo
ga_repo//readGroups/1D/F0C66B14C6F0FE13A63395580EAB8BDE8B17FF/reads
ga_repo//readGroups/81
ga_repo//readGroups/81/8F16439FB25A050D91F693B113F09ACFCE938D
ga_repo//readGroups/81/8F16439FB25A050D91F693B113F09ACFCE938D/.reads.crc
ga_repo//readGroups/81/8F16439FB25A050D91F693B113F09ACFCE938D/readGroupInfo
ga_repo//readGroups/81/8F16439FB25A050D91F693B113F09ACFCE938D/reads
ga_repo//readGroups/F2
ga_repo//readGroups/F2/20A849274E6DEAE66DCA5C6043A6DAEA154CA9
ga_repo//readGroups/F2/20A849274E6DEAE66DCA5C6043A6DAEA154CA9/.reads.crc
ga_repo//readGroups/F2/20A849274E6DEAE66DCA5C6043A6DAEA154CA9/readGroupInfo
ga_repo//readGroups/F2/20A849274E6DEAE66DCA5C6043A6DAEA154CA9/reads
ga_repo//staging
ga_repo//staging/1414186600183--5828974329906361401
```

There are three read groups from `HG00096` in the repo stored by their associated digest (a SHA-1). The `readGroupInfo` is encoded as JSON and the `reads` are stored as Parquet.

```
$ cat ga_repo//readGroups/F2/20A849274E6DEAE66DCA5C6043A6DAEA154CA9/readGroupInfo
{
  "id" : "SRR062641",
  "datasetId" : {
    "string" : "SRR062641"
  },
  "name" : {
    "string" : "SRR062641"
  },
  "description" : {
    "string" : "SRP001294"
  },
  "sampleId" : {
    "string" : "HG00096"
  },
  "experiment" : null,
  "predictedInsertSize" : {
    "int" : 206
  },
  "created" : {
    "long" : 0
  },
  "updated" : {
    "long" : 0
  },
  "programs" : [ ],
  "referenceSetId" : null,
  "info" : { }
}
```

Let's add `HG00097` to the repo now.

```
java -jar gastore-0.1-SNAPSHOT.jar --input HG00097.chrom20.ILLUMINA.bwa.GBR.low_coverage.20130415.bam --ga_readstore_dir ga_repo
```

After the program finishes, you'll see we five read groups in the repo...

```
$ find ga_repo/
ga_repo/
ga_repo//readGroups
ga_repo//readGroups/1D
ga_repo//readGroups/1D/F0C66B14C6F0FE13A63395580EAB8BDE8B17FF
ga_repo//readGroups/1D/F0C66B14C6F0FE13A63395580EAB8BDE8B17FF/.reads.crc
ga_repo//readGroups/1D/F0C66B14C6F0FE13A63395580EAB8BDE8B17FF/readGroupInfo
ga_repo//readGroups/1D/F0C66B14C6F0FE13A63395580EAB8BDE8B17FF/reads
ga_repo//readGroups/45
ga_repo//readGroups/45/FEFC0240C97495DCC40BC4E9B2517DA687DACD
ga_repo//readGroups/45/FEFC0240C97495DCC40BC4E9B2517DA687DACD/.reads.crc
ga_repo//readGroups/45/FEFC0240C97495DCC40BC4E9B2517DA687DACD/readGroupInfo
ga_repo//readGroups/45/FEFC0240C97495DCC40BC4E9B2517DA687DACD/reads
ga_repo//readGroups/81
ga_repo//readGroups/81/8F16439FB25A050D91F693B113F09ACFCE938D
ga_repo//readGroups/81/8F16439FB25A050D91F693B113F09ACFCE938D/.reads.crc
ga_repo//readGroups/81/8F16439FB25A050D91F693B113F09ACFCE938D/readGroupInfo
ga_repo//readGroups/81/8F16439FB25A050D91F693B113F09ACFCE938D/reads
ga_repo//readGroups/F2
ga_repo//readGroups/F2/20A849274E6DEAE66DCA5C6043A6DAEA154CA9
ga_repo//readGroups/F2/20A849274E6DEAE66DCA5C6043A6DAEA154CA9/.reads.crc
ga_repo//readGroups/F2/20A849274E6DEAE66DCA5C6043A6DAEA154CA9/readGroupInfo
ga_repo//readGroups/F2/20A849274E6DEAE66DCA5C6043A6DAEA154CA9/reads
ga_repo//readGroups/F6
ga_repo//readGroups/F6/1740F2A50CB38E53E3712D9754690036594ED0
ga_repo//readGroups/F6/1740F2A50CB38E53E3712D9754690036594ED0/.reads.crc
ga_repo//readGroups/F6/1740F2A50CB38E53E3712D9754690036594ED0/readGroupInfo
ga_repo//readGroups/F6/1740F2A50CB38E53E3712D9754690036594ED0/reads
ga_repo//staging
ga_repo//staging/1414186600183--5828974329906361401
ga_repo//staging/1414187874570--5444760301337977224
```

Note: The `staging` directories hold the intermediate data as it is being converted and a CRC generated. There are two staging directories because we added two files.

If you try to add `HG00097` a second time, you will get the following `Directory not empty` error,

```
Exception in thread "main" java.nio.file.FileSystemException: ga_repo/staging/1414188183754--6468335609415945395/1 -> ga_repo/readGroups/F6/1740F2A50CB38E53E3712D9754690036594ED0: Directory not empty
```

This design prevents duplicate data from making it into the GA repo.

## ReadGroupSet

There is no support for a read group *set* but they are simpler object having only read group children.

## Hadoop and Spark

This code could easily be modified to run on top of Apache Spark and Hadoop. Instead of writing to a local filesystem, the GA repo data would be held on HDFS. The conversion, CRC and digest calculations would be done in parallel as a Spark job.

Storing the data on HDFS also means that we can sync the records between sites using the Hadoop `distcp` ("distributed copy") utilty which is similar to rsync.

## Global Alliance/Parquet files vs. compressed BAM files

The Global Alliance `GAReadAlignment` is analogous to a `SAMRecord` object. This application fetches `SAMRecords` from the input BAM file, convert it to a `GAReadAlignment` and then stores it in a column-oriented Parquet file.

The Global Alliance/Parquet format is 20-25% smaller than a compressed BAM file using lossless compression techniques.

For example, `NA21144.chrom11.ILLUMINA.bwa.GIH.low_coverage.20130415.bam` is 1GB in size but only take 790M when stored in Parquet, e.g.

```
$ rm -rf ga_repo
$ java -jar /workspace/berkeley/gafile/target/gastore-0.1-SNAPSHOT.jar --input NA21144.chrom11.ILLUMINA.bwa.GIH.low_coverage.20130415.bam --ga_readstore_dir ga_repo
$ ls -lh NA21144.chrom11.ILLUMINA.bwa.GIH.low_coverage.20130415.bam
-rw-r--r--@ 1 matt  staff   1.0G Oct 28 15:08 NA21144.chrom11.ILLUMINA.bwa.GIH.low_coverage.20130415.bam
$ du -sh ga_repo/
790M    ga_repo/
```

Note that Parquet was configured with a 1MB page size and GZIP compression.

Parquet has utilities for analyzing column storage, e.g.

```
$ java -cp gastore-0.1-SNAPSHOT.jar parquet.hadoop.PrintFooter ga_repo//readGroups/81/53C6BC115EF22CFEFEDC0AD472D96101659542/reads
```

Here is the raw data output from `PrintFooter` (with all columns that take <1% of the total space removed)...

```
[fragmentName] BINARY 4.5% of all space [PLAIN, BIT_PACKED, PLAIN_DICTIONARY] min: 7.08M max: 11.738M average: 9.409M total: 18.818M (raw data: 108.706M saving 82%)
  values: min: 1.888M max: 3.134M average: 2.511M total: 5.023M
  uncompressed: min: 40.768M max: 67.938M average: 54.353M total: 108.706M
  column values statistics: min: ERR251691.10000031, max: ERR251691.999990, num_nulls: 0
[alignment, position, position] INT64 2.0% of all space [PLAIN, RLE, BIT_PACKED] min: 3.139M max: 5.212M average: 4.176M total: 8.352M (raw data: 40.113M saving 79%)
  values: min: 1.888M max: 3.134M average: 2.511M total: 5.023M
  uncompressed: min: 15.084M max: 25.029M average: 20.056M total: 40.113M
  column values statistics: min: 85630224, max: 134945932, num_nulls: 7745
[alignedSequence] BINARY 17.2% of all space [PLAIN, RLE, BIT_PACKED, PLAIN_DICTIONARY] min: 26.464M max: 44.057M average: 35.26M total: 70.521M (raw data: 522.553M saving 86%)
  values: min: 1.888M max: 3.134M average: 2.511M total: 5.023M
  uncompressed: min: 196.486M max: 326.067M average: 261.276M total: 522.553M
  column values statistics: min: AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAGG, max: TTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTAATAATAAAATAAAATTTCAAAAGGGGGGC, num_nulls: 0
[alignedQuality, array] INT32 71.4% of all space [RLE, PLAIN_DICTIONARY] min: 108.794M max: 183.665M average: 146.229M total: 292.459M (raw data: 392.484M saving 25%)
  values: min: 188.885M max: 313.454M average: 251.169M total: 502.339M
  uncompressed: min: 147.893M max: 244.591M average: 196.242M total: 392.484M
  column values statistics: min: 1, max: 50, num_nulls: 0
[nextMatePosition, position] INT64 2.4% of all space [PLAIN, RLE, BIT_PACKED] min: 3.759M max: 6.245M average: 5.002M total: 10.005M (raw data: 39.92M saving 74%)
  values: min: 1.888M max: 3.134M average: 2.511M total: 5.023M
  uncompressed: min: 15.013M max: 24.907M average: 19.96M total: 39.92M
  column values statistics: min: 956, max: 248418743, num_nulls: 17683
number of blocks: 2
total data size: 409.295M (raw 1.122G)
total record: 5.023M
average block size: 204.647M (raw 561.079M)
average record count: 2.511M
```

Observations:

* The most expensive column is `alignedQuality` which takes 71.4% of all space and compresses 25% from 392M to 292M.
* The next most expensive is `alignedSequence` which takes 17.2% of all space and compresses 86% from 522M to 70M.
* The `fragmentName` (`QNAME`) takes 4.5% of all space and compresses 82% from 108M to 18M.

Experiment:

If we change `alignedQuality` from `Array[Integer]` to a `String` of ASCII scores, we get better compression because Parquet is able to do bit packing. This drops the Parquet file size from 790M to 750M, e.g.

```
[alignedQuality, array] BINARY 69.9% of all space [PLAIN, RLE, BIT_PACKED] min: 90.264M max: 181.08M average: 135.672M total: 271.345M (raw data: 522.553M saving 48%)
  values: min: 1.671M max: 3.352M average: 2.511M total: 5.023M
  uncompressed: min: 173.832M max: 348.72M average: 261.276M total: 522.553M
  column values statistics: min: "GFAH<GA?@'DE;?;BCKIKIKIJIIJGIIGFJFHJGHF@HIFHJHIIHGDFJIIED>IIJJHEFEAIFGAGE<E;FE@AC@FDFHFGBCDF@DDEBC@, max:                                     IJHJIGKHKIIGJKKJIFLKJJJIJHHKHHJIMIKIJKKKHJMHJKLKFGHJIIJJBHIHIIJEGBEIIHIIIIIDHHHHHIFFHEGGHFFFGEDFDBE<, num_nulls: 0
```

Future Work: Delta-encoding the quality scores will likely result in disk-space savings.




## Code Walk-Through

All the source code is in the [src/main/scala/org/ga4gh/readstore](https://github.com/massie/gastore/tree/master/src/main/scala/org/ga4gh/readstore) directory.

* `Main.scala` is the main entry point to the program. It processes the command-line arguments and calls `addBamFile` on the `GAReadStore`
* `GAReadStore` is passed the `rootDirPath` for the GA repo. If the `staging` and `readGroups` directories don't exist, it creates them. The `addBamFile` method uses `htsjdk` to read a SAM/BAM file, stage the data in the `staging` area and then "commit" it to the repo.
* There is a single `ReadGroupStore` instance for each read group. They are responsible for converting `SAMReadGroupRecord` and `SAMRecord` objects to Global Alliance format. The method `generateSHA1` shows how we create a unique digest for a read group based on the name, description, data of creation, sample id, read sequence CRC and read quality score CRC values.
* The [SHA-1](http://en.wikipedia.org/wiki/SHA-1) hash is turned into a 40 character string with the first two characters being used as a directory.
* `SAMRecordConverter.scala` is mostly complete but there are still some `TODO`s in the code
* `SAMRecordGroupConverter.scala` has many `TODO`s in the code.
* `CRCAccumulator.scala` is a utility class for creating CRCs that are order-independent (mixing up sequence and quality score order will not effect the value.. see `CRCAccumulatorSuite.scala` as an example)

## Notes

* We need to publish are schema to Sonatype. It will make it easier for people to build projects on top of it. For now, I just copied in the schemas to this project.
* Schema `id` does not have a default value
* Schema `properPlacement`, `duplicateFragment`, `failedVendorQualityChecks`, `secondaryAlignment`, `supplementaryAlignment` have broken default values
* We need to move the schema namespace from `org.ga4gh` to `org.ga4gh.models` or something like that
