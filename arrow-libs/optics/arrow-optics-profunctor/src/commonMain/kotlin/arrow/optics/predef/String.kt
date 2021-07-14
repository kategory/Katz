package arrow.optics.predef

import arrow.optics.FoldK
import arrow.optics.IxFold
import arrow.optics.IxTraversal
import arrow.optics.Optic
import arrow.optics.Optic_
import arrow.optics.TraversalK
import arrow.optics.compose
import arrow.optics.get
import arrow.optics.iso
import arrow.optics.ixComposeRight
import kotlin.jvm.JvmName

fun Optic.Companion.foldedString(): IxFold<Int, String, Char> =
  Optic.get { str: String -> str.asIterable() }.ixComposeRight(foldedIterable())

@JvmName("string_folded")
fun <K : FoldK, I, S, T, B> Optic<K, I, S, T, String, B>.foldedIterable(): Optic<FoldK, I, S, T, Char, Nothing> =
  compose(Optic.foldedString())

@JvmName("string_traversed")
fun Optic.Companion.traversedString(): IxTraversal<Int, String, Char> =
  Optic.iso({ str: String -> str.toList() }, { xs: List<Char> -> xs.joinToString("") })
    .ixComposeRight(Optic.traversedList())

fun <K : TraversalK, I, S> Optic_<K, I, S, String>.traversed(): Optic_<TraversalK, I, S, Char> =
  compose(Optic.traversedString())
