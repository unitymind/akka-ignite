package com.cleawing

import org.apache.ignite.Ignition

package object ignite {
  trait IgniteService {
    protected def name: String
    protected def isDeployed : Boolean
    protected def inCancel : Boolean
  }

  def services = Ignition.ignite().services()
}
