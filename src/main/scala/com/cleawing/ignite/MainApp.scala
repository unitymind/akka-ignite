package com.cleawing.ignite

object MainApp extends App {
  try {
    init()
  } catch {
    case t: Throwable =>
      destroy()
      throw t
  }
}
