package com.cleawing

import com.cleawing.ignite.Injector.IgniteModule
import org.jetbrains.annotations.Nullable
import scaldi.TypesafeConfigInjector

import scala.annotation.meta.field

package object ignite {
  type NullableField = Nullable @field
  implicit val injector = TypesafeConfigInjector() :: new IgniteModule
}
