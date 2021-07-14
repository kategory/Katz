package arrow.optics.predef

import arrow.core.Nel
import arrow.optics.Optic
import arrow.optics.PIxTraversal
import arrow.optics.TraversalK
import arrow.optics.compose
import arrow.optics.iso
import arrow.optics.ixComposeRight
import kotlin.jvm.JvmName

fun <A, B> Optic.Companion.traversedNel(): PIxTraversal<Int, Nel<A>, Nel<B>, A, B> =
  Optic.iso({ xs: Nel<A> -> xs.all }, { b: List<B> -> Nel.fromListUnsafe(b) })
    .ixComposeRight(Optic.traversedList())

@JvmName("nel_traversed")
fun <K : TraversalK, I, S, T, A, B> Optic<K, I, S, T, Nel<A>, Nel<B>>.traversed(): Optic<TraversalK, I, S, T, A, B> =
  compose(Optic.traversedNel())
