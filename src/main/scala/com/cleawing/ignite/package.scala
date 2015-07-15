package com.cleawing

import org.apache.ignite.Ignition

package object ignite {
  trait IgniteService {
    protected def isDeployed : Boolean
    protected def inTerminate : Boolean
  }

  def services = Ignition.ignite().services()

  def start() : Unit = Ignition.start(getClass.getResourceAsStream("/reference_ignite.xml"))

  def stop(): Unit = {
    Ignition.stop(false)
  }
}
