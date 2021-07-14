package arrow.optics.predef

import arrow.core.Eval
import arrow.core.iterateRight
import arrow.optics.FoldK
import arrow.optics.Optic
import arrow.optics.PIxTraversal
import arrow.optics.TraversalK
import arrow.optics.compose
import arrow.optics.internal.Applicative
import arrow.optics.internal.IxWanderF
import arrow.optics.internal.Kind
import arrow.optics.ixTraversing
import kotlin.jvm.JvmName

fun <K, A, B> Optic.Companion.traversedMap(): PIxTraversal<K, Map<K, A>, Map<K, B>, A, B> =
  ixTraversing(object : IxWanderF<K, Map<K, A>, Map<K, B>, A, B> {
    override fun <F> invoke(AF: Applicative<F>, source: Map<K, A>, f: (K, A) -> Kind<F, B>): Kind<F, Map<K, B>> {
      val buf = mutableMapOf<K, B>()
      val funit = source.entries.fold(AF.pure(Unit)) { acc, (k, a) ->
        AF.ap(
          AF.map(acc) { { b -> buf[k] = b } },
          f(k, a)
        )
      }
      return AF.map(funit) { buf }
    }
    override fun <F> invokeLazy(AF: Applicative<F>, source: Map<K, A>, f: (K, A) -> Kind<F, B>): Kind<F, Map<K, B>> {
      val buf = mutableMapOf<K, B>()
      val funit = source.iterator().iterateRight(Eval.now(AF.pure(Unit))) { (k, a), acc ->
        AF.apLazy(AF.map(f(k, a)) { b -> { buf[k] = b } }, acc)
      }.value()
      return AF.map(funit) { buf }
    }
  })

@JvmName("map_traversal")
fun <K : TraversalK, I, S, T, Key, A, B> Optic<K, I, S, T, Map<Key, A>, Map<Key, B>>.traversed(): PIxTraversal<I, S, T, A, B> =
  compose(Optic.traversedMap())

@JvmName("map_fold")
fun <K : FoldK, I, S, Key, A> Optic<K, I, S, S, Map<Key, A>, Map<Key, A>>.traversed(): Optic<FoldK, I, S, S, A, Nothing> =
  compose(Optic.traversedMap())
