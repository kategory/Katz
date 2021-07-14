package arrow.optics.typeclasses

import arrow.core.Either
import arrow.optics.AffineTraversal
import arrow.optics.Optic
import arrow.optics.PPrism
import arrow.optics.compose
import arrow.optics.predef.pairFirst
import arrow.optics.predef.pairSecond
import arrow.optics.prism

fun interface Cons<S, T, A, B> {
  fun cons(): PPrism<S, T, Pair<A, S>, Pair<B, T>>

  companion object {
    fun <A, B> list(): Cons<List<A>, List<B>, A, B> = Cons {
      Optic.prism({ s ->
        if (s.isEmpty()) Either.Left(emptyList())
        else Either.Right(s.first() to s.drop(1))
      }, { (b, xs) -> listOf(b) + xs })
    }
    fun string(): Cons<String, String, Char, Char> = Cons {
      Optic.prism({ s ->
        if (s.isEmpty()) Either.Left(s)
        else Either.Right(s.first() to s.drop(1))
      }, { (c, str) -> "$c$str" })
    }
    inline fun <A, reified B> array(): Cons<Array<A>, Array<B>, A, B> = Cons {
      Optic.prism({ s ->
        if (s.isEmpty()) Either.Left(emptyArray())
        else Either.Right(s[0] to s.copyOfRange(1, s.size))
      }, { (b, xs) -> arrayOf(b, *xs) })
    }
  }
}

fun <S, A> Cons<S, S, A, A>.head(): AffineTraversal<S, A> = cons().compose(Optic.pairFirst())
fun <S, A> Cons<S, S, A, A>.tail(): AffineTraversal<S, S> = cons().compose(Optic.pairSecond())
