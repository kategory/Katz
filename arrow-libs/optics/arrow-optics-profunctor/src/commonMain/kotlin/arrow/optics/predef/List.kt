package arrow.optics.predef

import arrow.core.Eval
import arrow.core.iterateRight
import arrow.optics.IxFold
import arrow.optics.Optic
import arrow.optics.PIxTraversal
import arrow.optics.TraversalK
import arrow.optics.compose
import arrow.optics.internal.Applicative
import arrow.optics.internal.IxWanderF
import arrow.optics.internal.Kind
import arrow.optics.ixTraversing
import kotlin.jvm.JvmName

fun <A> Optic.Companion.foldedList(): IxFold<Int, List<A>, A> = foldedIterable()

fun <A, B> Optic.Companion.traversedList(): PIxTraversal<Int, List<A>, List<B>, A, B> =
  ixTraversing(object : IxWanderF<Int, List<A>, List<B>, A, B> {
    override fun <F> invoke(AF: Applicative<F>, source: List<A>, f: (Int, A) -> Kind<F, B>): Kind<F, List<B>> {
      val mutList = mutableListOf<B>()
      val fa = source.foldIndexed(AF.pure(Unit)) { i, acc, a ->
        AF.ap(AF.map(acc) { { a -> mutList += a } }, f(i, a))
      }
      return AF.map(fa) { mutList }
    }

    override fun <F> invokeLazy(AF: Applicative<F>, source: List<A>, f: (Int, A) -> Kind<F, B>): Kind<F, List<B>> {
      val mutList = mutableListOf<B>()
      val fUnit = source.iterator().withIndex().iterateRight(Eval.now(AF.pure(Unit))) { (i, a), acc ->
        AF.apLazy(AF.map(f(i, a)) { b: B -> { mutList += b } }, acc)
      }.value()
      return AF.map(fUnit) { mutList }
    }
  })

@JvmName("list_traversed")
fun <K : TraversalK, I, S, T, A, B> Optic<K, I, S, T, List<A>, List<B>>.traversed(): Optic<TraversalK, I, S, T, A, B> =
  compose(Optic.traversedList())
