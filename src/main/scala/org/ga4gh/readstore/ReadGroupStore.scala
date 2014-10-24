package org.ga4gh.readstore

import java.io.File
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.nio.file.{ Files, StandardCopyOption }
import java.security.MessageDigest

import htsjdk.samtools.{ SAMReadGroupRecord, SAMRecord }
import org.ga4gh.GAReadAlignment
import org.ga4gh.readstore.converters.{ SAMReadGroupConverter, SAMRecordConverter }
import parquet.avro.AvroParquetWriter

import scala.collection.JavaConversions._

/**
 * Class for managing the data in a single read store
 * @param dir The directory to write the read group and its reads
 * @param samReadGroup The SAMReadGroupsRecord associated with this read group store
 * @param num The unique number of this read group (from the SAM/BAM header)
 */
class ReadGroupStore(dir: File, samReadGroup: SAMReadGroupRecord, num: Int) {
  // Make sure the read group directory exists
  dir.mkdirs()
  // Convert SAM Read Group to Global Alliance Read Group
  val gaReadGroup = SAMReadGroupConverter.convert(samReadGroup)
  // Instantiate CRC Accumulators
  val seqCRCAccumulator = new CRCAccumulator
  val qualCRCAccumulator = new CRCAccumulator
  // Create a Parquet file to hold all the reads for this read store
  val reads: AvroParquetWriter[GAReadAlignment] =
    Helpers.newParquetWriter(new File(dir, "reads").toString, GAReadAlignment.SCHEMA$)

  def addSamRecord(samRecord: SAMRecord): Unit = {
    // Convert the SAMRecord into the Global Alliance standard
    val gaReadAlignment = SAMRecordConverter.convert(samRecord)
    // Write the GA Read Alignment to Parquet
    reads.write(gaReadAlignment)

    // Accumulate the CRCs
    seqCRCAccumulator.update(gaReadAlignment.getAlignedSequence)
    if (gaReadAlignment.getAlignedQuality != null) {
      qualCRCAccumulator.update(gaReadAlignment.getAlignedQuality.toList)
    }
  }

  def commit(readGroupDir: File): Unit = {
    // Convert the SAM read group to Global Alliance format
    val gaReadGroup = SAMReadGroupConverter.convert(samReadGroup)
    // Write the GA Read Group to file
    Helpers.writeReadGroup(gaReadGroup, new File(dir, "readGroupInfo"))
    // Close the Reads file
    reads.close()
    // Create a directory based on the SHA-1 (e.g. readGroup/1C/772F91CA073DA9D86D42B12378D910BC1A7B96)
    val sha1 = generateSHA1()
    val shaPrefix = new File(readGroupDir, sha1.substring(0, 2))
    val dest = new File(shaPrefix, sha1.substring(2))
    dest.mkdirs()
    // Move the directory (NOTE: This will fail if we already have the data)
    Files.move(dir.toPath, dest.toPath, StandardCopyOption.ATOMIC_MOVE)
  }

  private def generateSHA1(): String = {

    // A simple helper
    def charSequenceToBytes(s: CharSequence): Array[Byte] = {
      if (s == null) Array.empty else s.toString.getBytes(StandardCharsets.UTF_8)
    }

    // Create a digest of the Read Group by (order-dependent!)...
    val md = MessageDigest.getInstance("SHA1")
    // (1) Name
    md.update(charSequenceToBytes(gaReadGroup.getName))
    // (2) Description
    md.update(charSequenceToBytes(gaReadGroup.getDescription))
    // (3) Creation Date
    md.update(ByteBuffer.allocate(8).putLong(gaReadGroup.getCreated))
    // (4) Sample ID
    md.update(charSequenceToBytes(gaReadGroup.getSampleId))
    // (5) Sequence CRC
    md.update(ByteBuffer.allocate(8).putLong(seqCRCAccumulator.value))
    // (6) Quality Score CRC
    md.update(ByteBuffer.allocate(8).putLong(qualCRCAccumulator.value))

    // Return a hex-formatted string
    md.digest().map("%02X" format _).mkString
  }

}

