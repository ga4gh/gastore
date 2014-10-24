package org.ga4gh.readstore.converters

import htsjdk.samtools.SAMRecord
import org.ga4gh.GAReadAlignment

import scala.collection.JavaConversions._

/**
 * Helper for converting SAMRecords into GAReadAlignment objects
 */
object SAMRecordConverter {

  def convert(samRecord: SAMRecord): GAReadAlignment = {
    val builder = GAReadAlignment.newBuilder()
      .setId(null)
      .setReadGroupId(samRecord.getReadGroup.getReadGroupId)
      .setFragmentName(samRecord.getReadName)
      .setDuplicateFragment(samRecord.getDuplicateReadFlag)
      // TODO: double-check if we need this to be unclipped
      .setFragmentLength(samRecord.getAlignmentEnd - samRecord.getAlignmentStart)
      .setSecondaryAlignment(samRecord.getNotPrimaryAlignmentFlag)
      .setSupplementaryAlignment(samRecord.getSupplementaryAlignmentFlag)
      .setAlignedSequence(samRecord.getReadString)

    if (samRecord.getReadPairedFlag) {
      builder.setNumberReads(2).setReadNumber(if (samRecord.getFirstOfPairFlag) 0 else 1)
        .setProperPlacement(samRecord.getProperPairFlag)
    } else {
      builder.setNumberReads(1).setReadNumber(0).setProperPlacement(null)
    }

    // TODO: set alignment
    builder.setAlignment(null)
    // TODO: set aligned quality
    builder.setAlignedQuality(List[Integer](1, 2, 3))
    // TODO: next mate position
    builder.setNextMatePosition(null)
    builder.build()
  }

}
