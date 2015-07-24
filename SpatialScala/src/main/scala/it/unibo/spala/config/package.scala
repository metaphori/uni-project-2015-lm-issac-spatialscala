package it.unibo.spala

/**
 * Created by: Roberto Casadei 
 * Created on date: 20/07/15.
 */
package object config {

  def CmdLineParser[T] = new scopt.OptionParser[Settings[T]]("<spatial program>") {
    head("<spatial program>", "1.0")

    opt[String]("prefix") valueName("<prefix>") action { (x, c) =>
      c.copy(systemSettings = c.systemSettings.copy(prefix = x))
    } text("Prefix for unique device names")

    opt[Int]("numNodes") valueName("<N>") required() action { (x, c) =>
      c.copy(devDistributionSettings = c.devDistributionSettings.copy(n = x))
    } text("Number of nodes")

    opt[Double]("proximity") valueName("<N>") action { (x, c) =>
      c.copy(spatialSettings = c.spatialSettings.copy(proximityThreshold = x))
    } text("Proximity threshold")

    opt[Seq[Int]]("grid") required() valueName("<nrows>, <ncols>, <stepx>, <stepy>, <offsetx>, <offsety>") action { (x,c) =>
      c.copy(devDistributionSettings = c.devDistributionSettings.copy(shapeSettings =
        GridSettings(nrows = x(0), ncols = x(1), stepx = x(2), stepy = x(3), offsetx = x(4), offsety = x(5))))
    } text("Grid parameters")

    opt[String]("host") valueName("<HOST>") required() action { (x, c) =>
      c.copy(deploymentSettings = c.deploymentSettings.copy(
        kind = DistributedDeployment,
        distrib = c.deploymentSettings.distrib.copy(host = x)))
    } text("Host of deployment of the actor system")

    opt[Int]("port") valueName("<PORT>") required() action { (x, c) =>
      c.copy(deploymentSettings = c.deploymentSettings.copy(
        kind = DistributedDeployment,
        distrib = c.deploymentSettings.distrib.copy(port = x)))
    } text("Port of deployment of the actor system")

    opt[Seq[String]]("nodes") valueName("<ADDR1>, ..., <ADDRN>") action { (x,c) =>
      c.copy(deploymentSettings = c.deploymentSettings.copy(
        kind = DistributedDeployment,
        distrib = c.deploymentSettings.distrib.copy(nodes = x)))
    } text("Nodes that should be contacted on start")

    opt[Boolean]("gui") action { (x, c) =>
      c.copy(systemSettings = c.systemSettings.copy(gui = x))
    } text("To launch a GUI")

    cmd("start") valueName("start") action { (_, c) =>
      c.copy(systemSettings = c.systemSettings.copy(start = true))
    } text("start the system")

    //note("some notes.\n")
    help("help") text("prints this usage text")
  }

}
