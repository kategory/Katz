package arrow.optics.predef

import arrow.core.Eval
import arrow.core.iterateRight
import arrow.optics.FoldK
import arrow.optics.IxFold
import arrow.optics.IxFoldF
import arrow.optics.Optic
import arrow.optics.PIxTraversal
import arrow.optics.TraversalK
import arrow.optics.compose
import arrow.optics.internal.Applicative
import arrow.optics.internal.IxWanderF
import arrow.optics.internal.Kind
import arrow.optics.ixFolding
import arrow.optics.ixTraversing

fun <A> Optic.Companion.foldedArray(): IxFold<Int, Array<A>, A> =
  Optic.ixFolding(object : IxFoldF<Int, Array<A>, A> {
    override fun <F> invoke(AF: Applicative<F>, s: Array<A>, f: (Int, A) -> Kind<F, Unit>): Kind<F, Unit> =
      s.foldIndexed(AF.pure(Unit)) { i, acc, a ->
        AF.ap(AF.map(acc) { { } }, f(i, a))
      }

    override fun <F> invokeLazy(AF: Applicative<F>, s: Array<A>, f: (Int, A) -> Kind<F, Unit>): Kind<F, Unit> =
      s.iterator().withIndex().iterateRight(Eval.now(AF.pure(Unit))) { (i, a), acc ->
        AF.apLazy(
          AF.map(f(i, a)) { {} },
          acc
        )
      }.value()
  })

fun <K : FoldK, I, S, T, A, B> Optic<K, I, S, T, Array<A>, B>.folded(): Optic<FoldK, I, S, T, A, Nothing> =
  compose(Optic.foldedArray())

fun <A, B> Optic.Companion.traversedArray(): PIxTraversal<Int, Array<A>, Array<B>, A, B> =
  Optic.ixTraversing(object : IxWanderF<Int, Array<A>, Array<B>, A, B> {
    override fun <F> invoke(AF: Applicative<F>, source: Array<A>, f: (Int, A) -> Kind<F, B>): Kind<F, Array<B>> {
      val buf = arrayOfNulls<Any?>(source.size) as Array<B>
      val fUnit = source.foldIndexed(AF.pure(Unit)) { i, acc, a ->
        AF.ap(
          AF.map(acc) { { b: B -> buf[i] = b } },
          f(i, a)
        )
      }
      return AF.map(fUnit) { buf }
    }

    override fun <F> invokeLazy(AF: Applicative<F>, source: Array<A>, f: (Int, A) -> Kind<F, B>): Kind<F, Array<B>> {
      val buf = arrayOfNulls<Any?>(source.size) as Array<B>
      val fUnit = source.iterator().withIndex().iterateRight(Eval.now(AF.pure(Unit))) { (i, a), acc ->
        AF.apLazy(
          AF.map(f(i, a)) { b: B -> { buf[i] = b } },
          acc
        )
      }.value()
      return AF.map(fUnit) { buf }
    }
  })

fun <K : TraversalK, I, S, T, A, B> Optic<K, I, S, T, Array<A>, Array<B>>.traversed(): Optic<TraversalK, I, S, T, A, B> =
  compose(Optic.traversedArray())

// TODO primitive array types
