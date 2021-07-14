package arrow.typeclasses

import arrow.core.Const
import arrow.core.Either
import arrow.core.Endo
import arrow.core.Eval
import arrow.core.None
import arrow.core.Option
import arrow.core.Validated
import arrow.core.combine
import arrow.core.combineAll
import arrow.core.compose
import arrow.core.flatten
import arrow.core.identity
import kotlin.jvm.JvmName
import kotlin.jvm.JvmStatic
import kotlin.collections.plus as _plus

interface Monoid<A> : Semigroup<A> {
  /**
   * A zero value for this A
   */
  fun empty(): A

  /**
   * Combine an [Collection] of [A] values.
   */
  fun Collection<A>.combineAll(): A =
    if (isEmpty()) empty() else reduce { a, b -> a.combine(b) }

  /**
   * Combine an array of [A] values.
   */
  fun combineAll(elems: List<A>): A = elems.combineAll()

  companion object {
    @JvmStatic
    @JvmName("Boolean")
    fun boolean(): Monoid<Boolean> = AndMonoid

    @JvmStatic
    @JvmName("Byte")
    fun byte(): Monoid<Byte> = ByteMonoid

    @JvmStatic
    @JvmName("Double")
    @Deprecated(DoubleInstanceDeprecation)
    fun double(): Monoid<Double> = DoubleMonoid

    @JvmStatic
    @JvmName("Integer")
    fun int(): Monoid<Int> = IntMonoid

    @JvmStatic
    @JvmName("Long")
    fun long(): Monoid<Long> = LongMonoid

    @JvmStatic
    @JvmName("Short")
    fun short(): Monoid<Short> = ShortMonoid

    @JvmStatic
    @JvmName("Float")
    @Deprecated(FloatInstanceDeprecation)
    fun float(): Monoid<Float> = FloatMonoid

    @JvmStatic
    fun <A> list(): Monoid<List<A>> = ListMonoid as Monoid<List<A>>

    @JvmStatic
    fun <A> sequence(): Monoid<Sequence<A>> = SequenceMonoid as Monoid<Sequence<A>>

    @JvmStatic
    fun string(): Monoid<String> = StringMonoid

    @JvmStatic
    fun <A, B> either(MA: Monoid<A>, MB: Monoid<B>): Monoid<Either<A, B>> =
      EitherMonoid(MA, MB)

    @JvmStatic
    fun <A> endo(): Monoid<Endo<A>> =
      object : Monoid<Endo<A>> {
        override fun empty(): Endo<A> = Endo(::identity)
        override fun Endo<A>.combine(g: Endo<A>): Endo<A> = Endo(f.compose(g.f))
      }

    @JvmStatic
    @JvmName("constant")
    fun <A, T> const(MA: Monoid<A>): Monoid<Const<A, T>> =
      object : Monoid<Const<A, T>> {
        override fun empty(): Const<A, T> = Const(MA.empty())
        override fun Const<A, T>.combine(b: Const<A, T>): Const<A, T> = this.combine(MA, b)
      }

    @JvmStatic
    fun <K, A> map(SG: Semigroup<A>): Monoid<Map<K, A>> =
      MapMonoid(SG)

    @JvmStatic
    fun <A> option(MA: Semigroup<A>): Monoid<Option<A>> =
      OptionMonoid(MA)

    @JvmStatic
    fun <E, A> validated(SE: Semigroup<E>, MA: Monoid<A>): Monoid<Validated<E, A>> =
      ValidatedMonoid(SE, MA)

    @JvmStatic
    fun <A, B> pair(MA: Monoid<A>, MB: Monoid<B>): Monoid<Pair<A, B>> =
      PairMonoid(MA, MB)

    private class ValidatedMonoid<A, B>(
      private val SA: Semigroup<A>,
      private val MB: Monoid<B>
    ) : Monoid<Validated<A, B>> {
      private val empty = Validated.Valid(MB.empty())
      override fun empty(): Validated<A, B> = empty
      override fun Validated<A, B>.combine(b: Validated<A, B>): Validated<A, B> =
        combine(SA, MB, b)
    }

    private class OptionMonoid<A>(
      private val MA: Semigroup<A>
    ) : Monoid<Option<A>> {

      override fun Option<A>.combine(b: Option<A>): Option<A> =
        combine(MA, b)

      override fun Option<A>.maybeCombine(b: Option<A>?): Option<A> =
        b?.let { combine(MA, it) } ?: this

      override fun empty(): Option<A> = None
    }

    private class MapMonoid<K, A>(private val SG: Semigroup<A>) : Monoid<Map<K, A>> {
      override fun empty(): Map<K, A> = emptyMap()

      override fun Map<K, A>.combine(b: Map<K, A>): Map<K, A> =
        combine(SG, b)
    }

    private object AndMonoid : Monoid<Boolean> {
      override fun Boolean.combine(b: Boolean): Boolean = this && b
      override fun Boolean.combineLazy(b: Eval<Boolean>): Eval<Boolean> =
        if (this) b else Eval.now(false)
      override fun empty(): Boolean = true
    }

    private object ByteMonoid : Monoid<Byte> {
      override fun empty(): Byte = 0
      override fun Byte.combine(b: Byte): Byte = (this + b).toByte()
    }

    private object DoubleMonoid : Monoid<Double> {
      override fun empty(): Double = .0
      override fun Double.combine(b: Double): Double = this + b
    }

    private object IntMonoid : Monoid<Int> {
      override fun empty(): Int = 0
      override fun Int.combine(b: Int): Int = this + b
    }

    private object LongMonoid : Monoid<Long> {
      override fun empty(): Long = 0L
      override fun Long.combine(b: Long): Long = this + b
    }

    private object ShortMonoid : Monoid<Short> {
      override fun empty(): Short = 0
      override fun Short.combine(b: Short): Short = (this + b).toShort()
    }

    private object FloatMonoid : Monoid<Float> {
      override fun empty(): Float = 0f
      override fun Float.combine(b: Float): Float = this + b
    }

    private object StringMonoid : Monoid<String> {
      override fun String.combine(b: String): String = "${this}$b"
      override fun empty(): String = ""
    }

    private object ListMonoid : Monoid<List<Any?>> {
      override fun empty(): List<Any?> = emptyList()
      override fun List<Any?>.combine(b: List<Any?>): List<Any?> = this._plus(b)
    }

    private object SequenceMonoid : Monoid<Sequence<Any?>> {
      override fun empty(): Sequence<Any?> = emptySequence()
      override fun Sequence<Any?>.combine(b: Sequence<Any?>): Sequence<Any?> = sequenceOf(this, b).flatten()
    }

    private class EitherMonoid<L, R>(
      private val MOL: Monoid<L>,
      private val MOR: Monoid<R>
    ) : Monoid<Either<L, R>> {
      override fun empty(): Either<L, R> = Either.Right(MOR.empty())

      override fun Either<L, R>.combine(b: Either<L, R>): Either<L, R> =
        combine(MOL, MOR, b)

      override fun Collection<Either<L, R>>.combineAll(): Either<L, R> =
        combineAll(MOL, MOR)

      override fun Either<L, R>.maybeCombine(b: Either<L, R>?): Either<L, R> =
        b?.let { combine(MOL, MOR, it) } ?: this

      override fun combineAll(elems: List<Either<L, R>>): Either<L, R> =
        elems.combineAll(MOL, MOR)
    }

    private class PairMonoid<A, B>(
      private val MA: Monoid<A>,
      private val MB: Monoid<B>
    ) : Monoid<Pair<A, B>> {
      override fun empty(): Pair<A, B> = Pair(MA.empty(), MB.empty())
      override fun Pair<A, B>.combine(b: Pair<A, B>): Pair<A, B> = combine(MA, MB, b)
    }
  }
}
