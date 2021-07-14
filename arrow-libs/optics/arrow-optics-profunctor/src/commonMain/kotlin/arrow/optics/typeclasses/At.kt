package arrow.optics.typeclasses

import arrow.optics.Lens
import arrow.optics.Optic
import arrow.optics.lens

fun interface At<I, S, A> {
  fun at(i: I): Lens<S, A?>

  companion object {
    fun <K, V> map(): At<K, Map<K, V>, V> = At { k ->
      Optic.lens({ s -> s[k] }, { s, v ->
        s.toMutableMap().also {
          if (v == null) it.remove(k)
          else it[k] = v
        }
      })
    }
    fun <A> set(): At<A, Set<A>, Unit> = At { a ->
      Optic.lens({ s ->
        if (a in s) Unit else null
      }, { s, u ->
        if (u == null) s.toMutableSet().also { it.remove(a) }
        else s
      })
    }
  }
}
