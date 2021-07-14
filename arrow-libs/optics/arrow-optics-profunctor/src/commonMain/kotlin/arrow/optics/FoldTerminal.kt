package arrow.optics

import arrow.core.Eval
import arrow.core.identity
import arrow.optics.combinators.filter
import arrow.optics.combinators.get
import arrow.optics.combinators.ixFilter
import arrow.optics.internal.Applicative
import arrow.optics.internal.Kind
import arrow.typeclasses.Monoid

fun <K : FoldK, I, S, T, A, B> S.foldOf(optic: Optic<K, I, S, T, A, B>, MA: Monoid<A>): A =
  foldOf(optic, MA, ::identity)
fun <K : FoldK, I, S, T, A, B> S.foldLazyOf(optic: Optic<K, I, S, T, A, B>, MA: Monoid<A>): A =
  foldLazyOf(optic, MA, ::identity)

fun <K : FoldK, I, S, T, A, B, F> S.traverseOf_(optic: Optic<K, I, S, T, A, B>, AF: Applicative<F>, f: (A) -> Kind<F, Unit>): Kind<F, Unit> =
  foldOf(optic, Monoid.ap(AF), f)

fun <K : FoldK, I, S, T, A, B, F> S.traverseLazyOf_(optic: Optic<K, I, S, T, A, B>, AF: Applicative<F>, f: (A) -> Kind<F, Unit>): Kind<F, Unit> =
  foldLazyOf(optic, Monoid.ap(AF), f)

fun <K : FoldK, I, S, T, A, B, F> S.ixTraverseOf_(optic: Optic<K, I, S, T, A, B>, AF: Applicative<F>, f: (I, A) -> Kind<F, Unit>): Kind<F, Unit> =
  ixFoldOf(optic, Monoid.ap(AF), f)

fun <K : FoldK, I, S, T, A, B, F> S.ixTraverseLazyOf_(optic: Optic<K, I, S, T, A, B>, AF: Applicative<F>, f: (I, A) -> Kind<F, Unit>): Kind<F, Unit> =
  ixFoldLazyOf(optic, Monoid.ap(AF), f)

fun <K : FoldK, I, S, T, A, B> S.collectOf(optic: Optic<K, I, S, T, A, B>): List<A> =
  foldOf(optic, Monoid.list()) { listOf(it) }

fun <K : FoldK, I, S, T, A, B> S.ixCollectOf(optic: Optic<K, I, S, T, A, B>): List<Pair<I, A>> =
  ixFoldOf(optic, Monoid.list()) { i, a -> listOf(i to a) }

fun <K : FoldK, I, S, T, B> S.sumOf(optic: Optic<K, I, S, T, Int, B>): Int =
  foldOf(optic, Monoid.int())

fun <K : FoldK, I, S, T, A, B> S.lengthOf(optic: Optic<K, I, S, T, A, B>): Int =
  sumOf(optic.get { 1 })

fun <K : FoldK, I, S, T, A, B> S.isNotEmpty(optic: Optic<K, I, S, T, A, B>): Boolean =
  foldLazyOf(optic, Monoid.booleanOr()) { true }
fun <K : FoldK, I, S, T, A, B> S.isEmpty(optic: Optic<K, I, S, T, A, B>): Boolean =
  foldLazyOf(optic, Monoid.boolean()) { false }

fun <K : FoldK, I, S, T, A, B> S.anyOf(optic: Optic<K, I, S, T, A, B>, f: (A) -> Boolean): Boolean =
  foldLazyOf(optic, Monoid.booleanOr(), f)
fun <K : FoldK, I, S, T, A, B> S.ixAnyOf(optic: Optic<K, I, S, T, A, B>, f: (I, A) -> Boolean): Boolean =
  ixFoldLazyOf(optic, Monoid.booleanOr(), f)

fun <K : FoldK, I, S, T, A, B> S.allOf(optic: Optic<K, I, S, T, A, B>, f: (A) -> Boolean): Boolean =
  foldLazyOf(optic, Monoid.boolean(), f)
fun <K : FoldK, I, S, T, A, B> S.ixAllOf(optic: Optic<K, I, S, T, A, B>, f: (I, A) -> Boolean): Boolean =
  ixFoldLazyOf(optic, Monoid.boolean(), f)

fun <K : FoldK, I, S, T, A, B> S.noneOf(optic: Optic<K, I, S, T, A, B>, f: (A) -> Boolean): Boolean =
  anyOf(optic, f).not()
fun <K : FoldK, I, S, T, A, B> S.ixNoneOf(optic: Optic<K, I, S, T, A, B>, f: (I, A) -> Boolean): Boolean =
  ixAnyOf(optic, f).not()

fun <K : FoldK, I, S, T, A, B> S.firstOrNull(optic: Optic<K, I, S, T, A, B>): A? =
  foldLazyOf(optic, Monoid.first()) { it }

fun <K : FoldK, I, S, T, A, B> S.ixFirstOrNull(optic: Optic<K, I, S, T, A, B>): Pair<I, A>? =
  ixFoldLazyOf(optic, Monoid.first()) { i, a -> i to a }

fun <K : FoldK, I, S, T, A, B> S.firstOrNull(optic: Optic<K, I, S, T, A, B>, f: (A) -> Boolean): A? =
  foldLazyOf(optic.filter(f), Monoid.first()) { it }

fun <K : FoldK, I, S, T, A, B> S.ixFirstOrNull(optic: Optic<K, I, S, T, A, B>, f: (I, A) -> Boolean): Pair<I, A>? =
  ixFoldLazyOf(optic.ixFilter(f), Monoid.first()) { i, a -> i to a }

fun <K : FoldK, I, S, T, A, B> S.lastOrNull(optic: Optic<K, I, S, T, A, B>): A? =
  foldOf(optic, Monoid.last()) { it }

fun <K : FoldK, I, S, T, A, B> S.ixLastOrNull(optic: Optic<K, I, S, T, A, B>): Pair<I, A>? =
  ixFoldOf(optic, Monoid.last()) { i, a -> i to a }

fun <K : FoldK, I, S, T, A, B> S.lastOrNull(optic: Optic<K, I, S, T, A, B>, f: (A) -> Boolean): A? =
  foldOf(optic.filter(f), Monoid.last()) { it }

fun <K : FoldK, I, S, T, A, B> S.ixLastOrNull(optic: Optic<K, I, S, T, A, B>, f: (I, A) -> Boolean): Pair<I, A>? =
  ixFoldOf(optic.ixFilter(f), Monoid.last()) { i, a -> i to a }

fun <K : FoldK, I, S, T, A : Comparable<A>, B> S.maximumOf(optic: Optic<K, I, S, T, A, B>): A? =
  foldOf(optic, Monoid.max()) { it }

fun <K : FoldK, I, S, T, A : Comparable<A>, B> S.minimumOf(optic: Optic<K, I, S, T, A, B>): A? =
  foldOf(optic, Monoid.min()) { it }

fun <K : FoldK, I, S, T, A, B, C : Comparable<C>> S.maximumByOf(optic: Optic<K, I, S, T, A, B>, f: (A) -> C): A? =
  foldOf(optic, Monoid.maxBy(f)) { it }

fun <K : FoldK, I, S, T, A, B, C : Comparable<C>> S.minimumByOf(optic: Optic<K, I, S, T, A, B>, f: (A) -> C): A? =
  foldOf(optic, Monoid.minBy(f)) { it }

// Monoids used in the folds
internal fun <F> Monoid.Companion.ap(AF: Applicative<F>) = object : Monoid<Kind<F, Unit>> {
  override fun empty(): Kind<F, Unit> = AF.pure(Unit)
  override fun Kind<F, Unit>.combine(b: Kind<F, Unit>): Kind<F, Unit> =
    AF.ap(AF.map(this) { _ -> { _: Unit -> Unit } }, b)
  override fun Kind<F, Unit>.combineLazy(b: Eval<Kind<F, Unit>>): Eval<Kind<F, Unit>> =
    AF.apLazy(AF.map(this) { { } }, b)
}
internal fun Monoid.Companion.booleanOr(): Monoid<Boolean> =
  object : Monoid<Boolean> {
    override fun empty(): Boolean = false
    override fun Boolean.combine(b: Boolean): Boolean = this || b
    override fun Boolean.combineLazy(b: Eval<Boolean>): Eval<Boolean> =
      if (this) Eval.now(true) else b
  }
internal fun <A> Monoid.Companion.first() = object : Monoid<A?> {
  override fun A?.combine(b: A?): A? = this ?: b
  override fun A?.combineLazy(b: Eval<A?>): Eval<A?> =
    this?.let { Eval.now(it) } ?: b
  override fun empty(): A? = null
}
internal fun <A> Monoid.Companion.last() = object : Monoid<A?> {
  override fun A?.combine(b: A?): A? = b ?: this
  override fun empty(): A? = null
}
internal fun <A : Comparable<A>> Monoid.Companion.max() = object : Monoid<A?> {
  override fun A?.combine(b: A?): A? = when {
    this == null -> b
    b == null -> this
    this < b -> b
    else -> this
  }
  override fun empty(): A? = null
}
internal fun <A : Comparable<A>> Monoid.Companion.min() = object : Monoid<A?> {
  override fun A?.combine(b: A?): A? = when {
    this == null -> b
    b == null -> this
    this < b -> this
    else -> b
  }
  override fun empty(): A? = null
}
// Ideally we'd cache the current max/min objects but for now this works
internal fun <A, C : Comparable<C>> Monoid.Companion.maxBy(f: (A) -> C) = object : Monoid<A?> {
  override fun A?.combine(b: A?): A? = when {
    this == null -> b
    b == null -> this
    f(this) < f(b) -> b
    else -> this
  }
  override fun empty(): A? = null
}
internal fun <A, C : Comparable<C>> Monoid.Companion.minBy(f: (A) -> C) = object : Monoid<A?> {
  override fun A?.combine(b: A?): A? = when {
    this == null -> b
    b == null -> this
    f(this) < f(b) -> this
    else -> b
  }
  override fun empty(): A? = null
}
