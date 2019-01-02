package arrow.test.laws

import arrow.Kind
import arrow.core.compose
import arrow.core.identity
import arrow.test.generators.genConstructor
import arrow.test.generators.genFunctionAToB
import arrow.typeclasses.Eq
import arrow.typeclasses.Invariant
import io.kotlintest.properties.Gen
import io.kotlintest.properties.forAll

object InvariantLaws {

    fun <F> laws(IF: Invariant<F>, cf: (Int) -> Kind<F, Int>, EQ: Eq<Kind<F, Int>>,subName:String=""): List<Law> =
        listOf(
            Law("Invariant Laws: Invariant Identity $subName") { IF.identity(cf, EQ) },
            Law("Invariant Laws: Invariant Composition $subName") { IF.composition(cf, EQ) }
        )

    fun <F> Invariant<F>.identity(cf: (Int) -> Kind<F, Int>, EQ: Eq<Kind<F, Int>>): Unit =
        forAll(genConstructor(Gen.int(), cf)) { fa: Kind<F, Int> ->
            val imap: Kind<F, Int> = fa.imap<Int, Int>(::identity, ::identity)
            imap.equalUnderTheLaw(fa, EQ)
        }

    fun <F> Invariant<F>.composition(cf: (Int) -> Kind<F, Int>, EQ: Eq<Kind<F, Int>>): Unit =
        forAll(
            genConstructor(Gen.int(), cf),
            genFunctionAToB<Int, Int>(Gen.int()),
            genFunctionAToB<Int, Int>(Gen.int()),
            genFunctionAToB<Int, Int>(Gen.int()),
            genFunctionAToB<Int, Int>(Gen.int())
        ) { fa: Kind<F, Int>, f1, f2, g1, g2 ->
            fa.imap(f1, f2).imap(g1, g2).equalUnderTheLaw(fa.imap(g1 compose f1, f2 compose g2), EQ)
        }
}