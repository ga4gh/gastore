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
