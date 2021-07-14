package arrow.optics.combinators

import arrow.optics.AffineFoldK
import arrow.optics.AffineTraversalK
import arrow.optics.FoldK
import arrow.optics.Optic
import arrow.optics.Prism
import arrow.optics.PrismK
import arrow.optics.TraversalK
import arrow.optics.compose
import arrow.optics.simplePrism
import kotlin.jvm.JvmName

fun <A> Optic.Companion.only(a: A): Prism<A, Unit> =
  simplePrism({ if (it == a) Unit else null }, { a })

@JvmName("only_prism")
fun <K : PrismK, I, S, T, A> Optic<K, I, S, T, A, A>.only(a: A): Optic<PrismK, I, S, T, Unit, Unit> =
  compose(Optic.only(a))

@JvmName("only_affineTraversal")
fun <K : AffineTraversalK, I, S, T, A> Optic<K, I, S, T, A, A>.only(a: A): Optic<AffineTraversalK, I, S, T, Unit, Unit> =
  compose(Optic.only(a))

@JvmName("only_traversal")
fun <K : TraversalK, I, S, T, A> Optic<K, I, S, T, A, A>.only(a: A): Optic<TraversalK, I, S, T, Unit, Unit> =
  compose(Optic.only(a))

@JvmName("only_affineFold")
fun <K : AffineFoldK, I, S, T, A> Optic<K, I, S, T, A, A>.only(a: A): Optic<AffineFoldK, I, S, T, Unit, Unit> =
  compose(Optic.only(a))

@JvmName("only_fold")
fun <K : FoldK, I, S, T, A> Optic<K, I, S, T, A, A>.only(a: A): Optic<FoldK, I, S, T, Unit, Unit> =
  compose(Optic.only(a))
