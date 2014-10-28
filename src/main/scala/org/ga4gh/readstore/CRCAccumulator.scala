package org.ga4gh.readstore

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.util.zip.CRC32

class CRCAccumulator {
  val crc = new CRC32()
  private var _value = 0L

  def update(s: CharSequence): Unit = {
    if (s != null) {
      update(s.toString.getBytes(StandardCharsets.UTF_8))
    }
  }

  def update(l: Long): Unit = {
    update(ByteBuffer.allocate(8).putLong(l).array())
  }

  def update(b: Array[Byte]): Unit = {
    crc.reset()
    crc.update(b)
    _value ^= crc.getValue
  }

  // This copy-paste in code avoids a double copy of the data to a byte[]
  // NOTE: Reordering the integers changes the checksum
  def update(a: List[Integer]): Unit = {
    crc.reset()
    a.foreach { i => crc.update(i) }
    _value ^= crc.getValue
  }

  def value = _value
}
