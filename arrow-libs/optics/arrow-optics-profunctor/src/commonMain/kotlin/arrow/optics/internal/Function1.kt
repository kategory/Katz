package arrow.optics.internal

import arrow.core.Either

internal class ForFunction1

internal inline fun <I, A, B> Pro<ForFunction1, I, A, B>.fix(): Function1<I, A, B> = this as Function1<I, A, B>
internal class Function1<in I, in A, out B>(internal val f: (A) -> B) : Pro<ForFunction1, I, A, B> {
  companion object {
    fun mapping() = object : Function1Mapping {}
  }
}

internal interface Function1Profunctor : Profunctor<ForFunction1> {
  override fun <I, A, B, C, D> Pro<ForFunction1, I, B, C>.dimap(f: (A) -> B, g: (C) -> D): Pro<ForFunction1, I, A, D> =
    Function1 { a: A -> g(this.fix().f(f(a))) }

  override fun <I, A, B, C> Pro<ForFunction1, I, B, C>.lMap(f: (A) -> B): Pro<ForFunction1, I, A, C> =
    Function1 { a: A -> this.fix().f(f(a)) }

  override fun <I, B, C, D> Pro<ForFunction1, I, B, C>.rMap(g: (C) -> D): Pro<ForFunction1, I, B, D> =
    Function1 { b: B -> g(this.fix().f(b)) }

  override fun <I, J, A, B> Pro<ForFunction1, J, A, B>.ixMap(f: (I) -> J): Pro<ForFunction1, I, A, B> =
    this as Function1<I, A, B> // Safe I and J are phantom types
}

internal interface Function1Strong : Strong<ForFunction1>, Function1Profunctor {
  override fun <I, A, B, C> Pro<ForFunction1, I, A, B>.first(): Pro<ForFunction1, I, Pair<A, C>, Pair<B, C>> =
    Function1 { (a, c) -> this.fix().f(a) to c }

  override fun <I, A, B, C> Pro<ForFunction1, I, A, B>.second(): Pro<ForFunction1, I, Pair<C, A>, Pair<C, B>> =
    Function1 { (c, a) -> c to this.fix().f(a) }

  override fun <I, J, S, T, A, B> Pro<ForFunction1, J, A, B>.ilinear(f: IxLinearF<I, S, T, A, B>): Pro<ForFunction1, (I) -> J, S, T> =
    Function1 { s ->
      f.invoke(object : IdApplicative {}, s) { _, a -> Id(this.fix().f(a)) }
        .fix().v
    }
}

internal interface Function1Choice : Choice<ForFunction1>, Function1Profunctor {
  override fun <I, A, B, C> Pro<ForFunction1, I, A, B>.left(): Pro<ForFunction1, I, Either<A, C>, Either<B, C>> =
    Function1 { e -> e.fold({ Either.Left(this.fix().f(it)) }, { Either.Right(it) }) }

  override fun <I, A, B, C> Pro<ForFunction1, I, A, B>.right(): Pro<ForFunction1, I, Either<C, A>, Either<C, B>> =
    Function1 { e -> e.fold({ Either.Left(it) }, { Either.Right(this.fix().f(it)) }) }
}

internal interface Function1Traversing : Traversing<ForFunction1>, Function1Choice, Function1Strong {
  override fun <I, S, T, A, B> Pro<ForFunction1, I, A, B>.wander(f: WanderF<S, T, A, B>): Pro<ForFunction1, I, S, T> =
    Function1 { s ->
      f.invoke(object : IdApplicative {}, s) { a -> Id(this.fix().f(a)) }
        .fix().v
    }

  override fun <I, J, S, T, A, B> Pro<ForFunction1, J, A, B>.iwander(f: IxWanderF<I, S, T, A, B>): Pro<ForFunction1, (I) -> J, S, T> =
    Function1 { s ->
      f.invoke(object : IdApplicative {}, s) { _, a -> Id(this.fix().f(a)) }
        .fix().v
    }
}

internal interface Function1Mapping : Mapping<ForFunction1>, Function1Traversing {
  override fun <I, S, T, A, B> Pro<ForFunction1, I, A, B>.roam(f: (inner: (A) -> B, s: S) -> T): Pro<ForFunction1, I, S, T> =
    Function1 { s -> f(this.fix().f, s) }

  override fun <I, J, S, T, A, B> Pro<ForFunction1, J, A, B>.iroam(f: (inner: (I, A) -> B, s: S) -> T): Pro<ForFunction1, (I) -> J, S, T> =
    Function1 { s -> f({ _, a -> this.fix().f(a) }, s) }
}
