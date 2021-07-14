package arrow.optics.combinators

import arrow.core.identity
import arrow.optics.Iso
import arrow.optics.Optic
import arrow.optics.iso

fun <S> Optic.Companion.id(): Iso<S, S> = Optic.iso(::identity, ::identity)

// It is somewhat important to return null if the right side returns the default.
//  This makes default more convenient for use when e.g. getting/setting from a map using at, where we don't want to
//  write back the default everywhere!
fun <A> Optic.Companion.default(a: A): Iso<A?, A> =
  Optic.iso({ it ?: a }, { if (it == a) null else it })
