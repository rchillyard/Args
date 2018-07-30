/*
 * Copyright (c) 2018 Phasmid Software, Project Args.
 */

package com.phasmidsoftware.args

/**
  * Type-class trait to allow conversion from type X to type T
  *
  * @tparam T the result type
  */
trait Derivable[T] {
  /**
    * Method to convert an X to a T
    *
    * @param x the X value
    * @tparam X the input type
    * @return a T
    */
  def deriveFrom[X](x: X): T
}
