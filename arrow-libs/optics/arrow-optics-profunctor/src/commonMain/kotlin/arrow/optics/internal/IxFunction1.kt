package arrow.optics.internal

import arrow.core.Either

internal class ForIxFunction1

internal inline fun <I, A, B> Pro<ForIxFunction1, I, A, B>.fix(): IxFunction1<I, A, B> =
  this as IxFunction1<I, A, B>
internal class IxFunction1<I, A, B>(val f: (I, A) -> B) : Pro<ForIxFunction1, I, A, B> {
  companion object {
    fun mapping() = object : IxFunction1Mapping {}
  }
}

internal interface IxFunction1Profunctor : Profunctor<ForIxFunction1> {
  override fun <I, A, B, C, D> Pro<ForIxFunction1, I, B, C>.dimap(
    f: (A) -> B,
    g: (C) -> D
  ): Pro<ForIxFunction1, I, A, D> =
    IxFunction1 { i, a -> g(this.fix().f(i, f(a))) }

  override fun <I, A, B, C> Pro<ForIxFunction1, I, B, C>.lMap(f: (A) -> B): Pro<ForIxFunction1, I, A, C> =
    IxFunction1 { i, a -> this.fix().f(i, f(a)) }

  override fun <I, B, C, D> Pro<ForIxFunction1, I, B, C>.rMap(g: (C) -> D): Pro<ForIxFunction1, I, B, D> =
    IxFunction1 { i, a -> g(this.fix().f(i, a)) }

  override fun <I, J, A, B> Pro<ForIxFunction1, J, A, B>.ixMap(f: (I) -> J): Pro<ForIxFunction1, I, A, B> =
    IxFunction1 { i, a -> this.fix().f(f(i), a) }
}

internal interface IxFunction1Strong : Strong<ForIxFunction1>, IxFunction1Profunctor {
  override fun <I, A, B, C> Pro<ForIxFunction1, I, A, B>.first(): Pro<ForIxFunction1, I, Pair<A, C>, Pair<B, C>> =
    IxFunction1 { i, (a, c) -> this.fix().f(i, a) to c }

  override fun <I, A, B, C> Pro<ForIxFunction1, I, A, B>.second(): Pro<ForIxFunction1, I, Pair<C, A>, Pair<C, B>> =
    IxFunction1 { i, (c, a) -> c to this.fix().f(i, a) }

  override fun <I, J, S, T, A, B> Pro<ForIxFunction1, J, A, B>.ilinear(f: IxLinearF<I, S, T, A, B>): Pro<ForIxFunction1, (I) -> J, S, T> =
    IxFunction1 { ij, s ->
      f.invoke(object : IdApplicative {}, s) { i, a ->
        Id(this.fix().f(ij(i), a))
      }.fix().v
    }
}

internal interface IxFunction1Choice : Choice<ForIxFunction1>, IxFunction1Profunctor {
  override fun <I, A, B, C> Pro<ForIxFunction1, I, A, B>.left(): Pro<ForIxFunction1, I, Either<A, C>, Either<B, C>> =
    IxFunction1 { i, e ->
      e.fold({ a ->
        Either.Left(this.fix().f(i, a))
      }, { c -> Either.Right(c) })
    }

  override fun <I, A, B, C> Pro<ForIxFunction1, I, A, B>.right(): Pro<ForIxFunction1, I, Either<C, A>, Either<C, B>> =
    IxFunction1 { i, e ->
      e.fold({ c -> Either.Left(c) }, { a ->
        Either.Right(this.fix().f(i, a))
      })
    }
}

internal interface IxFunction1Traversing : Traversing<ForIxFunction1>, IxFunction1Choice, IxFunction1Strong {
  override fun <I, S, T, A, B> Pro<ForIxFunction1, I, A, B>.wander(f: WanderF<S, T, A, B>): Pro<ForIxFunction1, I, S, T> =
    IxFunction1 { i, s ->
      f.invoke(
        object : IdApplicative {},
        s
      ) { a -> Id(this.fix().f(i, a)) }
        .fix().v
    }

  override fun <I, J, S, T, A, B> Pro<ForIxFunction1, J, A, B>.iwander(f: IxWanderF<I, S, T, A, B>): Pro<ForIxFunction1, (I) -> J, S, T> =
    IxFunction1 { ij, s ->
      f.invoke(object : IdApplicative {}, s) { i, a -> Id(this.fix().f(ij(i), a)) }
        .fix().v
    }
}

internal interface IxFunction1Mapping : Mapping<ForIxFunction1>, IxFunction1Traversing {
  override fun <I, S, T, A, B> Pro<ForIxFunction1, I, A, B>.roam(f: (inner: (A) -> B, s: S) -> T): Pro<ForIxFunction1, I, S, T> =
    IxFunction1 { i, s -> f({ a -> this.fix().f(i, a) }, s) }

  override fun <I, J, S, T, A, B> Pro<ForIxFunction1, J, A, B>.iroam(f: (inner: (I, A) -> B, s: S) -> T): Pro<ForIxFunction1, (I) -> J, S, T> =
    IxFunction1 { ij, s -> f({ i, a -> this.fix().f(ij(i), a) }, s) }
}
