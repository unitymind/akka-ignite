package com.cleawing.ignite.playground

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, ObjectInputStream, ObjectOutputStream}

import com.spingo.op_rabbit.{RabbitMarshaller, RabbitUnmarshaller}

object JavaMarshaller extends RabbitMarshaller[ProxyEnvelope] with RabbitUnmarshaller[ProxyEnvelope]  {
  protected val contentType = "application/octet-stream"
  protected val contentEncoding = None
  def marshall(value: ProxyEnvelope) : Array[Byte] = {
    val buffer = new ByteArrayOutputStream()
    val output = new ObjectOutputStream(buffer)
    output.writeObject(value)
    output.close()
    buffer.toByteArray
  }
  def unmarshall(value: Array[Byte], contentType: Option[String], charset: Option[String]): ProxyEnvelope = {
    val buffer = new ByteArrayInputStream(value)
    val input = new ObjectInputStream(buffer)
    val res : ProxyEnvelope = input.readObject().asInstanceOf[ProxyEnvelope]
    input.close()
    res
  }
}
