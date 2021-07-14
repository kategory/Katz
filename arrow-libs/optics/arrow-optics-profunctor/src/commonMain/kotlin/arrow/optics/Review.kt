package arrow.optics

import arrow.optics.internal.Pro
import arrow.optics.internal.Profunctor
import arrow.optics.internal.Tagged
import arrow.optics.internal.fix

typealias Review<B, T> = Optic<ReviewK, Any?, Nothing, T, Nothing, B>

// TODO Better name?
fun <B, T> Optic.Companion.unGet(f: (B) -> T): Review<B, T> =
  object : Review<B, T> {
    override fun <P, J> Profunctor<P>.transform(focus: Pro<P, J, Nothing, B>): Pro<P, (Any?) -> J, Nothing, T> =
      // Safe because ReviewK ensures Tagged or similar is used
      focus.rMap(f) as Pro<P, (Any?) -> J, Nothing, T>
  }

fun <K : ReviewK, I, S, T, A, B> B.review(optic: Optic<K, I, S, T, A, B>): T =
  Tagged.choice().run { optic.run { transform(Tagged<I, A, B>(this@review)) } }
    .fix().b
