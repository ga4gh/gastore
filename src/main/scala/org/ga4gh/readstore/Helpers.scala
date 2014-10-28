package org.ga4gh.readstore

import java.io.{ File, FileOutputStream }

import org.apache.avro.Schema
import org.apache.avro.generic.IndexedRecord
import org.apache.avro.io.EncoderFactory
import org.apache.avro.specific.SpecificDatumWriter
import org.apache.hadoop.fs.Path
import org.ga4gh.GAReadGroup
import parquet.avro.AvroParquetWriter
import parquet.hadoop.metadata.CompressionCodecName

import scala.reflect.ClassTag

/**
 * Singleton to hold simple helper methods
 */
object Helpers {

  def newParquetWriter[T <: IndexedRecord: ClassTag](path: String, schema: Schema,
                                                     blockSize: Int = 256 * 1024 * 1024,
                                                     pageSize: Int = 1024 * 1024,
                                                     enableDictionary: Boolean = true): AvroParquetWriter[T] = {
    new AvroParquetWriter[T](new Path(path), schema,
      CompressionCodecName.GZIP, blockSize, pageSize, enableDictionary)
  }

  def writeReadGroup(gaReadGroup: GAReadGroup, file: File): Unit = {
    val datumWriter = new SpecificDatumWriter[GAReadGroup](GAReadGroup.SCHEMA$)
    val fileOutput = new FileOutputStream(file)
    val encoder = EncoderFactory.get().jsonEncoder(GAReadGroup.SCHEMA$, fileOutput, true)
    datumWriter.write(gaReadGroup, encoder)
    encoder.flush()
    fileOutput.close()
  }

}
