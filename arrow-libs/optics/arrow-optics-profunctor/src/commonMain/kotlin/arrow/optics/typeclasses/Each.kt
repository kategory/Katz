package arrow.optics.typeclasses

import arrow.core.Nel
import arrow.optics.Optic
import arrow.optics.PIxTraversal
import arrow.optics.predef.traversedArray
import arrow.optics.predef.traversedList
import arrow.optics.predef.traversedMap
import arrow.optics.predef.traversedNel
import arrow.optics.predef.traversedSequence
import arrow.optics.predef.traversedString

fun interface Each<I, S, T, A, B> {
  fun each(): PIxTraversal<I, S, T, A, B>

  companion object {
    fun <A, B> list(): Each<Int, List<A>, List<B>, A, B> =
      Each { Optic.traversedList() }
    fun <A, B> sequence(): Each<Int, Sequence<A>, Sequence<B>, A, B> =
      Each { Optic.traversedSequence() }
    fun <A, B> nonEmptyList(): Each<Int, Nel<A>, Nel<B>, A, B> =
      Each { Optic.traversedNel() }
    fun <K, A, B> map(): Each<K, Map<K, A>, Map<K, B>, A, B> =
      Each { Optic.traversedMap() }
    fun string(): Each<Int, String, String, Char, Char> =
      Each { Optic.traversedString() }
    fun <A, B> array(): Each<Int, Array<A>, Array<B>, A, B> =
      Each { Optic.traversedArray() }
  }
}
