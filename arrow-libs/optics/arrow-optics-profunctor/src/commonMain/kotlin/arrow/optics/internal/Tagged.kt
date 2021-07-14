package arrow.optics.internal

import arrow.core.Either

internal class ForTagged
internal inline fun <I, A, B> Pro<ForTagged, I, A, B>.fix(): Tagged<I, A, B> = this as Tagged<I, A, B>
internal class Tagged<in I, in A, out B>(val b: B) : Pro<ForTagged, I, A, B> {
  companion object {
    fun choice(): TaggedChoice = object : TaggedChoice, TaggedCoStrong {}
  }
}

internal interface TaggedProfunctor : Profunctor<ForTagged> {
  override fun <I, A, B, C, D> Pro<ForTagged, I, B, C>.dimap(f: (A) -> B, g: (C) -> D): Pro<ForTagged, I, A, D> =
    Tagged(g(fix().b))

  override fun <I, A, B, C> Pro<ForTagged, I, B, C>.lMap(f: (A) -> B): Pro<ForTagged, I, A, C> =
    this as Pro<ForTagged, I, A, C>

  override fun <I, B, C, D> Pro<ForTagged, I, B, C>.rMap(g: (C) -> D): Pro<ForTagged, I, B, D> =
    Tagged(g(fix().b))

  override fun <I, J, A, B> Pro<ForTagged, J, A, B>.ixMap(f: (I) -> J): Pro<ForTagged, I, A, B> =
    this as Pro<ForTagged, I, A, B>
}

internal interface TaggedChoice : Choice<ForTagged>, TaggedProfunctor {
  override fun <I, A, B, C> Pro<ForTagged, I, A, B>.left(): Pro<ForTagged, I, Either<A, C>, Either<B, C>> =
    Tagged(Either.Left(fix().b))

  override fun <I, A, B, C> Pro<ForTagged, I, A, B>.right(): Pro<ForTagged, I, Either<C, A>, Either<C, B>> =
    Tagged(Either.Right(fix().b))
}

internal interface TaggedCoStrong : CoStrong<ForTagged>, TaggedProfunctor {
  override fun <I, A, B, C> Pro<ForTagged, I, Pair<A, C>, Pair<B, C>>.unFirst(): Pro<ForTagged, I, A, B> =
    Tagged(fix().b.first)

  override fun <I, A, B, C> Pro<ForTagged, I, Pair<C, A>, Pair<C, B>>.unSecond(): Pro<ForTagged, I, A, B> =
    Tagged(fix().b.second)
}
