@file:Suppress("unused", "FunctionName")

package arrow.core

import arrow.typeclasses.Monoid
import arrow.typeclasses.Semigroup
import kotlin.collections.foldRight as _foldRight

inline fun <B, C, D, E> Iterable<B>.zip(
  c: Iterable<C>,
  d: Iterable<D>,
  map: (B, C, D) -> E
): List<E> =
  zip(c, d, unit, unit, unit, unit, unit, unit, unit) { b, c, d, _, _, _, _, _, _, _ -> map(b, c, d) }

inline fun <B, C, D, E, F> Iterable<B>.zip(
  c: Iterable<C>,
  d: Iterable<D>,
  e: Iterable<E>,
  map: (B, C, D, E) -> F
): List<F> =
  zip(c, d, e, unit, unit, unit, unit, unit, unit) { b, c, d, e, _, _, _, _, _, _ -> map(b, c, d, e) }

inline fun <B, C, D, E, F, G> Iterable<B>.zip(
  c: Iterable<C>,
  d: Iterable<D>,
  e: Iterable<E>,
  f: Iterable<F>,
  map: (B, C, D, E, F) -> G
): List<G> =
  zip(c, d, e, f, unit, unit, unit, unit, unit) { b, c, d, e, f, _, _, _, _, _ -> map(b, c, d, e, f) }

inline fun <B, C, D, E, F, G, H> Iterable<B>.zip(
  c: Iterable<C>,
  d: Iterable<D>,
  e: Iterable<E>,
  f: Iterable<F>,
  g: Iterable<G>,
  map: (B, C, D, E, F, G) -> H
): List<H> =
  zip(c, d, e, f, g, unit, unit, unit, unit) { b, c, d, e, f, g, _, _, _, _ -> map(b, c, d, e, f, g) }

inline fun <B, C, D, E, F, G, H, I> Iterable<B>.zip(
  c: Iterable<C>,
  d: Iterable<D>,
  e: Iterable<E>,
  f: Iterable<F>,
  g: Iterable<G>,
  h: Iterable<H>,
  map: (B, C, D, E, F, G, H) -> I
): List<I> =
  zip(c, d, e, f, g, h, unit, unit, unit) { b, c, d, e, f, g, h, _, _, _ -> map(b, c, d, e, f, g, h) }

inline fun <B, C, D, E, F, G, H, I, J> Iterable<B>.zip(
  c: Iterable<C>,
  d: Iterable<D>,
  e: Iterable<E>,
  f: Iterable<F>,
  g: Iterable<G>,
  h: Iterable<H>,
  i: Iterable<I>,
  map: (B, C, D, E, F, G, H, I) -> J
): List<J> =
  zip(c, d, e, f, g, h, i, unit, unit) { b, c, d, e, f, g, h, i, _, _ -> map(b, c, d, e, f, g, h, i) }

inline fun <B, C, D, E, F, G, H, I, J, K> Iterable<B>.zip(
  c: Iterable<C>,
  d: Iterable<D>,
  e: Iterable<E>,
  f: Iterable<F>,
  g: Iterable<G>,
  h: Iterable<H>,
  i: Iterable<I>,
  j: Iterable<J>,
  map: (B, C, D, E, F, G, H, I, J) -> K
): List<K> =
  zip(c, d, e, f, g, h, i, j, unit) { b, c, d, e, f, g, h, i, j, _ -> map(b, c, d, e, f, g, h, i, j) }

inline fun <B, C, D, E, F, G, H, I, J, K, L> Iterable<B>.zip(
  c: Iterable<C>,
  d: Iterable<D>,
  e: Iterable<E>,
  f: Iterable<F>,
  g: Iterable<G>,
  h: Iterable<H>,
  i: Iterable<I>,
  j: Iterable<J>,
  k: Iterable<K>,
  map: (B, C, D, E, F, G, H, I, J, K) -> L
): List<L> {
  val buffer = ArrayList<L>()
  for (bb in this) {
    for (cc in c) {
      for (dd in d) {
        for (ee in e) {
          for (ff in f) {
            for (gg in g) {
              for (hh in h) {
                for (ii in i) {
                  for (jj in j) {
                    for (kk in k) {
                      buffer.add(map(bb, cc, dd, ee, ff, gg, hh, ii, jj, kk))
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }
  return buffer
}

inline fun <A, B> Iterable<A>.foldRight(initial: B, operation: (A, acc: B) -> B): B =
  when (this) {
    is List -> _foldRight(initial, operation)
    else -> reversed().fold(initial) { acc, a -> operation(a, acc) }
  }

fun <A, B> Iterable<A>.ap(ff: Iterable<(A) -> B>): List<B> =
  flatMap { a -> ff.map { f -> f(a) } }

inline fun <E, A, B> Iterable<A>.traverseEither(f: (A) -> Either<E, B>): Either<E, List<B>> =
  foldRight<A, Either<E, List<B>>>(emptyList<B>().right()) { a, acc ->
    f(a).ap(acc.map { bs -> { b: B -> listOf(b) + bs } })
  }

inline fun <E, A, B> Iterable<A>.flatTraverseEither(f: (A) -> Either<E, Iterable<B>>): Either<E, List<B>> =
  foldRight<A, Either<E, List<B>>>(emptyList<B>().right()) { a, acc ->
    f(a).ap(acc.map { bs -> { b: Iterable<B> -> b + bs } })
  }

inline fun <E, A> Iterable<A>.traverseEither_(f: (A) -> Either<E, *>): Either<E, Unit> {
  val void = { _: Unit -> { _: Any? -> Unit } }
  return foldRight<A, Either<E, Unit>>(Unit.right()) { a, acc ->
    f(a).ap(acc.map(void))
  }
}

fun <E, A> Iterable<Either<E, A>>.sequenceEither(): Either<E, List<A>> =
  traverseEither(::identity)

fun <E, A> Iterable<Either<E, Iterable<A>>>.flatSequenceEither(): Either<E, List<A>> =
  flatTraverseEither(::identity)

fun <E> Iterable<Either<E, *>>.sequenceEither_(): Either<E, Unit> =
  traverseEither_(::identity)

inline fun <E, A, B> Iterable<A>.traverseValidated(semigroup: Semigroup<E>, f: (A) -> Validated<E, B>): Validated<E, List<B>> =
  foldRight<A, Validated<E, List<B>>>(emptyList<B>().valid()) { a, acc ->
    f(a).ap(semigroup, acc.map { bs -> { b: B -> listOf(b) + bs } })
  }

inline fun <E, A, B> Iterable<A>.flatTraverseValidated(semigroup: Semigroup<E>, f: (A) -> Validated<E, Iterable<B>>): Validated<E, List<B>> =
  foldRight<A, Validated<E, List<B>>>(emptyList<B>().valid()) { a, acc ->
    f(a).ap(semigroup, acc.map { bs -> { b: Iterable<B> -> b + bs } })
  }

inline fun <E, A> Iterable<A>.traverseValidated_(semigroup: Semigroup<E>, f: (A) -> Validated<E, *>): Validated<E, Unit> =
  foldRight<A, Validated<E, Unit>>(Unit.valid()) { a, acc ->
    f(a).ap(semigroup, acc.map { { Unit } })
  }

fun <E, A> Iterable<Validated<E, A>>.sequenceValidated(semigroup: Semigroup<E>): Validated<E, List<A>> =
  traverseValidated(semigroup, ::identity)

fun <E, A> Iterable<Validated<E, Iterable<A>>>.flatSequenceValidated(semigroup: Semigroup<E>): Validated<E, List<A>> =
  flatTraverseValidated(semigroup, ::identity)

fun <E> Iterable<Validated<E, *>>.sequenceValidated_(semigroup: Semigroup<E>): Validated<E, Unit> =
  traverseValidated_(semigroup, ::identity)

fun <A, B> Iterable<A>.mapConst(b: B): List<B> =
  map { b }

fun <A> Iterable<A>.void(): List<Unit> =
  mapConst(Unit)

fun <A, B> List<A>.foldRight(lb: Eval<B>, f: (A, Eval<B>) -> Eval<B>): Eval<B> {
  fun loop(fa_p: List<A>): Eval<B> = when {
    fa_p.isEmpty() -> lb
    else -> f(fa_p.first(), Eval.defer { loop(fa_p.drop(1)) })
  }

  return Eval.defer { loop(this) }
}

fun <A, B> Iterable<A>.reduceOrNull(initial: (A) -> B, operation: (acc: B, A) -> B): B? {
  val iterator = this.iterator()
  if (!iterator.hasNext()) return null
  var accumulator: B = initial(iterator.next())
  while (iterator.hasNext()) {
    accumulator = operation(accumulator, iterator.next())
  }
  return accumulator
}

inline fun <A, B> List<A>.reduceRightEvalOrNull(
  initial: (A) -> B,
  operation: (A, acc: Eval<B>) -> Eval<B>
): Eval<B?> {
  val iterator = listIterator(size)
  if (!iterator.hasPrevious()) return Eval.now(null)
  var accumulator: Eval<B> = Eval.now(initial(iterator.previous()))
  while (iterator.hasPrevious()) {
    accumulator = operation(iterator.previous(), accumulator)
  }
  return accumulator
}

inline fun <A, B> List<A>.reduceRightNull(
  initial: (A) -> B,
  operation: (A, acc: B) -> B
): B? {
  val iterator = listIterator(size)
  if (!iterator.hasPrevious()) return null
  var accumulator: B = initial(iterator.previous())
  while (iterator.hasPrevious()) {
    accumulator = operation(iterator.previous(), accumulator)
  }
  return accumulator
}

/**
 * Returns a [List<Pair<A?, B?>>] containing the zipped values of the two lists with null for padding.
 *
 * Example:
 * ```kotlin:ank:playground
 * import arrow.core.*
 *
 * //sampleStart
 * val padRight = listOf(1, 2).padZip(listOf("a"))        // Result: [Pair(1, "a"), Pair(2, null)]
 * val padLeft = listOf(1).padZip(listOf("a", "b"))       // Result: [Pair(1, "a"), Pair(null, "b")]
 * val noPadding = listOf(1, 2).padZip(listOf("a", "b"))  // Result: [Pair(1, "a"), Pair(2, "b")]
 * //sampleEnd
 *
 * fun main() {
 *   println("padRight = $padRight")
 *   println("padLeft = $padLeft")
 *   println("noPadding = $noPadding")
 * }
 * ```
 */
fun <A, B> Iterable<A>.padZip(other: Iterable<B>): List<Pair<A?, B?>> =
  align(other) { ior ->
    ior.fold(
      { it to null },
      { null to it },
      { a, b -> a to b }
    )
  }

/**
 * Returns a [List<C>] containing the result of applying some transformation `(A?, B?) -> C`
 * on a zip.
 *
 * Example:
 * ```kotlin:ank:playground
 * import arrow.core.*
 *
 * //sampleStart
 * val padZipRight = listOf(1, 2).padZip(listOf("a")) { l, r -> l to r }     // Result: [Pair(1, "a"), Pair(2, null)]
 * val padZipLeft = listOf(1).padZip(listOf("a", "b")) { l, r -> l to r }    // Result: [Pair(1, "a"), Pair(null, "b")]
 * val noPadding = listOf(1, 2).padZip(listOf("a", "b")) { l, r -> l to r }  // Result: [Pair(1, "a"), Pair(2, "b")]
 * //sampleEnd
 *
 * fun main() {
 *   println("padZipRight = $padZipRight")
 *   println("padZipLeft = $padZipLeft")
 *   println("noPadding = $noPadding")
 * }
 * ```
 */
inline fun <A, B, C> Iterable<A>.padZip(other: Iterable<B>, fa: (A?, B?) -> C): List<C> =
  padZip(other).map { fa(it.first, it.second) }

/**
 * Returns a [List<C>] containing the result of applying some transformation `(A?, B) -> C`
 * on a zip, excluding all cases where the right value is null.
 *
 * Example:
 * ```kotlin:ank:playground
 * import arrow.core.*
 *
 * //sampleStart
 * val left = listOf(1, 2).leftPadZip(listOf("a")) { l, r -> l to r }      // Result: [Pair(1, "a")]
 * val right = listOf(1).leftPadZip(listOf("a", "b")) { l, r -> l to r }   // Result: [Pair(1, "a"), Pair(null, "b")]
 * val both = listOf(1, 2).leftPadZip(listOf("a", "b")) { l, r -> l to r } // Result: [Pair(1, "a"), Pair(2, "b")]
 * //sampleEnd
 *
 * fun main() {
 *   println("left = $left")
 *   println("right = $right")
 *   println("both = $both")
 * }
 * ```
 */
inline fun <A, B, C> Iterable<A>.leftPadZip(other: Iterable<B>, fab: (A?, B) -> C): List<C> =
  padZip(other) { a: A?, b: B? -> b?.let { fab(a, it) } }.mapNotNull(::identity)

/**
 * Returns a [List<Pair<A?, B>>] containing the zipped values of the two lists
 * with null for padding on the left.
 *
 * Example:
 * ```kotlin:ank:playground
 * import arrow.core.*
 *
 * //sampleStart
 * val padRight = listOf(1, 2).leftPadZip(listOf("a"))        // Result: [Pair(1, "a")]
 * val padLeft = listOf(1).leftPadZip(listOf("a", "b"))       // Result: [Pair(1, "a"), Pair(null, "b")]
 * val noPadding = listOf(1, 2).leftPadZip(listOf("a", "b"))  // Result: [Pair(1, "a"), Pair(2, "b")]
 * //sampleEnd
 *
 * fun main() {
 *   println("left = $left")
 *   println("right = $right")
 *   println("both = $both")
 * }
 * ```
 */
fun <A, B> Iterable<A>.leftPadZip(other: Iterable<B>): List<Pair<A?, B>> =
  this.leftPadZip(other) { a, b -> a to b }

/**
 * Returns a [List<C>] containing the result of applying some transformation `(A, B?) -> C`
 * on a zip, excluding all cases where the left value is null.
 *
 * Example:
 * ```kotlin:ank:playground
 * import arrow.core.*
 *
 * //sampleStart
 * val left = listOf(1, 2).rightPadZip(listOf("a")) { l, r -> l to r }      // Result: [Pair(1, "a"), Pair(null, "b")]
 * val right = listOf(1).rightPadZip(listOf("a", "b")) { l, r -> l to r }   // Result: [Pair(1, "a")]
 * val both = listOf(1, 2).rightPadZip(listOf("a", "b")) { l, r -> l to r } // Result: [Pair(1, "a"), Pair(2, "b")]
 * //sampleEnd
 *
 * fun main() {
 *   println("left = $left")
 *   println("right = $right")
 *   println("both = $both")
 * }
 * ```
 */
inline fun <A, B, C> Iterable<A>.rightPadZip(other: Iterable<B>, fa: (A, B?) -> C): List<C> =
  other.leftPadZip(this) { a, b -> fa(b, a) }

/**
 * Returns a [List<Pair<A, B?>>] containing the zipped values of the two lists
 * with null for padding on the right.
 *
 * Example:
 * ```kotlin:ank:playground
 * import arrow.core.*
 *
 * //sampleStart
 * val padRight = listOf(1, 2).rightPadZip(listOf("a"))        // Result: [Pair(1, "a"), Pair(2, null)]
 * val padLeft = listOf(1).rightPadZip(listOf("a", "b"))       // Result: [Pair(1, "a")]
 * val noPadding = listOf(1, 2).rightPadZip(listOf("a", "b"))  // Result: [Pair(1, "a"), Pair(2, "b")]
 * //sampleEnd
 *
 * fun main() {
 *   println("left = $left")
 *   println("right = $right")
 *   println("both = $both")
 * }
 * ```
 */
fun <A, B> Iterable<A>.rightPadZip(other: Iterable<B>): List<Pair<A, B?>> =
  this.rightPadZip(other) { a, b -> a to b }

@Suppress("UNCHECKED_CAST")
private tailrec fun <A, B> go(
  buf: MutableList<B>,
  f: (A) -> Iterable<Either<A, B>>,
  v: List<Either<A, B>>
) {
  if (v.isNotEmpty()) {
    when (val head: Either<A, B> = v.first()) {
      is Either.Right -> {
        buf += head.b
        go(buf, f, v.drop(1))
      }
      is Either.Left -> go(buf, f, (f(head.a) + v.drop(1)))
    }
  }
}

fun <A, B> tailRecMIterable(a: A, f: (A) -> Iterable<Either<A, B>>): List<B> {
  val buf = mutableListOf<B>()
  go(buf, f, f(a).toList())
  return ListK(buf)
}

/**
 * Combines two structures by taking the union of their shapes and combining the elements with the given function.
 *
 * ```kotlin:ank:playground
 * import arrow.core.*
 *
 * fun main(args: Array<String>) {
 *   //sampleStart
 *   val result =
 *    listOf("A", "B").align(listOf(1, 2, 3)) {
 *      "$it"
 *    }
 *   //sampleEnd
 *   println(result)
 * }
 * ```
 */
inline fun <A, B, C> Iterable<A>.align(b: Iterable<B>, fa: (Ior<A, B>) -> C): List<C> =
  this.align(b).map(fa)

/**
 * Combines two structures by taking the union of their shapes and using Ior to hold the elements.
 *
 * ```kotlin:ank:playground
 * import arrow.core.*
 *
 * fun main(args: Array<String>) {
 *   //sampleStart
 *   val result =
 *     listOf("A", "B").align(listOf(1, 2, 3))
 *   //sampleEnd
 *   println(result)
 * }
 * ```
 */
fun <A, B> Iterable<A>.align(b: Iterable<B>): List<Ior<A, B>> =
  alignRec(this, b)

@Suppress("NAME_SHADOWING")
private fun <X, Y> alignRec(ls: Iterable<X>, rs: Iterable<Y>): List<Ior<X, Y>> {
  val ls = if (ls is List) ls else ls.toList()
  val rs = if (rs is List) rs else rs.toList()
  return when {
    ls.isEmpty() -> rs.map { it.rightIor() }
    rs.isEmpty() -> ls.map { it.leftIor() }
    else -> listOf(Ior.Both(ls.first(), rs.first())) + alignRec(ls.drop(1), rs.drop(1))
  }
}

/**
 * aligns two structures and combine them with the given [Semigroup.combine]
 */
fun <A> Iterable<A>.salign(
  SG: Semigroup<A>,
  other: Iterable<A>
): Iterable<A> = SG.run {
  align(other) {
    it.fold(::identity, ::identity) { a, b ->
      a.combine(b)
    }
  }
}

/**
 * unzips the structure holding the resulting elements in an `Pair`
 *
 * ```kotlin:ank:playground
 * import arrow.core.*
 *
 * fun main(args: Array<String>) {
 *   //sampleStart
 *   val result =
 *      listOf("A" to 1, "B" to 2).k().unzip()
 *   //sampleEnd
 *   println(result)
 * }
 * ```
 */
fun <A, B> Iterable<Pair<A, B>>.unzip(): Pair<List<A>, List<B>> =
  fold(emptyList<A>() to emptyList()) { (l, r), x ->
    l + x.first to r + x.second
  }

/**
 * after applying the given function unzip the resulting structure into its elements.
 *
 * ```kotlin:ank:playground
 * import arrow.core.*
 *
 * fun main(args: Array<String>) {
 *   //sampleStart
 *   val result =
 *    listOf("A:1", "B:2", "C:3").k().unzip { e ->
 *      e.split(":").let {
 *        it.first() to it.last()
 *      }
 *    }
 *   //sampleEnd
 *   println(result)
 * }
 * ```
 */
inline fun <A, B, C> Iterable<C>.unzip(fc: (C) -> Pair<A, B>): Pair<List<A>, List<B>> =
  map(fc).unzip()

/**
 * splits a union into its component parts.
 *
 * ```kotlin:ank:playground
 * import arrow.core.*
 *
 * fun main(args: Array<String>) {
 *   //sampleStart
 *   val result =
 *    listOf(("A" to 1).bothIor(), ("B" to 2).bothIor(), "C".leftIor())
 *      .unalign()
 *   //sampleEnd
 *   println(result)
 * }
 * ```
 */
fun <A, B> Iterable<Ior<A, B>>.unalign(): Pair<List<A>, List<B>> =
  fold(emptyList<A>() to emptyList()) { (l, r), x ->
    x.fold(
      { l + it to r },
      { l to r + it },
      { a, b -> l + a to r + b }
    )
  }

/**
 * after applying the given function, splits the resulting union shaped structure into its components parts
 *
 * ```kotlin:ank:playground
 * import arrow.core.*
 *
 * fun main(args: Array<String>) {
 *   //sampleStart
 *   val result =
 *      listOf(1, 2, 3).unalign {
 *        it.leftIor()
 *      }
 *   //sampleEnd
 *   println(result)
 * }
 * ```
 */
inline fun <A, B, C> Iterable<C>.unalign(fa: (C) -> Ior<A, B>): Pair<List<A>, List<B>> =
  map(fa).unalign()

fun <A> Iterable<A>.combineAll(MA: Monoid<A>): A = MA.run {
  this@combineAll.fold(empty()) { acc, a ->
    acc.combine(a)
  }
}

/**
 * attempt to split the computation, giving access to the first result.
 *
 * ```kotlin:ank:playground
 * import arrow.core.*
 *
 * fun main(args: Array<String>) {
 *   //sampleStart
 *   val result =
 *    listOf("A", "B", "C").split()
 *   //sampleEnd
 *   println(result)
 * }
 */
fun <A> Iterable<A>.split(): Pair<List<A>, A>? =
  firstOrNull()?.let { first ->
    tail() to first
  }

fun <A> Iterable<A>.tail(): List<A> =
  drop(1)

/**
 * interleave both computations in a fair way.
 *
 * ```kotlin:ank:playground
 * import arrow.core.*
 *
 * fun main(args: Array<String>) {
 *   //sampleStart
 *   val tags = List(10) { "#" }
 *   val result =
 *    tags.interleave(listOf("A", "B", "C"))
 *   //sampleEnd
 *   println(result)
 * }
 */
fun <A> Iterable<A>.interleave(other: Iterable<A>): List<A> =
  this.split()?.let { (fa, a) ->
    listOf(a) + other.interleave(fa)
  } ?: other.toList()

/**
 * Fair conjunction. Similarly to interleave
 *
 * ```kotlin:ank:playground
 * import arrow.core.*
 *
 * fun main(args: Array<String>) {
 *   //sampleStart
 *   val result =
 *    listOf(1,2,3).unweave { i -> listOf("$i, ${i + 1}") }
 *   //sampleEnd
 *   println(result)
 * }
 */
fun <A, B> Iterable<A>.unweave(ffa: (A) -> Iterable<B>): List<B> =
  split()?.let { (fa, a) ->
    ffa(a).interleave(fa.unweave(ffa))
  } ?: emptyList()

/**
 * Logical conditional. The equivalent of Prolog's soft-cut.
 * If its first argument succeeds at all, then the results will be
 * fed into the success branch. Otherwise, the failure branch is taken.
 *
 * ```kotlin:ank:playground
 * import arrow.core.*
 *
 * fun main(args: Array<String>) {
 *   //sampleStart
 *   val result =
 *    listOf(1,2,3).ifThen(listOf("empty")) { i ->
 *      listOf("$i, ${i + 1}")
 *    }
 *   //sampleEnd
 *   println(result)
 * }
 */
inline fun <A, B> Iterable<A>.ifThen(fb: Iterable<B>, ffa: (A) -> Iterable<B>): Iterable<B> =
  split()?.let { (fa, a) ->
    ffa(a) + fa.flatMap(ffa)
  } ?: fb.toList()

fun <A, B> Iterable<Either<A, B>>.uniteEither(): List<B> =
  flatMap { either ->
    either.fold({ emptyList() }, { b -> listOf(b) })
  }

fun <A, B> Iterable<Validated<A, B>>.uniteValidated(): List<B> =
  flatMap { validated ->
    validated.fold({ emptyList() }, { b -> listOf(b) })
  }

/**
 * Separate the inner [Either] values into the [Either.Left] and [Either.Right].
 *
 * @receiver Iterable of Validated
 * @return a tuple containing List with [Either.Left] and another List with its [Either.Right] values.
 */
fun <A, B> Iterable<Either<A, B>>.separateEither(): Pair<List<A>, List<B>> {
  val asep = flatMap { gab -> gab.fold({ listOf(it) }, { emptyList() }) }
  val bsep = flatMap { gab -> gab.fold({ emptyList() }, { listOf(it) }) }
  return asep to bsep
}

/**
 * Separate the inner [Validated] values into the [Validated.Invalid] and [Validated.Valid].
 *
 * @receiver Iterable of Validated
 * @return a tuple containing List with [Validated.Invalid] and another List with its [Validated.Valid] values.
 */
fun <A, B> Iterable<Validated<A, B>>.separateValidated(): Pair<List<A>, List<B>> {
  val asep = flatMap { gab -> gab.fold({ listOf(it) }, { emptyList() }) }
  val bsep = flatMap { gab -> gab.fold({ emptyList() }, { listOf(it) }) }
  return asep to bsep
}

fun <A> Iterable<Iterable<A>>.flatten(): List<A> =
  flatMap(::identity)

inline fun <B> Iterable<Boolean>.ifM(ifFalse: () -> Iterable<B>, ifTrue: () -> Iterable<B>): List<B> =
  flatMap { bool ->
    if (bool) ifTrue() else ifFalse()
  }

fun <A, B> Iterable<Either<A, B>>.selectM(f: Iterable<(A) -> B>): List<B> =
  flatMap { it.fold({ a -> f.map { ff -> ff(a) } }, { b -> listOf(b) }) }

/**
 *  Applies [f] to an [A] inside [Iterable] and returns the [List] structure with a tuple of the [A] value and the
 *  computed [B] value as result of applying [f]
 *
 *  ```kotlin:ank:playground
 * import arrow.core.*
 *
 *  fun main(args: Array<String>) {
 *   val result =
 *   //sampleStart
 *   listOf("Hello").fproduct { "$it World" }
 *   //sampleEnd
 *   println(result)
 *  }
 *  ```
 */
inline fun <A, B> Iterable<A>.fproduct(f: (A) -> B): List<Pair<A, B>> =
  map { a -> a to f(a) }

/**
 *  Pairs [B] with [A] returning a List<Pair<B, A>>
 *
 *  ```kotlin:ank:playground
 *  import arrow.core.*
 *
 *  fun main(args: Array<String>) {
 *   val result =
 *   //sampleStart
 *   listOf("Hello", "Hello2").tupleLeft("World")
 *   //sampleEnd
 *   println(result)
 *  }
 *  ```
 */
fun <A, B> Iterable<A>.tupleLeft(b: B): List<Pair<B, A>> =
  map { a -> b to a }

/**
 *  Pairs [A] with [B] returning a List<Pair<A, B>>
 *
 *  ```kotlin:ank:playground
 *  import arrow.core.*
 *
 *  fun main(args: Array<String>) {
 *   val result =
 *   //sampleStart
 *   listOf("Hello").tupleRight("World")
 *   //sampleEnd
 *   println(result)
 *  }
 *  ```
 */
fun <A, B> Iterable<A>.tupleRight(b: B): List<Pair<A, B>> =
  map { a -> a to b }

/**
 *  Given [A] is a sub type of [B], re-type this value from Iterable<A> to Iterable<B>
 *
 *  Kind<F, A> -> Kind<F, B>
 *
 *  ```kotlin:ank:playground
 *  import arrow.core.*
 *
 *  fun main(args: Array<String>) {
 *   //sampleStart
 *   val result: Iterable<CharSequence> =
 *     listOf("Hello World").widen()
 *   //sampleEnd
 *   println(result)
 *  }
 *  ```
 */
fun <B, A : B> Iterable<A>.widen(): Iterable<B> =
  this

fun <B, A : B> List<A>.widen(): List<B> =
  this

fun <A> Iterable<A>.fold(MA: Monoid<A>): A = MA.run {
  this@fold.fold(empty()) { acc, a ->
    acc.combine(a)
  }
}

fun <A, B> Iterable<A>.foldMap(MB: Monoid<B>, f: (A) -> B): B = MB.run {
  this@foldMap.fold(empty()) { acc, a ->
    acc.combine(f(a))
  }
}

fun <A, B> Iterable<A>.crosswalk(f: (A) -> Iterable<B>): List<List<B>> =
  fold(emptyList()) { bs, a ->
    f(a).align(bs) { ior ->
      ior.fold(
        { listOf(it) },
        ::identity,
        { l, r -> listOf(l) + r }
      )
    }
  }

fun <A, K, V> Iterable<A>.crosswalkMap(f: (A) -> Map<K, V>): Map<K, List<V>> =
  fold(emptyMap()) { bs, a ->
    f(a).align(bs) { (_, ior) ->
      ior.fold(
        { listOf(it) },
        ::identity,
        { l, r -> listOf(l) + r }
      )
    }
  }

fun <A, B> Iterable<A>.crosswalkNull(f: (A) -> B?): List<B>? =
  fold<A, List<B>?>(emptyList()) { bs, a ->
    Ior.fromNullables(f(a), bs)?.fold(
      { listOf(it) },
      ::identity,
      { l, r -> listOf(l) + r }
    )
  }

@PublishedApi
internal val unit: List<Unit> =
  listOf(Unit)

fun <A> Iterable<A>.replicate(n: Int): List<List<A>> =
  if (n <= 0) emptyList()
  else toList().let { l -> List(n) { l } }

fun <A> Iterable<A>.replicate(n: Int, MA: Monoid<A>): List<A> =
  if (n <= 0) listOf(MA.empty())
  else this@replicate.zip(replicate(n - 1, MA)) { a, xs -> MA.run { a + xs } }

operator fun <A : Comparable<A>> Iterable<A>.compareTo(other: Iterable<A>): Int =
  align(other) { ior -> ior.fold({ 1 }, { -1 }, { a1, a2 -> a1.compareTo(a2) }) }
    .fold(0) { acc, i ->
      when (acc) {
        0 -> i
        else -> acc
      }
    }
