package arrow.optics.internal

import arrow.core.Either

internal class ForStar

internal inline fun <F, I, A, B> Pro<Kind<ForStar, F>, I, A, B>.fix(): Star<F, I, A, B> = this as Star<F, I, A, B>
internal class Star<F, I, A, B>(internal val f: (A) -> Kind<F, B>) : Pro<Kind<ForStar, F>, I, A, B> {
  companion object {
    fun <F> traversing(AF: Applicative<F>) = object : StarTraversing<F> {
      override fun AF(): Applicative<F> = AF
    }
    fun <F> traversingLazy(AF: Applicative<F>) = object : StarTraversingLazy<F> {
      override fun AF(): Applicative<F> = AF
    }
  }
}

internal interface StarProfunctor<F> : Profunctor<Kind<ForStar, F>> {
  fun AF(): Applicative<F>
  override fun <I, A, B, C, D> Pro<Kind<ForStar, F>, I, B, C>.dimap(f: (A) -> B, g: (C) -> D): Pro<Kind<ForStar, F>, I, A, D> =
    Star { a -> AF().map(this.fix().f(f(a)), g) }

  override fun <I, A, B, C> Pro<Kind<ForStar, F>, I, B, C>.lMap(f: (A) -> B): Pro<Kind<ForStar, F>, I, A, C> =
    Star { a -> this.fix().f(f(a)) }

  override fun <I, B, C, D> Pro<Kind<ForStar, F>, I, B, C>.rMap(g: (C) -> D): Pro<Kind<ForStar, F>, I, B, D> =
    Star { b -> AF().map(this.fix().f(b), g) }

  override fun <I, J, A, B> Pro<Kind<ForStar, F>, J, A, B>.ixMap(f: (I) -> J): Pro<Kind<ForStar, F>, I, A, B> =
    this as Star<F, I, A, B> // Safe because I and J are phantom types
}

internal interface StarStrong<F> : Strong<Kind<ForStar, F>>, StarProfunctor<F> {
  override fun <I, A, B, C> Pro<Kind<ForStar, F>, I, A, B>.first(): Pro<Kind<ForStar, F>, I, Pair<A, C>, Pair<B, C>> =
    Star { (a, c) -> AF().map(this.fix().f(a)) { b -> b to c } }

  override fun <I, A, B, C> Pro<Kind<ForStar, F>, I, A, B>.second(): Pro<Kind<ForStar, F>, I, Pair<C, A>, Pair<C, B>> =
    Star { (c, a) -> AF().map(this.fix().f(a)) { b -> c to b } }

  override fun <I, J, S, T, A, B> Pro<Kind<ForStar, F>, J, A, B>.ilinear(f: IxLinearF<I, S, T, A, B>): Pro<Kind<ForStar, F>, (I) -> J, S, T> =
    Star { s ->
      f.invoke(AF(), s) { _, a -> this.fix().f(a) }
    }
}

internal interface StarChoice<F> : Choice<Kind<ForStar, F>>, StarProfunctor<F> {
  override fun <I, A, B, C> Pro<Kind<ForStar, F>, I, A, B>.left(): Pro<Kind<ForStar, F>, I, Either<A, C>, Either<B, C>> =
    Star { e ->
      e.fold({ a ->
        AF().map(this.fix().f(a)) { b -> Either.Left(b) }
      }, { c ->
        AF().pure(Either.Right(c))
      })
    }

  override fun <I, A, B, C> Pro<Kind<ForStar, F>, I, A, B>.right(): Pro<Kind<ForStar, F>, I, Either<C, A>, Either<C, B>> =
    Star { e ->
      e.fold({ c ->
        AF().pure(Either.Left(c))
      }, { a ->
        AF().map(this.fix().f(a)) { b -> Either.Right(b) }
      })
    }
}

internal interface StarTraversing<F> : Traversing<Kind<ForStar, F>>, StarChoice<F>, StarStrong<F> {
  override fun <I, S, T, A, B> Pro<Kind<ForStar, F>, I, A, B>.wander(f: WanderF<S, T, A, B>): Pro<Kind<ForStar, F>, I, S, T> =
    Star { s -> f.invoke(AF(), s, this.fix().f) }

  override fun <I, J, S, T, A, B> Pro<Kind<ForStar, F>, J, A, B>.iwander(f: IxWanderF<I, S, T, A, B>): Pro<Kind<ForStar, F>, (I) -> J, S, T> =
    Star { s -> f.invoke(AF(), s) { _, a -> this.fix().f(a) } }
}

internal interface StarTraversingLazy<F> : Traversing<Kind<ForStar, F>>, StarChoice<F>, StarStrong<F> {
  override fun <I, S, T, A, B> Pro<Kind<ForStar, F>, I, A, B>.wander(f: WanderF<S, T, A, B>): Pro<Kind<ForStar, F>, I, S, T> =
    Star { s -> f.invokeLazy(AF(), s, this.fix().f) }

  override fun <I, J, S, T, A, B> Pro<Kind<ForStar, F>, J, A, B>.iwander(f: IxWanderF<I, S, T, A, B>): Pro<Kind<ForStar, F>, (I) -> J, S, T> =
    Star { s -> f.invokeLazy(AF(), s) { _, a -> this.fix().f(a) } }
}
