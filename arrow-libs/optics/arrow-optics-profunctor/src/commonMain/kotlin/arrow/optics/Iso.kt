package arrow.optics

import arrow.optics.internal.Pro
import arrow.optics.internal.Profunctor

typealias Iso<S, A> = PIso<S, S, A, A>
typealias PIso<S, T, A, B> = Optic<IsoK, Any?, S, T, A, B>

fun <S, T, A, B> Optic.Companion.iso(
  to: (S) -> A,
  from: (B) -> T
): PIso<S, T, A, B> =
  object : PIso<S, T, A, B> {
    override fun <P, J> Profunctor<P>.transform(focus: Pro<P, J, A, B>): Pro<P, (Any?) -> J, S, T> =
      focus.dimap(to, from).ixMap { it(Unit) }
  }
