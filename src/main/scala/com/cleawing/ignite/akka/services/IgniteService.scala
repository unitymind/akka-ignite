package com.cleawing.ignite.akka.services

import java.util.UUID

import org.apache.ignite.services.{ServiceContext, Service}

trait IgniteService {
  def serviceId : String
  def executionId : UUID
}

abstract class IgniteServiceImpl extends Service with IgniteService {
  private var _executionId : UUID = _
  private var _serviceId : String = _

  def init(ctx: ServiceContext) : Unit = {
    _executionId = ctx.executionId()
    _serviceId = ctx.name()
  }

  def execute(ctx: ServiceContext) : Unit = ()
  def cancel(ctx: ServiceContext) : Unit = ()

  def executionId : UUID = _executionId
  def serviceId : String = _serviceId
}

