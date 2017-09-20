package kategory.optics

import kategory.Either
import kategory.Functor
import kategory.HK
import kategory.functor

typealias Setter<S, A> = PSetter<S, S, A, A>

/**
 * A [[PSetter]] is a generalisation of Functor map:
 *  - `map:    (A => B) => F[A] => F[B]`
 *  - `modify: (A => B) => S    => T`
 *
 * @param S the source of a [PSetter]
 * @param A the target of a [PSetter]
 */
abstract class PSetter<S, T, A, B> {

    /**
     * Modify polymorphically the target of a [PSetter] with a function
     */
    abstract fun modify(f: (A) -> B): (S) -> T

    /**
     * Set polymorphically the target of a [PSetter] with a value
     */
    abstract fun set(b: B): (S) -> T

    companion object {

        fun <A> id() = Iso.id<A>().asSetter()

        fun <A> codiagonal(): Setter<Either<A, A>, A> = Setter { f -> { aa -> aa.bimap(f, f) } }

        /**
         * Create a [PSetter] using modify function
         */
        operator fun <S, T, A, B> invoke(modify: ((A) -> B) -> (S) -> T): PSetter<S, T, A, B> = object : PSetter<S, T, A, B>() {
            override fun modify(f: (A) -> B): (S) -> T = modify(f)

            override fun set(b: B): (S) -> T = modify { b }
        }

        /**
         * Create a [PSetter] from a [kategory.Functor]
         */
        inline fun <reified F, A, B> fromFunctor(FF: Functor<F> = functor()): PSetter<HK<F, A>, HK<F, B>, A, B> = PSetter { f ->
            { fs: HK<F,A> -> FF.map(fs, f) }
        }
    }

    /**
     * Join two [PSetter] with the same target
     */
    fun <U, V> choice(other: PSetter<U, V, A, B>): PSetter<Either<S, U>, Either<T, V>, A, B> = PSetter { f ->
        { ac -> ac.bimap(modify(f), other.modify(f)) }
    }

    /**
     * Compose a [PSetter] with a [PSetter]
     */
    infix fun <C, D> compose(other: PSetter<A, B, C, D>): PSetter<S, T, C, D> = PSetter { fb ->
        modify(other.modify(fb))
    }

    /**
     * Compose a [PSetter] with a [POptional]
     */
    infix fun <C, D> compose(other: POptional<A, B, C, D>): PSetter<S, T, C, D> = compose(other.asSetter())

    /**
     * Compose a [PSetter] with a [PPrism]
     */
    infix fun <C, D> compose(other: PPrism<A, B, C, D>): PSetter<S, T, C, D> = compose(other.asSetter())

    /**
     * Compose a [PSetter] with a [PLens]
     */
    infix fun <C, D> compose(other: PLens<A, B, C, D>): PSetter<S, T, C, D> = compose(other.asSetter())

    /**
     * Compose a [PSetter] with a [PIso]
     */
    infix fun <C, D> compose(other: PIso<A, B, C, D>): PSetter<S, T, C, D> = compose(other.asSetter())

    /**
     * Plus operator overload to compose optionals
     */
    operator fun <C, D> plus(o: PSetter<A, B, C, D>): PSetter<S, T, C, D> = compose(o)

    operator fun <C, D> plus(o: POptional<A, B, C, D>): PSetter<S, T, C, D> = compose(o)

    operator fun <C, D> plus(o: PPrism<A, B, C, D>): PSetter<S, T, C, D> = compose(o)

    operator fun <C, D> plus(o: PLens<A, B, C, D>): PSetter<S, T, C, D> = compose(o)

    operator fun <C, D> plus(o: PIso<A, B, C, D>): PSetter<S, T, C, D> = compose(o)

}