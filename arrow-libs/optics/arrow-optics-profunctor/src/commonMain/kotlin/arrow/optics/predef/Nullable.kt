package arrow.optics.predef

import arrow.core.Either
import arrow.optics.AffineFoldK
import arrow.optics.AffineTraversalK
import arrow.optics.FoldK
import arrow.optics.Optic
import arrow.optics.PPrism
import arrow.optics.Prism
import arrow.optics.PrismK
import arrow.optics.TraversalK
import arrow.optics.compose
import arrow.optics.prism
import kotlin.jvm.JvmName

fun <A, B> Optic.Companion.notNull(): PPrism<A?, B?, A, B> =
  prism({ a ->
    a?.let { Either.Right(it) } ?: Either.Left(null)
  }, { b -> b })

@JvmName("notNull_prism")
fun <K : PrismK, I, S, T, A, B> Optic<K, I, S, T, A?, B?>.notNull(): Optic<PrismK, I, S, T, A, B> =
  compose(Optic.notNull())

@JvmName("notNull_affineTraversal")
fun <K : AffineTraversalK, I, S, T, A, B> Optic<K, I, S, T, A?, B?>.notNull(): Optic<AffineTraversalK, I, S, T, A, B> =
  compose(Optic.notNull())

@JvmName("notNull_traversal")
fun <K : TraversalK, I, S, T, A, B> Optic<K, I, S, T, A?, B?>.notNull(): Optic<TraversalK, I, S, T, A, B> =
  compose(Optic.notNull())

@JvmName("notNull_affineFold")
fun <K : AffineFoldK, I, S, T, A, B> Optic<K, I, S, T, A?, B?>.notNull(): Optic<AffineFoldK, I, S, T, A, B> =
  compose(Optic.notNull())

@JvmName("notNull_fold")
fun <K : FoldK, I, S, T, A, B> Optic<K, I, S, T, A?, B?>.notNull(): Optic<FoldK, I, S, T, A, B> =
  compose(Optic.notNull())

fun <A> Optic.Companion.isNull(): Prism<A?, Unit> =
  prism({ a ->
    a?.let { Either.Left(a) } ?: Either.Right(Unit)
  }, { null })

@JvmName("isNull_prism")
fun <K : PrismK, I, S, A> Optic<K, I, S, S, A?, A?>.isNull(): Optic<PrismK, I, S, S, Unit, Unit> =
  compose(Optic.isNull<A>())

@JvmName("isNull_affineTraversal")
fun <K : AffineTraversalK, I, S, A> Optic<K, I, S, S, A?, A?>.isNull(): Optic<AffineTraversalK, I, S, S, Unit, Unit> =
  compose(Optic.isNull())

@JvmName("isNull_traversal")
fun <K : TraversalK, I, S, A> Optic<K, I, S, S, A?, A?>.isNull(): Optic<TraversalK, I, S, S, Unit, Unit> =
  compose(Optic.isNull())

@JvmName("isNull_affineFold")
fun <K : AffineFoldK, I, S, A> Optic<K, I, S, S, A?, A?>.isNull(): Optic<AffineFoldK, I, S, S, Unit, Unit> =
  compose(Optic.isNull())

@JvmName("isNull_fold")
fun <K : FoldK, I, S, A> Optic<K, I, S, S, A?, A?>.isNull(): Optic<FoldK, I, S, S, Unit, Unit> =
  compose(Optic.isNull())
