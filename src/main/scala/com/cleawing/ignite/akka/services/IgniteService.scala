package com.cleawing.ignite.akka.services

import java.util.UUID

import org.apache.ignite.services.{ServiceContext, Service}

trait IgniteService {
  def name : String
  def executionId : UUID
}

abstract class IgniteServiceImpl extends Service with IgniteService {
  protected var _executionId : UUID = _
  protected var _name : String = _

  def init(ctx: ServiceContext) : Unit = {
    _executionId = ctx.executionId()
    _name = ctx.name()
  }

  def execute(ctx: ServiceContext) : Unit = ()
  def cancel(ctx: ServiceContext) : Unit = ()

  def executionId : UUID = _executionId
  def name : String = _name
}

