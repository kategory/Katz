package arrow.optics.combinators

import arrow.optics.AffineFoldK
import arrow.optics.AffineTraversalK
import arrow.optics.FoldK
import arrow.optics.IxAffineFold
import arrow.optics.IxAffineTraversal
import arrow.optics.IxFold
import arrow.optics.IxFoldF
import arrow.optics.IxTraversal
import arrow.optics.Optic
import arrow.optics.PIxTraversal
import arrow.optics.TraversalK
import arrow.optics.internal.Applicative
import arrow.optics.internal.IxWanderF
import arrow.optics.internal.Kind
import arrow.optics.ixFolding
import arrow.optics.ixTraverseLazyOf
import arrow.optics.ixTraverseLazyOf_
import arrow.optics.ixTraverseOf
import arrow.optics.ixTraverseOf_
import arrow.optics.ixTraversing
import kotlin.jvm.JvmName

@JvmName("ixFilter_traversal")
fun <K : TraversalK, I, S, A> Optic<K, I, S, S, A, A>.ixFilter(
  filter: (I, A) -> Boolean
): IxTraversal<I, S, A> =
  Optic.ixTraversing(object : IxWanderF<I, S, S, A, A> {
    override fun <F> invoke(AF: Applicative<F>, source: S, f: (I, A) -> Kind<F, A>): Kind<F, S> =
      source.ixTraverseOf(this@ixFilter, AF) { i, a ->
        if (filter(i, a)) f(i, a)
        else AF.pure(a)
      }
    override fun <F> invokeLazy(AF: Applicative<F>, source: S, f: (I, A) -> Kind<F, A>): Kind<F, S> =
      source.ixTraverseLazyOf(this@ixFilter, AF) { i, a ->
        if (filter(i, a)) f(i, a)
        else AF.pure(a)
      }
  })

@JvmName("ixFilter_affineTraversal")
fun <K : AffineTraversalK, I, S, A> Optic<K, I, S, S, A, A>.ixFilter(
  filter: (I, A) -> Boolean
): IxAffineTraversal<I, S, A> =
  // This is safe since we know that the filter will only ever be given one element
  (this as Optic<TraversalK, I, S, S, A, A>).ixFilter(filter) as IxAffineTraversal<I, S, A>

@JvmName("ixFilter_fold")
fun <K : FoldK, I, S, T, A, B> Optic<K, I, S, T, A, B>.ixFilter(
  filter: (I, A) -> Boolean
): IxFold<I, S, A> =
  Optic.ixFolding(object : IxFoldF<I, S, A> {
    override fun <F> invoke(AF: Applicative<F>, s: S, f: (I, A) -> Kind<F, Unit>): Kind<F, Unit> =
      s.ixTraverseOf_(this@ixFilter, AF) { i, a ->
        if (filter(i, a)) f(i, a)
        else AF.pure(Unit)
      }
    override fun <F> invokeLazy(AF: Applicative<F>, s: S, f: (I, A) -> Kind<F, Unit>): Kind<F, Unit> =
      s.ixTraverseLazyOf_(this@ixFilter, AF) { i, a ->
        if (filter(i, a)) f(i, a)
        else AF.pure(Unit)
      }
  })

@JvmName("ixFilter_affineFold")
fun <K : AffineFoldK, I, S, T, A, B> Optic<K, I, S, T, A, B>.ixFilter(
  filter: (I, A) -> Boolean
): IxAffineFold<I, S, A> =
  // This is safe since we know that the filter will only ever be given one element
  (this as Optic<FoldK, I, S, T, A, B>).ixFilter(filter) as IxAffineFold<I, S, A>

@JvmName("index_traversal")
fun <K : TraversalK, I, S, A> Optic<K, I, S, S, A, A>.index(ind: I): IxTraversal<I, S, A> =
  ixFilter { i, _ -> i == ind }

@JvmName("index_affineTraversal")
fun <K : AffineTraversalK, I, S, A> Optic<K, I, S, S, A, A>.index(ind: I): IxAffineTraversal<I, S, A> =
  ixFilter { i, _ -> i == ind }

@JvmName("index_fold")
fun <K : FoldK, I, S, A> Optic<K, I, S, S, A, A>.index(ind: I): IxFold<I, S, A> =
  ixFilter { i, _ -> i == ind }

@JvmName("index_affineFold")
fun <K : AffineFoldK, I, S, A> Optic<K, I, S, S, A, A>.index(ind: I): IxAffineFold<I, S, A> =
  ixFilter { i, _ -> i == ind }

// This is unsafe if the index is not unique in the traversal
@JvmName("uindex_traversal")
fun <K : TraversalK, I, S, A> Optic<K, I, S, S, A, A>.uIndex(ind: I): IxAffineTraversal<I, S, A> =
  ixFilter { i, _ -> i == ind } as IxAffineTraversal<I, S, A>

@JvmName("uindex_fold")
fun <K : FoldK, I, S, A> Optic<K, I, S, S, A, A>.uIndex(ind: I): IxAffineFold<I, S, A> =
  ixFilter { i, _ -> i == ind } as IxAffineFold<I, S, A>

// withIndex
@JvmName("withIndex_traversal")
fun <K : TraversalK, I, S, T, A, B> Optic<K, I, S, T, A, B>.withIndex(): PIxTraversal<I, S, T, Pair<I, A>, Pair<I, B>> =
  Optic.ixTraversing(object : IxWanderF<I, S, T, Pair<I, A>, Pair<I, B>> {
    override fun <F> invoke(AF: Applicative<F>, source: S, f: (I, Pair<I, A>) -> Kind<F, Pair<I, B>>): Kind<F, T> =
      source.ixTraverseOf(this@withIndex, AF) { i, a ->
        AF.map(f(i, i to a)) { (_, b) -> b }
      }
    override fun <F> invokeLazy(AF: Applicative<F>, source: S, f: (I, Pair<I, A>) -> Kind<F, Pair<I, B>>): Kind<F, T> =
      source.ixTraverseLazyOf(this@withIndex, AF) { i, a ->
        AF.map(f(i, i to a)) { (_, b) -> b }
      }
  })

@JvmName("withIndex_fold")
fun <K : FoldK, I, S, T, A, B> Optic<K, I, S, T, A, B>.withIndex(): IxFold<I, S, Pair<I, A>> =
  Optic.ixFolding(object : IxFoldF<I, S, Pair<I, A>> {
    override fun <F> invoke(AF: Applicative<F>, s: S, f: (I, Pair<I, A>) -> Kind<F, Unit>): Kind<F, Unit> =
      s.ixTraverseOf_(this@withIndex, AF) { i, a ->
        f(i, i to a)
      }
    override fun <F> invokeLazy(AF: Applicative<F>, s: S, f: (I, Pair<I, A>) -> Kind<F, Unit>): Kind<F, Unit> =
      s.ixTraverseLazyOf_(this@withIndex, AF) { i, a ->
        f(i, i to a)
      }
  })
