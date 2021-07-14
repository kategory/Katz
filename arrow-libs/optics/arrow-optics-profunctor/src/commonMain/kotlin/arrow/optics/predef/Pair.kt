package arrow.optics.predef

import arrow.optics.AffineFoldK
import arrow.optics.AffineTraversalK
import arrow.optics.Fold
import arrow.optics.FoldF
import arrow.optics.FoldK
import arrow.optics.GetterK
import arrow.optics.LensK
import arrow.optics.Optic
import arrow.optics.PLens
import arrow.optics.PTraversal
import arrow.optics.TraversalK
import arrow.optics.compose
import arrow.optics.folding
import arrow.optics.internal.Applicative
import arrow.optics.internal.Kind
import arrow.optics.internal.WanderF
import arrow.optics.lens
import arrow.optics.traverseOf
import arrow.optics.traverseOf_
import arrow.optics.traversing
import kotlin.jvm.JvmName

fun <A, B, C> Optic.Companion.pairFirst(): PLens<Pair<A, C>, Pair<B, C>, A, B> =
  Optic.lens({ (a, _) -> a }, { (_, c), b -> b to c })

// Ugly^^
@JvmName("first_pair_lens")
fun <K : LensK, I, S, T, A, B, C> Optic<K, I, S, T, Pair<A, C>, Pair<B, C>>.first(): Optic<LensK, I, S, T, A, B> =
  compose(Optic.pairFirst())

@JvmName("first_affine_traversal")
fun <K : AffineTraversalK, I, S, T, A, B, C> Optic<K, I, S, T, Pair<A, C>, Pair<B, C>>.first(): Optic<AffineTraversalK, I, S, T, A, B> =
  compose(Optic.pairFirst())

@JvmName("first_pair_traversal")
fun <K : TraversalK, I, S, T, A, B, C> Optic<K, I, S, T, Pair<A, C>, Pair<B, C>>.first(): Optic<TraversalK, I, S, T, A, B> =
  compose(Optic.pairFirst())

@JvmName("first_pair_getter")
fun <K : GetterK, I, S, T, A, B, C> Optic<K, I, S, T, Pair<A, C>, Pair<B, C>>.first(): Optic<GetterK, I, S, T, A, B> =
  compose(Optic.pairFirst())

@JvmName("first_pair_affine_fold")
fun <K : AffineFoldK, I, S, T, A, B, C> Optic<K, I, S, T, Pair<A, C>, Pair<B, C>>.first(): Optic<AffineFoldK, I, S, T, A, B> =
  compose(Optic.pairFirst())

@JvmName("first_pair_fold")
fun <K : FoldK, I, S, T, A, B, C> Optic<K, I, S, T, Pair<A, C>, Pair<B, C>>.first(): Optic<FoldK, I, S, T, A, B> =
  compose(Optic.pairFirst())

fun <A, B, C> Optic.Companion.pairSecond(): PLens<Pair<A, B>, Pair<A, C>, B, C> =
  Optic.lens({ (_, b) -> b }, { (a, _), c -> a to c })

// Ugly^^
@JvmName("second_pair_lens")
fun <K : LensK, I, S, T, A, B, C> Optic<K, I, S, T, Pair<A, B>, Pair<A, C>>.second(): Optic<LensK, I, S, T, B, C> =
  compose(Optic.pairSecond())

@JvmName("second_affine_traversal")
fun <K : AffineTraversalK, I, S, T, A, B, C> Optic<K, I, S, T, Pair<A, B>, Pair<A, C>>.second(): Optic<AffineTraversalK, I, S, T, B, C> =
  compose(Optic.pairSecond())

@JvmName("second_pair_traversal")
fun <K : TraversalK, I, S, T, A, B, C> Optic<K, I, S, T, Pair<A, B>, Pair<A, C>>.second(): Optic<TraversalK, I, S, T, B, C> =
  compose(Optic.pairSecond())

@JvmName("second_pair_getter")
fun <K : GetterK, I, S, T, A, B, C> Optic<K, I, S, T, Pair<A, B>, Pair<A, C>>.second(): Optic<GetterK, I, S, T, B, C> =
  compose(Optic.pairSecond())

@JvmName("second_pair_affine_fold")
fun <K : AffineFoldK, I, S, T, A, B, C> Optic<K, I, S, T, Pair<A, B>, Pair<A, C>>.second(): Optic<AffineFoldK, I, S, T, B, C> =
  compose(Optic.pairSecond())

@JvmName("second_pair_fold")
fun <K : FoldK, I, S, T, A, B, C> Optic<K, I, S, T, Pair<A, B>, Pair<A, C>>.second(): Optic<FoldK, I, S, T, B, C> =
  compose(Optic.pairSecond())

fun <A, B> Optic.Companion.pairEvery(): Optic<TraversalK, Any?, Pair<A, A>, Pair<B, B>, A, B> =
  Optic.traversing(object : WanderF<Pair<A, A>, Pair<B, B>, A, B> {
    override fun <F> invoke(AF: Applicative<F>, source: Pair<A, A>, f: (A) -> Kind<F, B>): Kind<F, Pair<B, B>> =
      AF.ap(AF.map(f(source.first)) { fst -> { snd: B -> fst to snd } }, f(source.second))
  })

@JvmName("pair_every_traversal")
fun <K : TraversalK, I, S, T, A, B> Optic<K, I, S, T, Pair<A, A>, Pair<B, B>>.every(): Optic<TraversalK, I, S, T, A, B> =
  compose(Optic.pairEvery())

@JvmName("pair_every_fold")
fun <K : FoldK, I, S, T, A, B> Optic<K, I, S, T, Pair<A, A>, Pair<B, B>>.every(): Optic<FoldK, I, S, T, A, B> =
  compose(Optic.pairEvery())

@JvmName("pair_beside_traversal")
fun <K1 : TraversalK, K2 : TraversalK, I, J, A, B, C, D, E, F> Optic.Companion.pairBeside(
  l: Optic<K1, I, A, B, E, F>,
  r: Optic<K2, J, C, D, E, F>
): PTraversal<Pair<A, C>, Pair<B, D>, E, F> =
  traversing(object : WanderF<Pair<A, C>, Pair<B, D>, E, F> {
    override fun <G> invoke(AF: Applicative<G>, source: Pair<A, C>, f: (E) -> Kind<G, F>): Kind<G, Pair<B, D>> =
      AF.ap(
        AF.map(source.first.traverseOf(l, AF, f)) { b -> { d: D -> b to d } },
        source.second.traverseOf(r, AF, f)
      )
  })

@JvmName("pair_beside_fold")
fun <K1 : FoldK, K2 : FoldK, I, J, A, B, C, D, E, F, G> Optic.Companion.pairBeside(
  l: Optic<K1, I, A, B, C, D>,
  r: Optic<K2, J, E, F, C, G>
): Fold<Pair<A, E>, C> =
  folding(object : FoldF<Pair<A, E>, C> {
    override fun <F> invoke(AF: Applicative<F>, s: Pair<A, E>, f: (C) -> Kind<F, Unit>): Kind<F, Unit> =
      AF.ap(
        AF.map(s.first.traverseOf_(l, AF, f)) { { } },
        s.second.traverseOf_(r, AF, f)
      )
  })

@JvmName("pair_beside_traversal")
fun <K1 : TraversalK, K2 : TraversalK, K3 : TraversalK, I, J, K, S, T, A, B, C, D, E, F> Optic<K1, I, S, T, Pair<A, B>, Pair<C, D>>.beside(
  l: Optic<K2, J, A, C, E, F>,
  r: Optic<K3, K, B, D, E, F>
): Optic<TraversalK, I, S, T, E, F> =
  compose(Optic.pairBeside(l, r))

@JvmName("pair_beside_fold")
fun <K1 : FoldK, K2 : FoldK, K3 : FoldK, I, J, K, S, T, A, B, C, D, E, F, G> Optic<K1, I, S, T, Pair<A, E>, Pair<B, F>>.beside(
  l: Optic<K2, J, A, B, C, D>,
  r: Optic<K3, K, E, F, C, G>
): Optic<FoldK, I, S, T, C, Nothing> =
  compose(Optic.pairBeside(l, r))
