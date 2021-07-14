package arrow.optics.typeclasses

import arrow.core.Either
import arrow.optics.Optic
import arrow.optics.Prism
import arrow.optics.predef.isNull
import arrow.optics.prism
import arrow.typeclasses.Monoid

fun interface Empty<S> {
  fun empty(): Prism<S, Unit>

  companion object {
    fun <A> fromMonoid(MA: Monoid<A>): Empty<A> = Empty {
      Optic.prism({ s ->
        if (MA.empty() == s) Either.Right(Unit) else Either.Left(s)
      }, { MA.empty() })
    }
    fun <A> list(): Empty<List<A>> = Empty {
      Optic.prism({ s ->
        if (s.isEmpty()) Either.Right(Unit) else Either.Left(s)
      }, { emptyList() })
    }
    fun <A> sequence(): Empty<Sequence<A>> = Empty {
      Optic.prism({ s ->
        if (s.firstOrNull() == null) Either.Right(Unit) else Either.Left(s)
      }, { emptySequence() })
    }
    fun <A> set(): Empty<Set<A>> = Empty {
      Optic.prism({ s ->
        if (s.isEmpty()) Either.Right(Unit) else Either.Left(s)
      }, { emptySet() })
    }
    fun <K, A> map(): Empty<Map<K, A>> = Empty {
      Optic.prism({ s ->
        if (s.isEmpty()) Either.Right(Unit) else Either.Left(s)
      }, { emptyMap() })
    }
    fun <A> nullable(): Empty<A?> = Empty { Optic.isNull() }
    fun string(): Empty<String> = Empty {
      Optic.prism({ s ->
        if (s.isEmpty()) Either.Right(Unit) else Either.Left(s)
      }, { "" })
    }
    inline fun <reified A> array(): Empty<Array<A>> = Empty {
      Optic.prism({ s ->
        if (s.isEmpty()) Either.Right(Unit) else Either.Left(s)
      }, { emptyArray() })
    }
  }
}
