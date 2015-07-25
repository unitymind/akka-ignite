package com.cleawing.ignite

object MainApp extends App {
  try {
    Injector().initNonLazy()
  } catch {
    case t: Throwable =>
      Injector().destroy()
      throw t
  }
}
