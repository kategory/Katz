package arrow.optics.typeclasses

import arrow.core.Either
import arrow.core.Nel
import arrow.optics.AffineTraversalK
import arrow.optics.Optic
import arrow.optics.aTraversing

// TODO replace typeclasses with just objects that have ext functions for their types?
//  All one does with Ixed/At/Each is call their respective method right? So why not do Ixed.list(i: Int) = Optic<...> directly?
fun interface Ixed<K, I, S, A> {
  fun ix(i: I): Optic<K, Any?, S, S, A, A>

  companion object {
    fun <A> list() =
      Ixed<AffineTraversalK, Int, List<A>, A> { i ->
        Optic.aTraversing({ xs ->
          if (xs.size > i) Either.Right(xs[i]) else Either.Left(xs)
        }, { xs, a ->
          if (xs.size > i) xs.toMutableList().also { it[i] = a }
          else xs
        })
      }

    fun <A> set() = Ixed<AffineTraversalK, A, Set<A>, Unit> { a ->
      Optic.aTraversing({ s ->
        if (a in s) Either.Right(Unit) else Either.Left(s)
      }, { s, _ -> if (a in s) s + a else s })
    }

    fun <K, V> map() = Ixed<AffineTraversalK, K, Map<K, V>, V> { k ->
      Optic.aTraversing({ s ->
        s[k]?.let { Either.Right(it) } ?: Either.Left(s)
      }, { s, v ->
        if (k in s) s + (k to v) else s
      })
    }

    fun <A> nonEmptyList() = Ixed<AffineTraversalK, Int, Nel<A>, A> { i ->
      Optic.aTraversing({ xs ->
        if (xs.size > i) Either.Right(xs[i]) else Either.Left(xs)
      }, { xs, a ->
        if (xs.size > i) xs.all.toMutableList().also { it[i] = a }.let { Nel.fromListUnsafe(it) }
        else xs
      })
    }

    fun <A> sequence() = Ixed<AffineTraversalK, Int, Sequence<A>, A> { i ->
      Optic.aTraversing({ xs ->
        xs.drop(i).firstOrNull()?.let { Either.Right(it) } ?: Either.Left(xs)
      }, { xs, a ->
        xs.mapIndexed { ind, v -> if (i == ind) a else v }
      })
    }

    fun string() = Ixed<AffineTraversalK, Int, String, Char> { i ->
      Optic.aTraversing({ str ->
        if (str.length > i) Either.Right(str[i]) else Either.Left(str)
      }, { str, c ->
        if (str.length > i)
          str.toCharArray().copyOf().also { it[i] = c }.concatToString()
        else str
      })
    }

    fun byteArray() = Ixed<AffineTraversalK, Int, ByteArray, Byte> { i ->
      Optic.aTraversing({ arr ->
        if (arr.size > i) Either.Right(arr[i]) else Either.Left(arr)
      }, { arr, b ->
        if (arr.size > i) arr.copyOf().also { it[i] = b } else arr
      })
    }

    fun charArray() = Ixed<AffineTraversalK, Int, CharArray, Char> { i ->
      Optic.aTraversing({ arr ->
        if (arr.size > i) Either.Right(arr[i]) else Either.Left(arr)
      }, { arr, b ->
        if (arr.size > i) arr.copyOf().also { it[i] = b } else arr
      })
    }

    fun shortArray() = Ixed<AffineTraversalK, Int, ShortArray, Short> { i ->
      Optic.aTraversing({ arr ->
        if (arr.size > i) Either.Right(arr[i]) else Either.Left(arr)
      }, { arr, b ->
        if (arr.size > i) arr.copyOf().also { it[i] = b } else arr
      })
    }

    fun intArray() = Ixed<AffineTraversalK, Int, IntArray, Int> { i ->
      Optic.aTraversing({ arr ->
        if (arr.size > i) Either.Right(arr[i]) else Either.Left(arr)
      }, { arr, b ->
        if (arr.size > i) arr.copyOf().also { it[i] = b } else arr
      })
    }

    fun longArray() = Ixed<AffineTraversalK, Int, LongArray, Long> { i ->
      Optic.aTraversing({ arr ->
        if (arr.size > i) Either.Right(arr[i]) else Either.Left(arr)
      }, { arr, b ->
        if (arr.size > i) arr.copyOf().also { it[i] = b } else arr
      })
    }

    fun floatArray() = Ixed<AffineTraversalK, Int, FloatArray, Float> { i ->
      Optic.aTraversing({ arr ->
        if (arr.size > i) Either.Right(arr[i]) else Either.Left(arr)
      }, { arr, b ->
        if (arr.size > i) arr.copyOf().also { it[i] = b } else arr
      })
    }

    fun doubleArray() = Ixed<AffineTraversalK, Int, DoubleArray, Double> { i ->
      Optic.aTraversing({ arr ->
        if (arr.size > i) Either.Right(arr[i]) else Either.Left(arr)
      }, { arr, b ->
        if (arr.size > i) arr.copyOf().also { it[i] = b } else arr
      })
    }

    fun <A> array() = Ixed<AffineTraversalK, Int, Array<A>, A> { i ->
      Optic.aTraversing({ arr ->
        if (arr.size > i) Either.Right(arr[i]) else Either.Left(arr)
      }, { arr, b ->
        if (arr.size > i) arr.copyOf().also { it[i] = b } else arr
      })
    }
  }
}
