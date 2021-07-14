package arrow.optics.internal

import arrow.core.Either

internal class ForIxStar

internal inline fun <F, I, A, B> Pro<Kind<ForIxStar, F>, I, A, B>.fix(): IxStar<F, I, A, B> = this as IxStar<F, I, A, B>
internal class IxStar<F, I, A, B>(internal val f: (I, A) -> Kind<F, B>) : Pro<Kind<ForIxStar, F>, I, A, B> {
  companion object {
    fun <F> traversing(AF: Applicative<F>) = object : IxStarTraversing<F> {
      override fun AF(): Applicative<F> = AF
    }
    fun <F> traversingLazy(AF: Applicative<F>) = object : IxStarTraversingLazy<F> {
      override fun AF(): Applicative<F> = AF
    }
  }
}

internal interface IxStarProfunctor<F> : Profunctor<Kind<ForIxStar, F>> {
  fun AF(): Applicative<F>
  override fun <I, A, B, C, D> Pro<Kind<ForIxStar, F>, I, B, C>.dimap(f: (A) -> B, g: (C) -> D): Pro<Kind<ForIxStar, F>, I, A, D> =
    IxStar { i, a -> AF().map(this.fix().f(i, f(a)), g) }

  override fun <I, A, B, C> Pro<Kind<ForIxStar, F>, I, B, C>.lMap(f: (A) -> B): Pro<Kind<ForIxStar, F>, I, A, C> =
    IxStar { i, a -> this.fix().f(i, f(a)) }

  override fun <I, B, C, D> Pro<Kind<ForIxStar, F>, I, B, C>.rMap(g: (C) -> D): Pro<Kind<ForIxStar, F>, I, B, D> =
    IxStar { i, b -> AF().map(this.fix().f(i, b), g) }

  override fun <I, J, A, B> Pro<Kind<ForIxStar, F>, J, A, B>.ixMap(f: (I) -> J): Pro<Kind<ForIxStar, F>, I, A, B> =
    IxStar { i, a -> this.fix().f(f(i), a) }
}

internal interface IxStarStrong<F> : Strong<Kind<ForIxStar, F>>, IxStarProfunctor<F> {
  override fun <I, A, B, C> Pro<Kind<ForIxStar, F>, I, A, B>.first(): Pro<Kind<ForIxStar, F>, I, Pair<A, C>, Pair<B, C>> =
    IxStar { i, (a, c) -> AF().map(this.fix().f(i, a)) { b -> b to c } }

  override fun <I, A, B, C> Pro<Kind<ForIxStar, F>, I, A, B>.second(): Pro<Kind<ForIxStar, F>, I, Pair<C, A>, Pair<C, B>> =
    IxStar { i, (c, a) -> AF().map(this.fix().f(i, a)) { b -> c to b } }

  override fun <I, J, S, T, A, B> Pro<Kind<ForIxStar, F>, J, A, B>.ilinear(f: IxLinearF<I, S, T, A, B>): Pro<Kind<ForIxStar, F>, (I) -> J, S, T> =
    IxStar { ij, s ->
      f.invoke(AF(), s) { i, a ->
        this.fix().f(ij(i), a)
      }
    }
}

internal interface IxStarChoice<F> : Choice<Kind<ForIxStar, F>>, IxStarProfunctor<F> {
  override fun <I, A, B, C> Pro<Kind<ForIxStar, F>, I, A, B>.left(): Pro<Kind<ForIxStar, F>, I, Either<A, C>, Either<B, C>> =
    IxStar { i, e ->
      e.fold({ a ->
        AF().map(this.fix().f(i, a)) { b -> Either.Left(b) }
      }, { c ->
        AF().pure(Either.Right(c))
      })
    }

  override fun <I, A, B, C> Pro<Kind<ForIxStar, F>, I, A, B>.right(): Pro<Kind<ForIxStar, F>, I, Either<C, A>, Either<C, B>> =
    IxStar { i, e ->
      e.fold({ c ->
        AF().pure(Either.Left(c))
      }, { a ->
        AF().map(this.fix().f(i, a)) { b -> Either.Right(b) }
      })
    }
}

internal interface IxStarTraversing<F> : Traversing<Kind<ForIxStar, F>>, IxStarChoice<F>, IxStarStrong<F> {
  override fun <I, S, T, A, B> Pro<Kind<ForIxStar, F>, I, A, B>.wander(f: WanderF<S, T, A, B>): Pro<Kind<ForIxStar, F>, I, S, T> =
    IxStar { i, s -> f.invoke(AF(), s) { a -> this.fix().f(i, a) } }

  override fun <I, J, S, T, A, B> Pro<Kind<ForIxStar, F>, J, A, B>.iwander(f: IxWanderF<I, S, T, A, B>): Pro<Kind<ForIxStar, F>, (I) -> J, S, T> =
    IxStar { ij, s ->
      f.invoke(AF(), s) { i, a -> this.fix().f(ij(i), a) }
    }
}

internal interface IxStarTraversingLazy<F> : Traversing<Kind<ForIxStar, F>>, IxStarChoice<F>, IxStarStrong<F> {
  override fun <I, S, T, A, B> Pro<Kind<ForIxStar, F>, I, A, B>.wander(f: WanderF<S, T, A, B>): Pro<Kind<ForIxStar, F>, I, S, T> =
    IxStar { i, s -> f.invokeLazy(AF(), s) { a -> this.fix().f(i, a) } }

  override fun <I, J, S, T, A, B> Pro<Kind<ForIxStar, F>, J, A, B>.iwander(f: IxWanderF<I, S, T, A, B>): Pro<Kind<ForIxStar, F>, (I) -> J, S, T> =
    IxStar { ij, s ->
      f.invokeLazy(AF(), s) { i, a -> this.fix().f(ij(i), a) }
    }
}
