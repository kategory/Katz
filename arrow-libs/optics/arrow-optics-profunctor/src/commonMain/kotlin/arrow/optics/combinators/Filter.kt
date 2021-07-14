package arrow.optics.combinators

import arrow.core.Either
import arrow.core.identity
import arrow.optics.AffineFoldK
import arrow.optics.AffineTraversalK
import arrow.optics.FoldK
import arrow.optics.Optic
import arrow.optics.PrismK
import arrow.optics.TraversalK
import arrow.optics.compose
import arrow.optics.isNotEmpty
import arrow.optics.prism
import kotlin.jvm.JvmName

fun <S> Optic.Companion.filter(pred: (S) -> Boolean): Optic<PrismK, Any?, S, S, S, S> =
  prism({ s -> if (pred(s)) Either.Right(s) else Either.Left(s) }, ::identity)

@JvmName("filter_prism")
fun <K : PrismK, I, S, T, A> Optic<K, I, S, T, A, A>.filter(
  pred: (A) -> Boolean
): Optic<PrismK, I, S, T, A, A> = compose(Optic.filter(pred))

@JvmName("filter_affineTraversal")
fun <K : AffineTraversalK, I, S, T, A> Optic<K, I, S, T, A, A>.filter(
  pred: (A) -> Boolean
): Optic<AffineTraversalK, I, S, T, A, A> = compose(Optic.filter(pred))

@JvmName("filter_traversal")
fun <K : TraversalK, I, S, T, A> Optic<K, I, S, T, A, A>.filter(
  pred: (A) -> Boolean
): Optic<TraversalK, I, S, T, A, A> = compose(Optic.filter(pred))

// Safe cast because AffineFoldK ensures use of Forget. This typecast makes usage with folds created from polymorphic traversals slightly easier
@JvmName("filter_affineFold")
fun <K : AffineFoldK, I, S, T, A, B> Optic<K, I, S, T, A, B>.filter(
  pred: (A) -> Boolean
): Optic<AffineFoldK, I, S, T, A, B> = compose(Optic.filter(pred) as Optic<PrismK, Any?, A, B, A, B>)

// See above for notes on typecast
@JvmName("filter_fold")
fun <K : FoldK, I, S, T, A, B> Optic<K, I, S, T, A, B>.filter(
  pred: (A) -> Boolean
): Optic<FoldK, I, S, T, A, B> = compose(Optic.filter(pred) as Optic<PrismK, Any?, A, B, A, B>)

fun <K : FoldK, I, S> Optic.Companion.filteredBy(filter: Optic<K, I, S, S, Any?, Any?>): Optic<PrismK, Any?, S, S, S, S> =
  filter { s -> s.isNotEmpty(filter) }

@JvmName("filteredBy_prism")
fun <K1 : PrismK, K2 : FoldK, I, J, S, T, A> Optic<K1, I, S, T, A, A>.filteredBy(
  filter: Optic<K2, J, A, A, Any?, Any?>
): Optic<PrismK, I, S, T, A, A> =
  compose(Optic.filteredBy(filter))

@JvmName("filteredBy_affineTraversal")
fun <K1 : AffineTraversalK, K2 : FoldK, I, J, S, T, A> Optic<K1, I, S, T, A, A>.filteredBy(
  filter: Optic<K2, J, A, A, Any?, Any?>
): Optic<AffineTraversalK, I, S, T, A, A> =
  compose(Optic.filteredBy(filter))

@JvmName("filteredBy_traversal")
fun <K1 : TraversalK, K2 : FoldK, I, J, S, T, A> Optic<K1, I, S, T, A, A>.filteredBy(
  filter: Optic<K2, J, A, A, Any?, Any?>
): Optic<TraversalK, I, S, T, A, A> =
  compose(Optic.filteredBy(filter))

// See above for notes on typecast
@JvmName("filteredBy_affineFold")
fun <K1 : AffineFoldK, K2 : FoldK, I, J, S, T, A, B> Optic<K1, I, S, T, A, B>.filteredBy(
  filter: Optic<K2, J, A, A, Any?, Any?>
): Optic<AffineFoldK, I, S, T, A, B> =
  compose(Optic.filteredBy(filter) as Optic<PrismK, Any?, A, B, A, B>)

// See above for notes on typecast
@JvmName("filteredBy_fold")
fun <K1 : FoldK, K2 : FoldK, S, T, I, J, A, B> Optic<K1, I, S, T, A, B>.filteredBy(
  filter: Optic<K2, J, A, A, Any?, Any?>
): Optic<FoldK, I, S, T, A, B> =
  compose(Optic.filteredBy(filter) as Optic<PrismK, Any?, A, B, A, B>)
