package it.unibo.spatial.test

import org.scalatest._

object MyTag extends Tag("it.unibo.tags.myTag")

class Tests extends FlatSpec with BeforeAndAfter {

  "Sum" must "work with 3 nums" in { assert((1+2+3)===6) }

  it should "sum two numbers" taggedAs(MyTag) in { assertResult(2){1+1} }

}