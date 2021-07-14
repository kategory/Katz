package arrow.optics.combinators

import arrow.core.Either
import arrow.optics.GetterK
import arrow.optics.IsoK
import arrow.optics.LensK
import arrow.optics.Optic
import arrow.optics.PrismK
import arrow.optics.ReversedLensK
import arrow.optics.ReversedPrismK
import arrow.optics.ReviewK
import arrow.optics.internal.Choice
import arrow.optics.internal.CoChoice
import arrow.optics.internal.CoStrong
import arrow.optics.internal.IxLinearF
import arrow.optics.internal.Kind
import arrow.optics.internal.Pro
import arrow.optics.internal.Profunctor
import arrow.optics.internal.Strong
import kotlin.jvm.JvmName

// All below implementations are equal but have different constraints on P
// isomorphism between getter/review
@JvmName("re_review")
fun <K : ReviewK, S, T, A, B> Optic<K, Any?, S, T, A, B>.re(): Optic<GetterK, Any?, B, A, T, S> =
  object : Optic<GetterK, Any?, B, A, T, S> {
    override fun <P, J> Profunctor<P>.transform(focus: Pro<P, J, T, S>): Pro<P, (Any?) -> J, B, A> {
      val re = Re.profunctor<P, A, B>(this).run { transform(Re<P, J, A, B, A, B> { it }) }.fix()
      return re.unRe(focus.ixMap { it(Unit) })
    }
  }

@JvmName("re_getter")
fun <K : GetterK, S, T, A, B> Optic<K, Any?, S, T, A, B>.re(): Optic<ReviewK, Any?, B, A, T, S> =
  object : Optic<ReviewK, Any?, B, A, T, S> {
    override fun <P, J> Profunctor<P>.transform(focus: Pro<P, J, T, S>): Pro<P, (Any?) -> J, B, A> {
      val re = Re.profunctor<P, A, B>(this).run { transform(Re<P, J, A, B, A, B> { it }) }.fix()
      return re.unRe(focus.ixMap { it(Unit) })
    }
  }

// isomorphism between prism/reversedPrism
@JvmName("re_prism")
fun <K : PrismK, S, T, A, B> Optic<K, Any?, S, T, A, B>.re(): Optic<ReversedPrismK, Any?, B, A, T, S> =
  object : Optic<ReversedPrismK, Any?, B, A, T, S> {
    override fun <P, J> Profunctor<P>.transform(focus: Pro<P, J, T, S>): Pro<P, (Any?) -> J, B, A> {
      val re = Re.choice<P, A, B>(this as CoChoice<P>).run { transform(Re<P, J, A, B, A, B> { it }) }.fix()
      return re.unRe(focus.ixMap { it(Unit) })
    }
  }

@JvmName("re_reversedPrism")
fun <K : ReversedPrismK, S, T, A, B> Optic<K, Any?, S, T, A, B>.re(): Optic<PrismK, Any?, B, A, T, S> =
  object : Optic<PrismK, Any?, B, A, T, S> {
    override fun <P, J> Profunctor<P>.transform(focus: Pro<P, J, T, S>): Pro<P, (Any?) -> J, B, A> {
      val re = Re.coChoice<P, A, B>(this as Choice<P>).run { transform(Re<P, J, A, B, A, B> { it }) }.fix()
      return re.unRe(focus.ixMap { it(Unit) })
    }
  }

// isomorphism between lens/reversedLens
@JvmName("re_lens")
fun <K : LensK, S, T, A, B> Optic<K, Any?, S, T, A, B>.re(): Optic<ReversedLensK, Any?, B, A, T, S> =
  object : Optic<ReversedLensK, Any?, B, A, T, S> {
    override fun <P, J> Profunctor<P>.transform(focus: Pro<P, J, T, S>): Pro<P, (Any?) -> J, B, A> {
      val re = Re.strong<P, A, B>(this as CoStrong<P>).run { transform(Re<P, J, A, B, A, B> { it }) }.fix()
      return re.unRe(focus.ixMap { it(Unit) })
    }
  }

@JvmName("re_reversedLens")
fun <K : ReversedLensK, S, T, A, B> Optic<K, Any?, S, T, A, B>.re(): Optic<LensK, Any?, B, A, T, S> =
  object : Optic<LensK, Any?, B, A, T, S> {
    override fun <P, J> Profunctor<P>.transform(focus: Pro<P, J, T, S>): Pro<P, (Any?) -> J, B, A> {
      val re = Re.coStrong<P, A, B>(this as Strong<P>).run { transform(Re<P, J, A, B, A, B> { it }) }.fix()
      return re.unRe(focus.ixMap { it(Unit) })
    }
  }

// reversing isos yields isos
@JvmName("re_iso")
fun <K : IsoK, S, T, A, B> Optic<K, Any?, S, T, A, B>.re(): Optic<IsoK, Any?, B, A, T, S> =
  object : Optic<IsoK, Any?, B, A, T, S> {
    override fun <P, J> Profunctor<P>.transform(focus: Pro<P, J, T, S>): Pro<P, (Any?) -> J, B, A> {
      val re = Re.profunctor<P, A, B>(this).run { transform(Re<P, J, A, B, A, B> { it }) }.fix()
      return re.unRe(focus.ixMap { it(Unit) })
    }
  }

internal class ForRe
internal inline fun <P, I, S, T, A, B> Pro<Kind<Kind<Kind<ForRe, P>, S>, T>, I, A, B>.fix(): Re<P, I, S, T, A, B> = this as Re<P, I, S, T, A, B>
internal class Re<P, I, S, T, A, B>(val unRe: (Pro<P, I, B, A>) -> Pro<P, I, T, S>) : Pro<Kind<Kind<Kind<ForRe, P>, S>, T>, I, A, B> {
  companion object {
    fun <P, S, T> profunctor(PP: Profunctor<P>) = object : ReProfunctor<P, S, T> {
      override fun PP(): Profunctor<P> = PP
    }
    fun <P, S, T> strong(CSP: CoStrong<P>) = object : ReStrong<P, S, T> {
      override fun CSP(): CoStrong<P> = CSP
    }
    fun <P, S, T> choice(CCP: CoChoice<P>) = object : ReChoice<P, S, T> {
      override fun CCP(): CoChoice<P> = CCP
    }
    fun <P, S, T> coStrong(SP: Strong<P>) = object : ReCoStrong<P, S, T> {
      override fun SP(): Strong<P> = SP
    }
    fun <P, S, T> coChoice(CP: Choice<P>) = object : ReCoChoice<P, S, T> {
      override fun CP(): Choice<P> = CP
    }
  }
}

internal interface ReProfunctor<P, S, T> : Profunctor<Kind<Kind<Kind<ForRe, P>, S>, T>> {
  fun PP(): Profunctor<P>

  override fun <I, A, B, C, D> Pro<Kind<Kind<Kind<ForRe, P>, S>, T>, I, B, C>.dimap(
    f: (A) -> B,
    g: (C) -> D
  ): Pro<Kind<Kind<Kind<ForRe, P>, S>, T>, I, A, D> =
    Re { fix().unRe(PP().run { it.dimap(g, f) }) }

  override fun <I, A, B, C> Pro<Kind<Kind<Kind<ForRe, P>, S>, T>, I, B, C>.lMap(f: (A) -> B): Pro<Kind<Kind<Kind<ForRe, P>, S>, T>, I, A, C> =
    Re { fix().unRe(PP().run { it.rMap(f) }) }

  override fun <I, B, C, D> Pro<Kind<Kind<Kind<ForRe, P>, S>, T>, I, B, C>.rMap(g: (C) -> D): Pro<Kind<Kind<Kind<ForRe, P>, S>, T>, I, B, D> =
    Re { fix().unRe(PP().run { it.lMap(g) }) }

  // reversed optics cannot have indices
  override fun <I, J, A, B> Pro<Kind<Kind<Kind<ForRe, P>, S>, T>, J, A, B>.ixMap(f: (I) -> J): Pro<Kind<Kind<Kind<ForRe, P>, S>, T>, I, A, B> =
    this as Re<P, I, S, T, A, B>
}

internal interface ReStrong<P, S, T> : Strong<Kind<Kind<Kind<ForRe, P>, S>, T>>, ReProfunctor<P, S, T> {
  override fun PP(): Profunctor<P> = CSP()
  fun CSP(): CoStrong<P>

  override fun <I, A, B, C> Pro<Kind<Kind<Kind<ForRe, P>, S>, T>, I, A, B>.first(): Pro<Kind<Kind<Kind<ForRe, P>, S>, T>, I, Pair<A, C>, Pair<B, C>> =
    Re { fix().unRe(CSP().run { it.unFirst() }) }

  override fun <I, A, B, C> Pro<Kind<Kind<Kind<ForRe, P>, S>, T>, I, A, B>.second(): Pro<Kind<Kind<Kind<ForRe, P>, S>, T>, I, Pair<C, A>, Pair<C, B>> =
    Re { fix().unRe(CSP().run { it.unSecond() }) }

  override fun <I, J, S1, T1, A, B> Pro<Kind<Kind<Kind<ForRe, P>, S>, T>, J, A, B>.ilinear(f: IxLinearF<I, S1, T1, A, B>): Pro<Kind<Kind<Kind<ForRe, P>, S>, T>, (I) -> J, S1, T1> =
    throw IllegalStateException("ilinear:Re should not be reachable")
}

internal interface ReCoStrong<P, S, T> : CoStrong<Kind<Kind<Kind<ForRe, P>, S>, T>>, ReProfunctor<P, S, T> {
  override fun PP(): Profunctor<P> = SP()
  fun SP(): Strong<P>

  override fun <I, A, B, C> Pro<Kind<Kind<Kind<ForRe, P>, S>, T>, I, Pair<A, C>, Pair<B, C>>.unFirst(): Pro<Kind<Kind<Kind<ForRe, P>, S>, T>, I, A, B> =
    Re { fix().unRe(SP().run { it.first() }) }

  override fun <I, A, B, C> Pro<Kind<Kind<Kind<ForRe, P>, S>, T>, I, Pair<C, A>, Pair<C, B>>.unSecond(): Pro<Kind<Kind<Kind<ForRe, P>, S>, T>, I, A, B> =
    Re { fix().unRe(SP().run { it.second() }) }
}

internal interface ReChoice<P, S, T> : Choice<Kind<Kind<Kind<ForRe, P>, S>, T>>, ReProfunctor<P, S, T> {
  override fun PP(): Profunctor<P> = CCP()
  fun CCP(): CoChoice<P>

  override fun <I, A, B, C> Pro<Kind<Kind<Kind<ForRe, P>, S>, T>, I, A, B>.left(): Pro<Kind<Kind<Kind<ForRe, P>, S>, T>, I, Either<A, C>, Either<B, C>> =
    Re { fix().unRe(CCP().run { it.unLeft() }) }

  override fun <I, A, B, C> Pro<Kind<Kind<Kind<ForRe, P>, S>, T>, I, A, B>.right(): Pro<Kind<Kind<Kind<ForRe, P>, S>, T>, I, Either<C, A>, Either<C, B>> =
    Re { fix().unRe(CCP().run { it.unRight() }) }
}

internal interface ReCoChoice<P, S, T> : CoChoice<Kind<Kind<Kind<ForRe, P>, S>, T>>, ReProfunctor<P, S, T> {
  override fun PP(): Profunctor<P> = CP()
  fun CP(): Choice<P>

  override fun <I, A, B, C> Pro<Kind<Kind<Kind<ForRe, P>, S>, T>, I, Either<A, C>, Either<B, C>>.unLeft(): Pro<Kind<Kind<Kind<ForRe, P>, S>, T>, I, A, B> =
    Re { fix().unRe(CP().run { it.left() }) }

  override fun <I, A, B, C> Pro<Kind<Kind<Kind<ForRe, P>, S>, T>, I, Either<C, A>, Either<C, B>>.unRight(): Pro<Kind<Kind<Kind<ForRe, P>, S>, T>, I, A, B> =
    Re { fix().unRe(CP().run { it.right() }) }
}
