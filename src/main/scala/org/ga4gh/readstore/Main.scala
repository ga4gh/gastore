package org.ga4gh.readstore

object Main {

  val parser = new scopt.OptionParser[Params]("gastore") {
    opt[String]('i', "input") required () valueName "<file>" action { (x, c) =>
      c.copy(bamFile = Some(x))
    } text "The sam/bam file to convert and compute a digest on"
    opt[String]('s', "ga_readstore_dir") required () valueName "<path>" action { (x, c) =>
      c.copy(gaReadstoreDir = Some(x))
    } text "Path to the Global Alliance read store data"
    help("help") text "prints this usage text"
  }

  def main(args: Array[String]): Unit = {
    // parser.parse returns Option[C]
    parser.parse(args, Params()) map { params =>

      val gaReadstore = new GAReadStore(params.gaReadstoreDir.get)
      // add a new bam file to the read store
      gaReadstore.addBamFile(params.bamFile.get)

    } getOrElse {
      // arguments are bad, error message will have been displayed
    }
  }

  case class Params(bamFile: Option[String] = None, gaReadstoreDir: Option[String] = None)

}
