package org.ga4gh.readstore.converters

import htsjdk.samtools.SAMReadGroupRecord
import org.ga4gh.{ GAProgram, GAReadGroup }

import scala.collection.JavaConversions._

/**
 * Helper object for converting SAMReadGroupRecord object into GAReadGroup objects
 */
object SAMReadGroupConverter {

  def convert(samReadGroupRecord: SAMReadGroupRecord): GAReadGroup = {
    val created = if (samReadGroupRecord.getRunDate == null) 0L else samReadGroupRecord.getRunDate.getTime
    GAReadGroup.newBuilder()
      .setCreated(created)
      .setDatasetId(samReadGroupRecord.getReadGroupId)
      .setDescription(samReadGroupRecord.getDescription)
      .setExperiment(null) // TODO
      .setId(samReadGroupRecord.getReadGroupId) // TODO
      // TODO .setInfo()
      .setName(samReadGroupRecord.getReadGroupId) // TODO: Check
      .setPredictedInsertSize(samReadGroupRecord.getPredictedMedianInsertSize)
      .setPrograms(List.empty[GAProgram]) // TODO
      // TODO .setReferenceSetId()
      .setSampleId(samReadGroupRecord.getSample)
      .setUpdated(0L) // TODO: this should be removed... as a read group is immutable
      .build()
  }

}
