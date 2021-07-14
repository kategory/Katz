package arrow.optics.typeclasses

import arrow.optics.Fold
import arrow.optics.FoldF
import arrow.optics.FoldK
import arrow.optics.IxFold
import arrow.optics.Optic
import arrow.optics.PIxTraversal
import arrow.optics.Traversal
import arrow.optics.TraversalK
import arrow.optics.collectOf
import arrow.optics.combinators.deepOf
import arrow.optics.folding
import arrow.optics.internal.Applicative
import arrow.optics.internal.Kind
import arrow.optics.internal.WanderF
import arrow.optics.traversing
import kotlin.jvm.JvmName

fun interface Plated<A> {
  fun plate(): Traversal<A, A>

  companion object {
    fun <A> list(): Plated<List<A>> = Plated {
      Optic.traversing(object : WanderF<List<A>, List<A>, List<A>, List<A>> {
        override fun <F> invoke(
          AF: Applicative<F>,
          source: List<A>,
          f: (List<A>) -> Kind<F, List<A>>
        ): Kind<F, List<A>> =
          if (source.isEmpty()) AF.pure(source)
          else AF.map(f(source.drop(1))) { xs -> listOf(source.first()) + xs }
      })
    }
  }
}

@ExperimentalStdlibApi
@JvmName("deep_traversal")
fun <K : TraversalK, I, S, A, B> Plated<S>.deep(next: Optic<K, I, S, S, A, B>): PIxTraversal<I, S, S, A, B> =
  plate().deepOf(next)

@JvmName("deep_fold")
fun <K : FoldK, I, S, A, B> Plated<S>.deep(next: Optic<K, I, S, S, A, B>): IxFold<I, S, A> =
  plate().deepOf(next)

fun <S> Plated<S>.cosmos(): Fold<S, S> =
  Optic.folding(object : FoldF<S, S> {
    override fun <F> invoke(AF: Applicative<F>, s: S, f: (S) -> Kind<F, Unit>): Kind<F, Unit> {
      val plate = plate()
      val nextS = mutableListOf(s)
      var fUnit = AF.pure(Unit)
      while (nextS.isNotEmpty()) {
        val head = nextS.removeFirst()
        fUnit = AF.ap(AF.map(fUnit) { { } }, f(head))
        nextS.addAll(head.collectOf(plate))
      }
      return fUnit
    }
  })
