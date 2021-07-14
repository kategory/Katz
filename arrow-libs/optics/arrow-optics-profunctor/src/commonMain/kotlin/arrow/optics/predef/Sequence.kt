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
import kotlin.jvm.JvmName

fun <A> Optic.Companion.foldedSequence(): IxFold<Int, Sequence<A>, A> =
  Optic.ixFolding(object : IxFoldF<Int, Sequence<A>, A> {
    override fun <F> invoke(AF: Applicative<F>, s: Sequence<A>, f: (Int, A) -> Kind<F, Unit>): Kind<F, Unit> =
      s.foldIndexed(AF.pure(Unit)) { i, acc, a ->
        AF.ap(AF.map(acc) { { } }, f(i, a))
      }

    override fun <F> invokeLazy(AF: Applicative<F>, s: Sequence<A>, f: (Int, A) -> Kind<F, Unit>): Kind<F, Unit> =
      s.withIndex().iterator().iterateRight(Eval.now(AF.pure(Unit))) { (i, a), acc ->
        AF.apLazy(AF.map(f(i, a)) { { } }, acc)
      }.value()
  })

@JvmName("sequence_folded")
fun <K : FoldK, I, S, T, A, B> Optic<K, I, S, T, Sequence<A>, B>.folded(): Optic<FoldK, I, S, T, A, Nothing> =
  compose(Optic.foldedSequence())

fun <A, B> Optic.Companion.traversedSequence(): PIxTraversal<Int, Sequence<A>, Sequence<B>, A, B> =
  Optic.ixTraversing(object : IxWanderF<Int, Sequence<A>, Sequence<B>, A, B> {
    override fun <F> invoke(AF: Applicative<F>, source: Sequence<A>, f: (Int, A) -> Kind<F, B>): Kind<F, Sequence<B>> {
      val buf = mutableListOf<B>()
      val fUnit = source.foldIndexed(AF.pure(Unit)) { i, acc, a ->
        AF.ap(
          AF.map(acc) { { b: B -> buf += b } },
          f(i, a)
        )
      }
      return AF.map(fUnit) { buf.asSequence() }
    }

    override fun <F> invokeLazy(
      AF: Applicative<F>,
      source: Sequence<A>,
      f: (Int, A) -> Kind<F, B>
    ): Kind<F, Sequence<B>> {
      val buf = mutableListOf<B>()
      val fUnit = source.withIndex().iterator().iterateRight(Eval.now(AF.pure(Unit))) { (i, a), acc ->
        AF.apLazy(
          AF.map(f(i, a)) { b -> { buf += b } },
          acc
        )
      }.value()
      return AF.map(fUnit) { buf.asSequence() }
    }
  })

@JvmName("sequence_traversed")
fun <K : TraversalK, I, S, T, A, B> Optic<K, I, S, T, Sequence<A>, Sequence<B>>.traversed(): Optic<TraversalK, I, S, T, A, B> =
  compose(Optic.traversedSequence())
