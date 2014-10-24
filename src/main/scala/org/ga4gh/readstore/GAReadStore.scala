package org.ga4gh.readstore

import java.io.File

import htsjdk.samtools.{ SamReaderFactory, ValidationStringency }

import scala.collection.JavaConversions._
import scala.util.Random

class GAReadStore(rootDirPath: String) {
  // The root of the read store
  val rootDir = new File(rootDirPath)
  // Directory for staging new files before committing to the read store
  val stagingDir = new File(rootDir, "staging")
  // Directory to hold the processed read groups
  val readGroupDir = new File(rootDir, "readGroups")
  stagingDir.mkdirs()
  readGroupDir.mkdirs()

  def addBamFile(bamFile: String, stringency: ValidationStringency = ValidationStringency.LENIENT): Unit = {
    val sessionDir = new File(stagingDir, s"${System.currentTimeMillis()}-${new Random().nextLong()}")
    val samReader = SamReaderFactory.make().validationStringency(stringency).open(new File(bamFile))
    val readGroupStores = scala.collection.mutable.HashMap.empty[String, ReadGroupStore]
    // Create a storage location for each read group
    samReader.getFileHeader.getReadGroups.zipWithIndex.foreach {
      case (readGroup, num) =>
        readGroupStores += (readGroup.getReadGroupId ->
          new ReadGroupStore(new File(sessionDir, s"$num"), readGroup, num))
    }
    // Walk through the reads and save them to the correct read group
    samReader foreach { samRecord =>
      val readGroupId = samRecord.getReadGroup.getReadGroupId
      val readGroupStore = readGroupStores.get(readGroupId)
      readGroupStore match {
        case Some(store) =>
          store.addSamRecord(samRecord)
        case None =>
        // TODO: throw an error
      }
    }
    samReader.close()
    // Commit the staged read groups to the repository
    readGroupStores.foreach {
      case (name, store) => store.commit(readGroupDir)
    }
  }

}
