package arrow.optics.predef

import arrow.core.Either
import arrow.core.None
import arrow.core.Option
import arrow.core.Some
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

fun <A, B> Optic.Companion.some(): PPrism<Option<A>, Option<B>, A, B> =
  prism({ opt ->
    opt.fold({ Either.Left(None) }, { a -> Either.Right(a) })
  }, { b -> Some(b) })

@JvmName("some_prism")
fun <K : PrismK, I, S, T, A, B> Optic<K, I, S, T, Option<A>, Option<B>>.some(): Optic<PrismK, I, S, T, A, B> =
  compose(Optic.some())

@JvmName("some_affineTraversal")
fun <K : AffineTraversalK, I, S, T, A, B> Optic<K, I, S, T, Option<A>, Option<B>>.some(): Optic<AffineTraversalK, I, S, T, A, B> =
  compose(Optic.some())

@JvmName("some_traversal")
fun <K : TraversalK, I, S, T, A, B> Optic<K, I, S, T, Option<A>, Option<B>>.some(): Optic<TraversalK, I, S, T, A, B> =
  compose(Optic.some())

@JvmName("some_affineFold")
fun <K : AffineFoldK, I, S, T, A, B> Optic<K, I, S, T, Option<A>, Option<B>>.some(): Optic<AffineFoldK, I, S, T, A, B> =
  compose(Optic.some())

@JvmName("some_fold")
fun <K : FoldK, I, S, T, A, B> Optic<K, I, S, T, Option<A>, Option<B>>.some(): Optic<FoldK, I, S, T, A, B> =
  compose(Optic.some())

fun <A> Optic.Companion.none(): Prism<Option<A>, Unit> =
  prism({ opt ->
    opt.fold({ Either.Right(Unit) }, { a -> Either.Left(Some(a)) })
  }, { None })

@JvmName("none_prism")
fun <K : PrismK, I, S, A> Optic<K, I, S, S, Option<A>, Option<A>>.none(): Optic<PrismK, I, S, S, Unit, Unit> =
  compose(Optic.none())

@JvmName("none_affineTraversal")
fun <K : AffineTraversalK, I, S, A> Optic<K, I, S, S, Option<A>, Option<A>>.none(): Optic<AffineTraversalK, I, S, S, Unit, Unit> =
  compose(Optic.none())

@JvmName("none_traversal")
fun <K : TraversalK, I, S, A> Optic<K, I, S, S, Option<A>, Option<A>>.none(): Optic<TraversalK, I, S, S, Unit, Unit> =
  compose(Optic.none())

@JvmName("none_affineFold")
fun <K : AffineFoldK, I, S, A> Optic<K, I, S, S, Option<A>, Option<A>>.none(): Optic<AffineFoldK, I, S, S, Unit, Unit> =
  compose(Optic.none())

@JvmName("none_fold")
fun <K : FoldK, I, S, A> Optic<K, I, S, S, Option<A>, Option<A>>.none(): Optic<FoldK, I, S, S, Unit, Unit> =
  compose(Optic.none())
