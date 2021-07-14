package arrow.optics.internal

import arrow.core.Either
import arrow.typeclasses.Monoid

internal class ForForget

internal inline fun <R, I, A, B> Pro<Kind<ForForget, R>, I, A, B>.fix(): Forget<R, I, A, B> = this as Forget<R, I, A, B>
internal class Forget<out R, in I, in A, out B>(internal val f: (A) -> R) : Pro<Kind<ForForget, @kotlin.UnsafeVariance R>, I, A, B> {
  companion object {
    fun <R> strong(): ForgetStrong<R> = object : ForgetStrong<R>, ForgetCoChoice<R> {}
    fun <R> traversing(MR: Monoid<R>): ForgetTraversing<R> = object : ForgetTraversing<R> {
      override fun MR(): Monoid<R> = MR
    }
    fun <R> traversingLazy(MR: Monoid<R>): ForgetLazyTraversing<R> = object : ForgetLazyTraversing<R> {
      override fun MR(): Monoid<R> = MR
    }
  }
}

internal interface ForgetProfunctor<R> : Profunctor<Kind<ForForget, R>> {
  override fun <I, A, B, C, D> Pro<Kind<ForForget, R>, I, B, C>.dimap(
    f: (A) -> B,
    g: (C) -> D
  ): Pro<Kind<ForForget, R>, I, A, D> =
    Forget { a -> this.fix().f(f(a)) }

  override fun <I, A, B, C> Pro<Kind<ForForget, R>, I, B, C>.lMap(f: (A) -> B): Pro<Kind<ForForget, R>, I, A, C> =
    Forget { a -> this.fix().f(f(a)) }

  override fun <I, B, C, D> Pro<Kind<ForForget, R>, I, B, C>.rMap(g: (C) -> D): Pro<Kind<ForForget, R>, I, B, D> =
    this as Forget<R, I, B, D> // Safe because C and D are phantom types

  override fun <I, J, A, B> Pro<Kind<ForForget, R>, J, A, B>.ixMap(f: (I) -> J): Pro<Kind<ForForget, R>, I, A, B> =
    this as Forget<R, I, A, B> // Safe I and J are a phantom types
}

internal interface ForgetStrong<R> : Strong<Kind<ForForget, R>>, ForgetProfunctor<R> {
  override fun <I, A, B, C> Pro<Kind<ForForget, R>, I, A, B>.first(): Pro<Kind<ForForget, R>, I, Pair<A, C>, Pair<B, C>> =
    Forget { (a, _) -> this.fix().f(a) }

  override fun <I, A, B, C> Pro<Kind<ForForget, R>, I, A, B>.second(): Pro<Kind<ForForget, R>, I, Pair<C, A>, Pair<C, B>> =
    Forget { (_, a) -> this.fix().f(a) }

  override fun <I, J, S, T, A, B> Pro<Kind<ForForget, R>, J, A, B>.ilinear(f: IxLinearF<I, S, T, A, B>): Pro<Kind<ForForget, R>, (I) -> J, S, T> =
    Forget { s ->
      f.invoke(
        object : ConstFunctor<R> {}, s
      ) { _, a -> Const(this.fix().f(a)) }
        .fix().v
    }
}

internal interface ForgetChoice<R> : Choice<Kind<ForForget, R>>, ForgetProfunctor<R> {
  fun MR(): Monoid<R>

  override fun <I, A, B, C> Pro<Kind<ForForget, R>, I, A, B>.left(): Pro<Kind<ForForget, R>, I, Either<A, C>, Either<B, C>> =
    Forget { e -> e.fold({ a -> this.fix().f(a) }, { MR().empty() }) }

  override fun <I, A, B, C> Pro<Kind<ForForget, R>, I, A, B>.right(): Pro<Kind<ForForget, R>, I, Either<C, A>, Either<C, B>> =
    Forget { e -> e.fold({ MR().empty() }, { a -> this.fix().f(a) }) }
}

internal interface ForgetTraversing<R> : Traversing<Kind<ForForget, R>>, ForgetStrong<R>, ForgetChoice<R> {
  override fun <I, S, T, A, B> Pro<Kind<ForForget, R>, I, A, B>.wander(f: WanderF<S, T, A, B>): Pro<Kind<ForForget, R>, I, S, T> =
    Forget { s ->
      f.invoke(
        object : ConstApplicative<R> {
          override fun MR(): Monoid<R> = this@ForgetTraversing.MR()
        }, s, { a -> Const(this.fix().f(a)) }
      ).fix().v
    }

  override fun <I, J, S, T, A, B> Pro<Kind<ForForget, R>, J, A, B>.iwander(f: IxWanderF<I, S, T, A, B>): Pro<Kind<ForForget, R>, (I) -> J, S, T> =
    Forget { s ->
      f.invoke(
        object : ConstApplicative<R> {
          override fun MR(): Monoid<R> = this@ForgetTraversing.MR()
        }, s, { _, a -> Const(this.fix().f(a)) }
      ).fix().v
    }
}

internal interface ForgetLazyTraversing<R> : Traversing<Kind<ForForget, R>>, ForgetStrong<R>, ForgetChoice<R> {
  override fun <I, S, T, A, B> Pro<Kind<ForForget, R>, I, A, B>.wander(f: WanderF<S, T, A, B>): Pro<Kind<ForForget, R>, I, S, T> =
    Forget { s ->
      f.invokeLazy(
        object : ConstApplicative<R> {
          override fun MR(): Monoid<R> = this@ForgetLazyTraversing.MR()
        }, s, { a -> Const(this.fix().f(a)) }
      ).fix().v
    }

  override fun <I, J, S, T, A, B> Pro<Kind<ForForget, R>, J, A, B>.iwander(f: IxWanderF<I, S, T, A, B>): Pro<Kind<ForForget, R>, (I) -> J, S, T> =
    Forget { s ->
      f.invokeLazy(
        object : ConstApplicative<R> {
          override fun MR(): Monoid<R> = this@ForgetLazyTraversing.MR()
        }, s, { _, a -> Const(this.fix().f(a)) }
      ).fix().v
    }
}

internal interface ForgetCoChoice<R> : CoChoice<Kind<ForForget, R>>, ForgetProfunctor<R> {
  override fun <I, A, B, C> Pro<Kind<ForForget, R>, I, Either<A, C>, Either<B, C>>.unLeft(): Pro<Kind<ForForget, R>, I, A, B> =
    Forget { a -> fix().f(Either.Left(a)) }

  override fun <I, A, B, C> Pro<Kind<ForForget, R>, I, Either<C, A>, Either<C, B>>.unRight(): Pro<Kind<ForForget, R>, I, A, B> =
    Forget { a -> fix().f(Either.Right(a)) }
}
