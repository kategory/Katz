package arrow.optics

import arrow.optics.internal.Pro
import arrow.optics.internal.Profunctor

typealias Optic_<K, I, S, A> = Optic<K, I, S, S, A, A>

/**
 * ** Base interface for optics.
 *
 * > Note: This documents the internals of optics and is rarely ever relevant for users
 *
 * An optic is a profunctor transformation from a profunctor that acts upon a focus to
 *  a profunctor that acts upon a source. `Pro<A, B> -> Pro<S, T>`.
 *
 * > If you are not familiar with profunctors, read the documentation for the profunctor
 *  typeclass (and the rest of its hierarchy) first.
 *
 * Optics:
 *
 *          Source/Target
 *         S --------------> T
 *          \              /
 *   Getter  \            /  Setter
 *  (Zoom in) \          / (Zoom-out)
 *             \        /
 *              A ---> B
 *               Focus
 *
 * As you can see, the focus profunctor `A -> B` is the zoomed in part of the optic,
 *  it operates on the elements we have focused. When running our transformation
 *  we get back a profunctor from source to target which "embeds" our focus transformation
 *  and thus represents our complete runnable optic.
 *
 * Another important observation is that we effectively have two sides:
 * - The contravariant "getter" side of an optic `S -> A`
 * - The covariant "setter" side of an optic `B -> T`
 *
 * This is very important to understand how the actual profunctor instances are used
 *  to achieve their very different uses on the same optic.
 *
 * Creating the final profunctor usually involves passing in a terminal focus operation
 *  such as `f` in `optic.modify(s, f)`. This terminal operation together with
 *  the chosen profunctor instance determines the behavior of the optic.
 *
 * ** Profunctor instances:
 *
 * ```kotlin
 * class Forget<R, A, B>(val f: (A) -> R) {
 *   fun <C, D> dimap(f: (C -> A), g: (B) -> D): Forget<R, C, D> =
 *     Forget { c -> this.f(f(c)) } // B/D are phantom types and thus free to reassign
 * ```
 *
 * Forget is a profunctor which has no covariant side.
 * As such, when used in the profunctor transform, it throws away the setter side
 *  and, when used with `id` as the terminal operation, produces `Forget<A, S, T>`
 *  which we can trivially pass `S` and get our focus `A` out. This perfectly fits the
 *  getter use case. `Forget` can also do folds as long as `R` is a monoid. In this case
 *  `Forget`'s `Traversing` instance is used with `Const` as an applicative, which simply
 *  combines its elements.
 *
 * ```kotlin
 * class Function1<A, B>(val f: (A) -> B)
 * ```
 *
 * Function1 is a trivial profunctor which has no special effects other than composing
 *  functions. It is the profunctor used when the setter side of an optic is needed without
 *  any effects, such as when calling `modify/set`.
 * Its terminal operation is often a transformation function passed from the user or
 *  for `set` a constant function to the supplied element.
 *
 * ```kotlin
 * class Star<F, A, B>(val f: (A) -> Kind<F, B>)
 *   fun <C, D> dimap(AF: Functor<F>, f: (C -> A), g: (B) -> D): Star<F, C, D> =
 *     Star { c -> AF.map(this.f(f(c)), g) }
 *
 * typealias Function1<A, B> == Star<ForIdentity, A, B> // Not used for perf reasons
 * ```
 *
 * Star embeds and threads applicative effects through the optic.
 * This makes it perfect for use when using `traverse-*` methods on the optic, other than
 *  the fact that it exposes effects when running it has no difference to `Function1`
 *
 * ```kotlin
 * class Tagged<A, B>(val b: B) {
 *   fun <C, D> dimap(f: (C) -> A, g: (B) -> D): Tagged<C, D> =
 *    Tagged(g(b))
 * }
 * ```
 *
 * Tagged is very similar to `Id` except that it includes a phantom argument as its
 *  contravariant side. This it is opposite to `Forget` which has a phantom covariant side.
 *
 * Thus Tagged is excellent when only the setter side of an optic is needed, such as with
 *  Prisms and `review`.
 *
 * ** The (phantom) parameters of the optic:
 *
 * `Optic` has a lot of type parameters, for those familiar with lenses `S`, `T`, `A`, `B`
 *  should pose no problem to understand, however `K` and `I` take a bit to get used to,
 *  especially when trying to decipher the compose method.
 *
 * *** Optics kind
 *
 * `K` is used to indicate the capabilities of an optic, all optics are profunctor transformation
 *  but many require more advanced versions of the profunctor typeclass and some also
 *  restrict the use of the final profunctor.
 *
 * For example, lenses require at least `Strong` from the profunctor and `Getter` requires
 *  the use of `Forget` as an instance.
 *
 * To keep track of these restrictions/requirements a phantom parameter `K` is introduced.
 * `K` will always be a phantom type and usually an empty interface.
 * There are three general hierarchies used:
 * - `FoldK`: One of the most general kinds, represents all optics that can be folded
 * - `SetterK`: Another very general kind, represents everything that can be modified.
 * - `ReviewK`: Represents reviews.
 * All other optics kinds extend those three in some way, but also bring with them
 *  unique requirements or abilities.
 *
 * `K` is also marked with `out` for variance to allow composing two different optics
 *  so long as they have a common kind in their hierarchy.
 *  E.g. composing a LensK with FoldK yield FoldK. PrismK and GetterK yield AffineFoldK
 *
 * This encoding leads to almost perfect inference when using compose with monomorphic optics,
 *  it does however struggle when provided with polymorphic combinators for optics.
 *  This can be seen by the boilerplate needed to make those work over any optic kind.
 *  Those are technically not necessary, however they greatly improve the dsl for optics.
 *
 * *** Indices
 *
 * The last type parameter for optics to cover is `I`. `I` represents an index that
 *  an optic can keep track of. This can be integers for list iteration, keys when
 *  traversing all elements of a map or anything else really.
 *
 * For non-indexed terminal operations indices become phantom parameters and thus do
 *  not affect the optic at all.
 * When using indexed terminal operations, indexed variants of the profunctor instances
 *  are used, which keep track of the indices. See `IxForget`, `IxStar`, `IxFunction`.
 *
 * The complicated part, however, isn't really what indices are or how they are used, but how they
 *  compose.
 *
 * As you can see in the transform function, the result of a transform yields `I -> J` for
 *  some existential `J`. This allows the index of the focus profunctor to differ from
 *  the index of the optic, so long as one can map between the two.
 *
 * This leads to some type-tetris and nested functions in the compose functions, but is
 *  really the only good way to embed indices decently well into profunctor optics.
 */
// TODO: Come back to indices docs when I understand them better...
interface Optic<out K, I, in S, out T, A, B> {
  fun <P, J> Profunctor<P>.transform(focus: Pro<P, J, A, B>): Pro<P, (I) -> J, S, T>

  companion object
}

// Markers to use for K
interface FoldK
interface AffineFoldK : FoldK
interface GetterK : AffineFoldK

interface SetterK
interface TraversalK : SetterK, FoldK
interface AffineTraversalK : TraversalK, AffineFoldK
interface PrismK : AffineTraversalK, ReviewK
interface LensK : AffineTraversalK, GetterK
interface ReviewK
interface ReversedLensK : ReviewK
interface ReversedPrismK : GetterK
interface IsoK : LensK, PrismK, ReversedLensK, ReversedPrismK

// Type tetris! Although this isn't that bad ^-^
fun <K1, K2 : K1, I1, I2, S, T, A1 : A2, A2, B1, B2 : B1, C, D> Optic<K1, I1, S, T, A1, B1>.compose(
  other: Optic<K2, I2, A2, B2, C, D>
): Optic<K1, I1, S, T, C, D> = object : Optic<K1, I1, S, T, C, D> {
  override fun <P, J> Profunctor<P>.transform(focus: Pro<P, J, C, D>): Pro<P, (I1) -> J, S, T> {
    val pab = other.run { transform(focus) }.ixMap<J, (I2) -> J, A2, B2> { j -> { j } }
    return this@compose.run { transform(pab) }
  }
}

fun <K1, K2 : K1, I1, I2, I3, S, T, A1 : A2, A2, B1, B2 : B1, C, D> Optic<K1, I1, S, T, A1, B1>.ixCompose(
  other: Optic<K2, I2, A2, B2, C, D>,
  f: (I1, I2) -> I3
): Optic<K1, I3, S, T, C, D> = object : Optic<K1, I3, S, T, C, D> {
  override fun <P, J> Profunctor<P>.transform(focus: Pro<P, J, C, D>): Pro<P, (I3) -> J, S, T> {
    val pab = other.run { transform(focus) }
    return this@ixCompose.run { transform(pab) }
      .ixMap { i3j -> { i1: I1 -> { i2: I2 -> i3j(f(i1, i2)) } } }
  }
}

fun <K1, K2 : K1, I1, I2, S, T, A1 : A2, A2, B1, B2 : B1, C, D> Optic<K1, I1, S, T, A1, B1>.ixCompose(
  other: Optic<K2, I2, A2, B2, C, D>
): Optic<K1, Pair<I1, I2>, S, T, C, D> =
  ixCompose(other) { i1, i2 -> i1 to i2 }

// Included for symmetry, but this is just compose
fun <K1, K2 : K1, I1, I2, S, T, A1 : A2, A2, B1, B2 : B1, C, D> Optic<K1, I1, S, T, A1, B1>.ixComposeLeft(
  other: Optic<K2, I2, A2, B2, C, D>
): Optic<K1, I1, S, T, C, D> =
  compose(other)

fun <K1, K2 : K1, I1, I2, S, T, A1 : A2, A2, B1, B2 : B1, C, D> Optic<K1, I1, S, T, A1, B1>.ixComposeRight(
  other: Optic<K2, I2, A2, B2, C, D>
): Optic<K1, I2, S, T, C, D> =
  ixCompose(other) { _, i2 -> i2 }

fun <K, I, J, S, T, A, B> Optic<K, I, S, T, A, B>.reindexed(
  f: (I) -> J
): Optic<K, J, S, T, A, B> =
  object : Optic<K, J, S, T, A, B> {
    override fun <P, K> Profunctor<P>.transform(focus: Pro<P, K, A, B>): Pro<P, (J) -> K, S, T> =
      this@reindexed.run {
        transform(focus).ixMap { jk -> { i: I -> jk((f(i))) } }
      }
  }
