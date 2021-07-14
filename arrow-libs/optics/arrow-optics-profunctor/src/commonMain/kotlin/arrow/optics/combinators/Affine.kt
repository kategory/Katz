package arrow.optics.combinators

import arrow.optics.AffineFoldK
import arrow.optics.AffineTraversalK
import arrow.optics.Getter
import arrow.optics.Lens
import arrow.optics.Optic
import arrow.optics.get
import arrow.optics.lens
import arrow.optics.set
import arrow.optics.viewOrNull
import kotlin.jvm.JvmName

@JvmName("at_affineTraversal")
fun <K : AffineTraversalK, I, S, A> Optic<K, I, S, S, A, A>.at(
  ind: I
): Lens<S, A?> =
  Optic.lens({ s ->
    s.viewOrNull(this@at.index(ind))
  }, { s, a: A? ->
    a?.let { s.set(this@at.index(ind), it) } ?: s
  })

@JvmName("at_affineFold")
fun <K : AffineFoldK, I, S, A> Optic<K, I, S, S, A, A>.at(
  ind: I
): Getter<S, A?> =
  Optic.get { s -> s.viewOrNull(this@at.index(ind)) }
