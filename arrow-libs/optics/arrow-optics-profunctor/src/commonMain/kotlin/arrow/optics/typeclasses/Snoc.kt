package arrow.optics.typeclasses

import arrow.core.Either
import arrow.optics.AffineTraversal
import arrow.optics.Optic
import arrow.optics.PPrism
import arrow.optics.compose
import arrow.optics.predef.pairFirst
import arrow.optics.predef.pairSecond
import arrow.optics.prism

fun interface Snoc<S, T, A, B> {
  fun snoc(): PPrism<S, T, Pair<S, A>, Pair<T, B>>

  companion object {
    fun <A, B> list(): Snoc<List<A>, List<B>, A, B> = Snoc {
      Optic.prism({ s ->
        if (s.isEmpty()) Either.Left(emptyList())
        else Either.Right(s.dropLast(1) to s.last())
      }, { (xs, b) -> xs + b })
    }
    fun string(): Snoc<String, String, Char, Char> = Snoc {
      Optic.prism({ s ->
        if (s.isEmpty()) Either.Left(s)
        else Either.Right(s.dropLast(1) to s.last())
      }, { (str, c) -> "$str$c" })
    }
    inline fun <A, reified B> array(): Snoc<Array<A>, Array<B>, A, B> = Snoc {
      Optic.prism({ s ->
        if (s.isEmpty()) Either.Left(emptyArray())
        else Either.Right(s.copyOfRange(0, s.size - 1) to s[s.size - 1])
      }, { (xs, b) -> xs + b })
    }
  }
}

fun <S, A> Snoc<S, S, A, A>.init(): AffineTraversal<S, S> = snoc().compose(Optic.pairFirst())
fun <S, A> Snoc<S, S, A, A>.last(): AffineTraversal<S, A> = snoc().compose(Optic.pairSecond())
