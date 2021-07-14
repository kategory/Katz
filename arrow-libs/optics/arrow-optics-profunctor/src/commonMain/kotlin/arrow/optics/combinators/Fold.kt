package arrow.optics.combinators

import arrow.core.Either
import arrow.optics.FoldK
import arrow.optics.IxAffineFold
import arrow.optics.IxFold
import arrow.optics.IxFoldF
import arrow.optics.IxGetter
import arrow.optics.Optic
import arrow.optics.collectOf
import arrow.optics.internal.Applicative
import arrow.optics.internal.Kind
import arrow.optics.ixAFolding
import arrow.optics.ixCollectOf
import arrow.optics.ixFirstOrNull
import arrow.optics.ixFolding
import arrow.optics.ixGet
import arrow.optics.ixTraverseLazyOf_
import arrow.optics.ixTraverseOf_
import kotlin.jvm.JvmName

fun <K : FoldK, I, S, T, A, B> Optic<K, I, S, T, A, B>.take(n: Int): IxFold<I, S, A> =
  Optic.ixFolding(object : IxFoldF<I, S, A> {
    override fun <F> invoke(AF: Applicative<F>, s: S, f: (I, A) -> Kind<F, Unit>): Kind<F, Unit> {
      var counter = 0
      return s.ixTraverseOf_(this@take, AF) { i, a ->
        if (counter++ < n) f(i, a)
        else AF.pure(Unit)
      }
    }
    override fun <F> invokeLazy(AF: Applicative<F>, s: S, f: (I, A) -> Kind<F, Unit>): Kind<F, Unit> {
      var counter = 0
      return s.ixTraverseLazyOf_(this@take, AF) { i, a ->
        if (counter++ < n) f(i, a)
        else AF.pure(Unit)
      }
    }
  })

fun <K : FoldK, I, S, T, A, B> Optic<K, I, S, T, A, B>.drop(n: Int): IxFold<I, S, A> =
  Optic.ixFolding(object : IxFoldF<I, S, A> {
    override fun <F> invoke(AF: Applicative<F>, s: S, f: (I, A) -> Kind<F, Unit>): Kind<F, Unit> {
      var counter = 0
      return s.ixTraverseOf_(this@drop, AF) { i, a ->
        if (counter++ < n) AF.pure(Unit)
        else f(i, a)
      }
    }
    override fun <F> invokeLazy(AF: Applicative<F>, s: S, f: (I, A) -> Kind<F, Unit>): Kind<F, Unit> {
      var counter = 0
      return s.ixTraverseLazyOf_(this@drop, AF) { i, a ->
        if (counter++ < n) AF.pure(Unit)
        else f(i, a)
      }
    }
  })

fun <K : FoldK, I, S, T, A> Optic<K, I, S, T, A, A>.takeWhile(filter: (A) -> Boolean): IxFold<I, S, A> =
  Optic.ixFolding(object : IxFoldF<I, S, A> {
    override fun <F> invoke(AF: Applicative<F>, s: S, f: (I, A) -> Kind<F, Unit>): Kind<F, Unit> {
      var tripped = false
      return s.ixTraverseOf_(this@takeWhile, AF) { i, a ->
        if (!tripped && filter(a)) f(i, a)
        else AF.pure(Unit).also { tripped = true }
      }
    }
    override fun <F> invokeLazy(AF: Applicative<F>, s: S, f: (I, A) -> Kind<F, Unit>): Kind<F, Unit> {
      var tripped = false
      return s.ixTraverseLazyOf_(this@takeWhile, AF) { i, a ->
        if (!tripped && filter(a)) f(i, a)
        else AF.pure(Unit).also { tripped = true }
      }
    }
  })

fun <K : FoldK, I, S, T, A> Optic<K, I, S, T, A, A>.dropWhile(filter: (A) -> Boolean): IxFold<I, S, A> =
  Optic.ixFolding(object : IxFoldF<I, S, A> {
    override fun <F> invoke(AF: Applicative<F>, s: S, f: (I, A) -> Kind<F, Unit>): Kind<F, Unit> {
      var tripped = false
      return s.ixTraverseOf_(this@dropWhile, AF) { i, a ->
        if (!tripped && filter(a)) AF.pure(Unit)
        else f(i, a).also { tripped = true }
      }
    }
    override fun <F> invokeLazy(AF: Applicative<F>, s: S, f: (I, A) -> Kind<F, Unit>): Kind<F, Unit> {
      var tripped = false
      return s.ixTraverseLazyOf_(this@dropWhile, AF) { i, a ->
        if (!tripped && filter(a)) AF.pure(Unit)
        else f(i, a).also { tripped = true }
      }
    }
  })

fun <K : FoldK, I, S, T, A> Optic<K, I, S, T, A, A>.ixTakeWhile(filter: (I, A) -> Boolean): IxFold<I, S, A> =
  Optic.ixFolding(object : IxFoldF<I, S, A> {
    override fun <F> invoke(AF: Applicative<F>, s: S, f: (I, A) -> Kind<F, Unit>): Kind<F, Unit> {
      var tripped = false
      return s.ixTraverseOf_(this@ixTakeWhile, AF) { i, a ->
        if (!tripped && filter(i, a)) f(i, a)
        else AF.pure(Unit).also { tripped = true }
      }
    }
    override fun <F> invokeLazy(AF: Applicative<F>, s: S, f: (I, A) -> Kind<F, Unit>): Kind<F, Unit> {
      var tripped = false
      return s.ixTraverseLazyOf_(this@ixTakeWhile, AF) { i, a ->
        if (!tripped && filter(i, a)) f(i, a)
        else AF.pure(Unit).also { tripped = true }
      }
    }
  })

fun <K : FoldK, I, S, T, A> Optic<K, I, S, T, A, A>.ixDropWhile(filter: (I, A) -> Boolean): IxFold<I, S, A> =
  Optic.ixFolding(object : IxFoldF<I, S, A> {
    override fun <F> invoke(AF: Applicative<F>, s: S, f: (I, A) -> Kind<F, Unit>): Kind<F, Unit> {
      var tripped = false
      return s.ixTraverseOf_(this@ixDropWhile, AF) { i, a ->
        if (!tripped && filter(i, a)) AF.pure(Unit)
        else f(i, a).also { tripped = true }
      }
    }
    override fun <F> invokeLazy(AF: Applicative<F>, s: S, f: (I, A) -> Kind<F, Unit>): Kind<F, Unit> {
      var tripped = false
      return s.ixTraverseLazyOf_(this@ixDropWhile, AF) { i, a ->
        if (!tripped && filter(i, a)) AF.pure(Unit)
        else f(i, a).also { tripped = true }
      }
    }
  })

// As with the traversal backwards, this is hacked together
// TODO If we end up using a lazy applicative, we can just use Applicative.backwards instead!
fun <K : FoldK, I, S, T, A, B> Optic<K, I, S, T, A, B>.backwards(): IxFold<I, S, A> =
  Optic.ixFolding(object : IxFoldF<I, S, A> {
    override fun <F> invoke(AF: Applicative<F>, s: S, f: (I, A) -> Kind<F, Unit>): Kind<F, Unit> =
      s.ixCollectOf(this@backwards).asReversed().fold(AF.pure(Unit)) { acc, (i, a) ->
        AF.ap(
          AF.map(acc) { {} },
          f(i, a)
        )
      }
  })

fun <K : FoldK, I, S, A> Optic<K, I, S, S, A, A>.singular(): IxAffineFold<I, S, A> =
  Optic.ixAFolding { s -> s.ixFirstOrNull(this@singular) }

// Unsafe because it throws if the fold is empty
fun <K : FoldK, I, S, T, A, B> Optic<K, I, S, T, A, B>.unsafeSingular(): IxGetter<I, S, A> =
  Optic.ixGet { s -> s.ixFirstOrNull(this@unsafeSingular) ?: throw IllegalStateException("unsafeSingular: No element") }

@JvmName("failing_fold")
fun <K1 : FoldK, K2 : FoldK, I, J, S, T, A, B> Optic<K1, I, S, T, A, B>.failing(
  other: Optic<K2, J, S, T, A, B>
): Optic<FoldK, Either<I, J>, S, Nothing, A, Nothing> =
  Optic.ixFolding(object : IxFoldF<Either<I, J>, S, A> {
    override fun <F> invoke(AF: Applicative<F>, s: S, f: (Either<I, J>, A) -> Kind<F, Unit>): Kind<F, Unit> {
      var tripped = false
      val res = s.ixTraverseOf_(this@failing, AF) { i, a -> f(Either.Left(i), a).also { tripped = true } }

      return if (tripped) res
      else s.ixTraverseOf_(other, AF) { j, a -> f(Either.Right(j), a) }
    }

    override fun <F> invokeLazy(AF: Applicative<F>, s: S, f: (Either<I, J>, A) -> Kind<F, Unit>): Kind<F, Unit> {
      var tripped = false
      val res = s.ixTraverseLazyOf_(this@failing, AF) { i, a -> f(Either.Left(i), a).also { tripped = true } }

      return if (tripped) res
      else s.ixTraverseLazyOf_(other, AF) { j, a -> f(Either.Right(j), a) }
    }
  })

@JvmName("deepOf_fold")
fun <K1 : FoldK, K2 : FoldK, I, S, T, A, B> Optic<K1, Any?, S, T, S, T>.deepOf(
  next: Optic<K2, I, S, T, A, B>
): IxFold<I, S, A> =
  Optic.ixFolding(object : IxFoldF<I, S, A> {
    override fun <F> invoke(AF: Applicative<F>, s: S, f: (I, A) -> Kind<F, Unit>): Kind<F, Unit> {
      val nextS = mutableListOf(s)
      var ft: Kind<F, Unit>? = null
      while (nextS.isNotEmpty()) {
        var tripped = false
        val head = nextS.removeFirst()
        val newFt = head.ixTraverseOf_(next, AF) { i, a -> f(i, a).also { tripped = true } }
        ft = ft?.let {
          AF.ap(AF.map(it) { { } }, newFt)
        } ?: newFt

        if (!tripped) nextS.addAll(head.collectOf(this@deepOf))
      }
      return ft!!
    }
    override fun <F> invokeLazy(AF: Applicative<F>, s: S, f: (I, A) -> Kind<F, Unit>): Kind<F, Unit> {
      val nextS = mutableListOf(s)
      var ft: Kind<F, Unit>? = null
      while (nextS.isNotEmpty()) {
        var tripped = false
        val head = nextS.removeFirst()
        val newFt = head.ixTraverseLazyOf_(next, AF) { i, a -> f(i, a).also { tripped = true } }
        ft = ft?.let {
          AF.ap(AF.map(it) { { } }, newFt)
        } ?: newFt

        if (!tripped) nextS.addAll(head.collectOf(this@deepOf))
      }
      return ft!!
    }
  })
