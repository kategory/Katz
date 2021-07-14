package arrow.optics.predef

import arrow.core.Eval
import arrow.core.iterateRight
import arrow.optics.FoldK
import arrow.optics.IxFold
import arrow.optics.IxFoldF
import arrow.optics.Optic
import arrow.optics.compose
import arrow.optics.internal.Applicative
import arrow.optics.internal.Kind
import arrow.optics.ixFolding
import kotlin.jvm.JvmName

@JvmName("iterable_folded")
fun <S : Iterable<A>, A> Optic.Companion.foldedIterable(): IxFold<Int, S, A> =
  Optic.ixFolding(object : IxFoldF<Int, S, A> {
    override fun <F> invoke(AF: Applicative<F>, s: S, f: (Int, A) -> Kind<F, Unit>): Kind<F, Unit> =
      s.foldIndexed(AF.pure(Unit)) { i, acc, a ->
        AF.ap(AF.map(acc) { { } }, f(i, a))
      }

    override fun <F> invokeLazy(AF: Applicative<F>, s: S, f: (Int, A) -> Kind<F, Unit>): Kind<F, Unit> {
      return s.iterator().withIndex()
        .iterateRight(Eval.now(AF.pure(Unit))) { (i, a), acc ->
          AF.apLazy(AF.map(f(i, a)) { { } }, acc)
        }.value()
    }
  })

@JvmName("iterable_folded")
fun <K : FoldK, I, S, T, IA : Iterable<A>, A, B> Optic<K, I, S, T, IA, B>.foldedIterable(): Optic<FoldK, I, S, T, A, Nothing> =
  compose(Optic.foldedIterable())
