package arrow.optics

import arrow.core.identity
import arrow.optics.internal.Function1
import arrow.optics.internal.IxFunction1
import arrow.optics.internal.Mapping
import arrow.optics.internal.Pro
import arrow.optics.internal.Profunctor
import arrow.optics.internal.fix

typealias Setter<S, A> = PSetter<S, S, A, A>
typealias PSetter<S, T, A, B> = Optic<SetterK, Any?, S, T, A, B>

fun <S, T, A, B> Optic.Companion.set(
  setter: (inner: (A) -> B, s: S) -> T
): PSetter<S, T, A, B> =
  object : PSetter<S, T, A, B> {
    override fun <P, J> Profunctor<P>.transform(focus: Pro<P, J, A, B>): Pro<P, (Any?) -> J, S, T> =
      (this as Mapping<P>).run {
        focus.roam(setter).ixMap { it(Unit) }
      }
  }

typealias IxSetter<I, S, A> = PIxSetter<I, S, S, A, A>
typealias PIxSetter<I, S, T, A, B> = Optic<SetterK, I, S, T, A, B>

fun <I, S, T, A, B> Optic.Companion.ixSet(
  setter: (inner: (I, A) -> B, s: S) -> T
): PIxSetter<I, S, T, A, B> =
  object : PIxSetter<I, S, T, A, B> {
    override fun <P, J> Profunctor<P>.transform(focus: Pro<P, J, A, B>): Pro<P, (I) -> J, S, T> =
      (this as Mapping<P>).run {
        focus.iroam(setter)
      }
  }

fun <K : SetterK, I, S, T, A, B> S.modify(optic: Optic<K, I, S, T, A, B>, f: (A) -> B): T =
  Function1.mapping().run { optic.run { transform(Function1<I, A, B>(f)) } }.fix().f(this)

fun <K : SetterK, I, S, T, A, B> S.set(optic: Optic<K, I, S, T, A, B>, v: B): T =
  modify(optic) { v }

fun <K : SetterK, I, S, T, A, B> S.ixModify(optic: Optic<K, I, S, T, A, B>, f: (I, A) -> B): T =
  IxFunction1.mapping().run { optic.run { transform(IxFunction1(f)) } }
    .fix().f(::identity, this)
