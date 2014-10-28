package org.ga4gh.readstore.converters

import htsjdk.samtools.{ Cigar, CigarOperator, SAMRecord }
import org.ga4gh._

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
      .setSecondaryAlignment(samRecord.getNotPrimaryAlignmentFlag)
      .setSupplementaryAlignment(samRecord.getSupplementaryAlignmentFlag)
      .setAlignedSequence(samRecord.getReadString)

    if (samRecord.getReadPairedFlag) {
      builder
        .setNumberReads(2)
        .setReadNumber(if (samRecord.getFirstOfPairFlag) 0 else 1)
        .setProperPlacement(samRecord.getProperPairFlag)
    } else {
      builder
        .setNumberReads(1)
        .setReadNumber(0)
        .setProperPlacement(null)
    }

    if (!samRecord.getReadUnmappedFlag) {
      // TODO: double-check if we need this to be unclipped
      builder.setFragmentLength(samRecord.getAlignmentEnd - samRecord.getAlignmentStart)
      builder.setAlignment(
        GALinearAlignment.newBuilder()
          .setCigar(convert(samRecord.getCigar))
          .setMappingQuality(samRecord.getMappingQuality)
          .setPosition(
            GAPosition.newBuilder()
              .setPosition(samRecord.getAlignmentStart - 1) // SAM is 1-based, GA is 0-based
              .setReferenceName(samRecord.getReferenceName)
              .setReverseStrand(samRecord.getReadNegativeStrandFlag).build())
          .build())
      // Save the mate position this is a paired read with a mapped mate
      if (samRecord.getReadPairedFlag && !samRecord.getMateUnmappedFlag) {
        builder.setNextMatePosition(
          GAPosition.newBuilder()
            .setPosition(samRecord.getMateAlignmentStart - 1)
            .setReferenceName(samRecord.getReferenceName)
            .setReverseStrand(samRecord.getMateNegativeStrandFlag).build())
      }
    }

    builder
      .setAlignedQuality(samRecord.getBaseQualities.map(b => Integer.valueOf(b)).toList)
      .build()
  }

  def convert(cigar: Cigar): List[GACigarUnit] = {
    if (cigar != null) {
      cigar.getCigarElements.map(el =>
        GACigarUnit.newBuilder()
          .setOperation(
            el.getOperator match {
              case CigarOperator.M  => GACigarOperation.ALIGNMENT_MATCH
              case CigarOperator.I  => GACigarOperation.INSERT
              case CigarOperator.D  => GACigarOperation.DELETE
              case CigarOperator.N  => GACigarOperation.SKIP
              case CigarOperator.S  => GACigarOperation.CLIP_SOFT
              case CigarOperator.H  => GACigarOperation.CLIP_HARD
              case CigarOperator.P  => GACigarOperation.PAD
              case CigarOperator.EQ => GACigarOperation.SEQUENCE_MATCH
              case CigarOperator.X  => GACigarOperation.SEQUENCE_MISMATCH
            })
          .setOperationLength(el.getLength)
          // TODO: setReferenceSequence (see MD tag?)
          .build()).toList
    } else {
      List.empty
    }
  }

}
