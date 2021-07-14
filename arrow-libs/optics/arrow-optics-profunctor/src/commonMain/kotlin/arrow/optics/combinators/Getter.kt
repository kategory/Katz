package arrow.optics.combinators

import arrow.optics.AffineFoldK
import arrow.optics.FoldK
import arrow.optics.GetterK
import arrow.optics.IxGetter
import arrow.optics.Optic
import arrow.optics.compose
import arrow.optics.get
import arrow.optics.ixGet
import kotlin.jvm.JvmName

fun <S> Optic.Companion.selfIndex(): IxGetter<S, S, S> =
  ixGet { s -> s to s }

@JvmName("getFold")
fun <K : FoldK, I, S, T, A, B, C> Optic<K, I, S, T, A, C>.get(f: (A) -> B): Optic<FoldK, I, S, T, B, Nothing> =
  this.compose(Optic.get(f))
@JvmName("getAffineFold")
fun <K : AffineFoldK, I, S, T, A, B, C> Optic<K, I, S, T, A, C>.get(f: (A) -> B): Optic<AffineFoldK, I, S, T, B, Nothing> =
  this.compose(Optic.get(f))
@JvmName("getGetter")
fun <K : GetterK, I, S, T, A, B, C> Optic<K, I, S, T, A, C>.get(f: (A) -> B): Optic<GetterK, I, S, T, B, Nothing> =
  this.compose(Optic.get(f))
