package arrow.optics.internal

import arrow.core.Either
import arrow.typeclasses.Monoid

internal class ForIxForget

internal inline fun <R, I, A, B> Pro<Kind<ForIxForget, R>, I, A, B>.fix(): IxForget<R, I, A, B> =
  this as IxForget<R, I, A, B>

internal class IxForget<out R, in I, in A, out B>(internal val f: (I, A) -> R) :
  Pro<Kind<ForIxForget, @kotlin.UnsafeVariance R>, I, A, B> {
  companion object {
    fun <R> strong(): IxForgetStrong<R> = object : IxForgetStrong<R> {}
    fun <R> traversing(MR: Monoid<R>) = object : IxForgetTraversing<R> {
      override fun MR(): Monoid<R> = MR
    }
    fun <R> traversingLazy(MR: Monoid<R>) = object : IxForgetTraversingLazy<R> {
      override fun MR(): Monoid<R> = MR
    }
  }
}

internal interface IxForgetProfunctor<R> : Profunctor<Kind<ForIxForget, R>> {
  override fun <I, A, B, C, D> Pro<Kind<ForIxForget, R>, I, B, C>.dimap(
    f: (A) -> B,
    g: (C) -> D
  ): Pro<Kind<ForIxForget, R>, I, A, D> =
    IxForget { i, a -> this.fix().f(i, f(a)) }

  override fun <I, A, B, C> Pro<Kind<ForIxForget, R>, I, B, C>.lMap(f: (A) -> B): Pro<Kind<ForIxForget, R>, I, A, C> =
    IxForget { i, a -> this.fix().f(i, f(a)) }

  override fun <I, B, C, D> Pro<Kind<ForIxForget, R>, I, B, C>.rMap(g: (C) -> D): Pro<Kind<ForIxForget, R>, I, B, D> =
    this as IxForget<R, I, B, D> // Safe because C and D are phantom types

  override fun <I, J, A, B> Pro<Kind<ForIxForget, R>, J, A, B>.ixMap(f: (I) -> J): Pro<Kind<ForIxForget, R>, I, A, B> =
    IxForget { i, a -> this.fix().f(f(i), a) }
}

internal interface IxForgetStrong<R> : Strong<Kind<ForIxForget, R>>, IxForgetProfunctor<R> {
  override fun <I, A, B, C> Pro<Kind<ForIxForget, R>, I, A, B>.first(): Pro<Kind<ForIxForget, R>, I, Pair<A, C>, Pair<B, C>> =
    IxForget { i, (a, _) -> this.fix().f(i, a) }

  override fun <I, A, B, C> Pro<Kind<ForIxForget, R>, I, A, B>.second(): Pro<Kind<ForIxForget, R>, I, Pair<C, A>, Pair<C, B>> =
    IxForget { i, (_, a) -> this.fix().f(i, a) }

  override fun <I, J, S, T, A, B> Pro<Kind<ForIxForget, R>, J, A, B>.ilinear(f: IxLinearF<I, S, T, A, B>): Pro<Kind<ForIxForget, R>, (I) -> J, S, T> =
    IxForget { ij, s ->
      f.invoke(object : ConstFunctor<R> {}, s) { i, a ->
        Const(this.fix().f(ij(i), a))
      }.fix().v
    }
}

internal interface IxForgetChoice<R> : Choice<Kind<ForIxForget, R>>, IxForgetProfunctor<R> {
  fun MR(): Monoid<R>

  override fun <I, A, B, C> Pro<Kind<ForIxForget, R>, I, A, B>.left(): Pro<Kind<ForIxForget, R>, I, Either<A, C>, Either<B, C>> =
    IxForget { i, e -> e.fold({ a -> this.fix().f(i, a) }, { MR().empty() }) }

  override fun <I, A, B, C> Pro<Kind<ForIxForget, R>, I, A, B>.right(): Pro<Kind<ForIxForget, R>, I, Either<C, A>, Either<C, B>> =
    IxForget { i, e -> e.fold({ MR().empty() }, { a -> this.fix().f(i, a) }) }
}

internal interface IxForgetTraversing<R> : Traversing<Kind<ForIxForget, R>>, IxForgetStrong<R>, IxForgetChoice<R> {
  override fun <I, S, T, A, B> Pro<Kind<ForIxForget, R>, I, A, B>.wander(f: WanderF<S, T, A, B>): Pro<Kind<ForIxForget, R>, I, S, T> =
    IxForget { i, s ->
      f.invoke(
        object : ConstApplicative<R> {
          override fun MR(): Monoid<R> = this@IxForgetTraversing.MR()
        }, s, { a -> Const(this.fix().f(i, a)) }
      ).fix().v
    }

  override fun <I, J, S, T, A, B> Pro<Kind<ForIxForget, R>, J, A, B>.iwander(f: IxWanderF<I, S, T, A, B>): Pro<Kind<ForIxForget, R>, (I) -> J, S, T> =
    IxForget { ij, s ->
      f.invoke(
        object : ConstApplicative<R> {
          override fun MR(): Monoid<R> = this@IxForgetTraversing.MR()
        }, s, { i, a -> Const(this.fix().f(ij(i), a)) }
      ).fix().v
    }
}

internal interface IxForgetTraversingLazy<R> : Traversing<Kind<ForIxForget, R>>, IxForgetStrong<R>, IxForgetChoice<R> {
  override fun <I, S, T, A, B> Pro<Kind<ForIxForget, R>, I, A, B>.wander(f: WanderF<S, T, A, B>): Pro<Kind<ForIxForget, R>, I, S, T> =
    IxForget { i, s ->
      f.invokeLazy(
        object : ConstApplicative<R> {
          override fun MR(): Monoid<R> = this@IxForgetTraversingLazy.MR()
        }, s, { a -> Const(this.fix().f(i, a)) }
      ).fix().v
    }

  override fun <I, J, S, T, A, B> Pro<Kind<ForIxForget, R>, J, A, B>.iwander(f: IxWanderF<I, S, T, A, B>): Pro<Kind<ForIxForget, R>, (I) -> J, S, T> =
    IxForget { ij, s ->
      f.invokeLazy(
        object : ConstApplicative<R> {
          override fun MR(): Monoid<R> = this@IxForgetTraversingLazy.MR()
        }, s, { i, a -> Const(this.fix().f(ij(i), a)) }
      ).fix().v
    }
}

internal interface IxForgetCoChoice<R> : CoChoice<Kind<ForIxForget, R>>, IxForgetProfunctor<R> {
  override fun <I, A, B, C> Pro<Kind<ForIxForget, R>, I, Either<A, C>, Either<B, C>>.unLeft(): Pro<Kind<ForIxForget, R>, I, A, B> =
    IxForget { i, a -> fix().f(i, Either.Left(a)) }

  override fun <I, A, B, C> Pro<Kind<ForIxForget, R>, I, Either<C, A>, Either<C, B>>.unRight(): Pro<Kind<ForIxForget, R>, I, A, B> =
    IxForget { i, a -> fix().f(i, Either.Right(a)) }
}
