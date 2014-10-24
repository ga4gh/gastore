package org.ga4gh.readstore

import org.scalatest.FunSuite

class CRCAccumulatorSuite extends FunSuite {

  test("different inputs, different crc") {
    val acc1 = new CRCAccumulator()
    acc1.update("foo")
    acc1.update("bar")
    acc1.update("baz")

    val acc2 = new CRCAccumulator()
    acc2.update("one")
    acc2.update("two")
    acc2.update("three")

    assert(acc1.value != acc2.value)
  }

  test("order independent") {
    val acc1 = new CRCAccumulator()
    acc1.update("foo")
    acc1.update("bar")
    acc1.update("baz")
    acc1.update(List[Integer](1, 2, 3))

    val acc2 = new CRCAccumulator()
    acc2.update("foo")
    acc2.update(List[Integer](1, 2, 3))
    acc2.update("baz")
    acc2.update("bar")

    assert(acc1.value == acc2.value)
  }

  test("list of integers is order-dependent") {
    val acc1 = new CRCAccumulator()
    acc1.update(List[Integer](1, 2, 3))

    val acc2 = new CRCAccumulator()
    acc2.update(List[Integer](3, 2, 1))

    assert(acc1.value != acc2.value)
  }

}
