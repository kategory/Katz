package arrow.core

import arrow.Kind
import arrow.higherkind
import arrow.typeclasses.Applicative
import arrow.typeclasses.Semigroup

typealias ValidatedNel<E, A> = Validated<Nel<E>, A>
typealias Valid<A> = Validated.Valid<A>
typealias Invalid<E> = Validated.Invalid<E>

/**
 *
 * ank_macro_hierarchy(arrow.core.Validated)
 *
 * {:.beginner}
 * beginner
 *
 * Imagine you are filling out a web form to sign up for an account. You input your username and
 * password and submit. Response comes back saying your username can't have dashes in it,
 * so you make some changes and resubmit. Can't have special characters either. Change, resubmit.
 * Passwords need to have at least one capital letter. Change, resubmit. Password needs to have at least one number.
 *
 * Or perhaps you're reading from a configuration file. One could imagine the configuration library
 * you're using returns a `Try`, or maybe a `Either`. Your parsing may look something like:
 *
 * ```kotlin:ank
 * data class ConnectionParams(val url: String, val port: Int)
 * ```
 *
 * ```kotlin
 * fun <A> config(key: String): Either<String, A> = TODO()
 *
 * config<String>("url").flatMap { url ->
 *  config<Int>("port").map { ConnectionParams(url, it) }
 * }
 * ```
 *
 * You run your program and it says key "url" not found, turns out the key was "endpoint". So
 * you change your code and re-run. Now it says the "port" key was not a well-formed integer.
 *
 * It would be nice to have all of these errors reported simultaneously. That the username can't
 * have dashes can be validated separately from it not having special characters, as well as
 * from the password needing to have certain requirements. A misspelled (or missing) field in
 * a config can be validated separately from another field not being well-formed.
 *
 * # Enter `Validated`.
 *
 * ## Parallel Validation
 *
 * Our goal is to report any and all errors across independent bits of data. For instance, when
 * we ask for several pieces of configuration, each configuration field can be validated separately
 * from one another. How then do we enforce that the data we are working with is independent?
 * We ask for both of them up front.
 *
 * As our running example, we will look at config parsing. Our config will be represented by a
 * `Map<String, String>`. Parsing will be handled by a `Read` type class - we provide instances only
 * for `String` and `Int` for brevity.
 *
 * ```kotlin:ank
 * import arrow.*
 * import arrow.core.*
 *
 * abstract class Read<A> {
 *
 * abstract fun read(s: String): Option<A>
 *
 *  companion object {
 *
 *   val stringRead: Read<String> =
 *    object: Read<String>() {
 *     override fun read(s: String): Option<String> = Option(s)
 *    }
 *
 *   val intRead: Read<Int> =
 *    object: Read<Int>() {
 *     override fun read(s: String): Option<Int> =
 *      if (s.matches(Regex("-?[0-9]+"))) Option(s.toInt()) else None
 *    }
 *  }
 * }
 * ```
 *
 * Then we enumerate our errors—when asking for a config value, one of two things can go wrong:
 * the field is missing, or it is not well-formed with regards to the expected type.
 *
 * ```kotlin:ank
 * sealed class ConfigError {
 * data class MissingConfig(val field: String): ConfigError()
 * data class ParseConfig(val field: String): ConfigError()
 * }
 * ```
 *
 * We need a data type that can represent either a successful value (a parsed configuration), or an error.
 * It would look like the following, which Arrow provides in `arrow.Validated`:
 *
 * ```kotlin
 * @higherkind sealed class Validated<out E, out A> : ValidatedOf<E, A> {
 *
 *  data class Valid<out A>(val a: A) : Validated<Nothing, A>()
 *
 *  data class Invalid<out E>(val e: E) : Validated<E, Nothing>()
 * }
 * ```
 *
 * Now we are ready to write our parser.
 *
 * ```kotlin:ank
 * import arrow.core.*
 * data class Config(val map: Map<String, String>) {
 *  fun <A> parse(read: Read<A>, key: String): Validated<ConfigError, A> {
 *   val v = Option.fromNullable(map[key])
 *   return when(v) {
 *    is Some ->
 *     when (val s = read.read(v.t)) {
 *      is Some -> s.t.valid()
 *      is None -> ConfigError.ParseConfig(key).invalid()
 *     }
 *    is None -> Validated.Invalid(ConfigError.MissingConfig(key))
 *   }
 *  }
 * }
 * ```
 *
 * Everything is in place to write the parallel validator. Recall that we can only do parallel
 * validation if each piece is independent. How do we enforce the data is independent? By
 * asking for all of it up front. Let's start with two pieces of data.
 *
 * ```kotlin
 * fun <E, A, B, C> parallelValidate(v1: Validated<E, A>, v2: Validated<E, B>, f: (A, B) -> C): Validated<E, C> {
 *  return when {
 *   v1 is Validated.Valid && v2 is Validated.Valid -> Validated.Valid(f(v1.a, v2.a))
 *   v1 is Validated.Valid && v2 is Validated.Invalid -> v2
 *   v1 is Validated.Invalid && v2 is Validated.Valid -> v1
 *   v1 is Validated.Invalid && v2 is Validated.Invalid -> TODO()
 *   else -> TODO()
 *  }
 * }
 * ```
 *
 * We've run into a problem. In the case where both have errors, We want to report both. we
 * don't have a way to combine ConfigErrors. But as clients, we can change our Validated
 * values where the error can be combined, say, a `List<ConfigError>`.We are going to use a
 * `NonEmptyList<ConfigError>`—the NonEmptyList statically guarantees we have at least one value,
 * which aligns with the fact that if we have an Invalid, then we most certainly have at least one error.
 * This technique is so common there is a convenient method on `Validated` called `toValidatedNel`
 * that turns any `Validated<E, A>` value to a `Validated<NonEmptyList<E>, A>`. Additionally, the
 * type alias `ValidatedNel<E, A>` is provided.
 *
 * Time to parse.
 *
 * ```kotlin:ank
 * fun <E, A, B, C> parallelValidate
 *   (v1: Validated<E, A>, v2: Validated<E, B>, f: (A, B) -> C): Validated<NonEmptyList<E>, C> {
 *  return when {
 *   v1 is Validated.Valid && v2 is Validated.Valid -> Validated.Valid(f(v1.a, v2.a))
 *   v1 is Validated.Valid && v2 is Validated.Invalid -> v2.toValidatedNel()
 *   v1 is Validated.Invalid && v2 is Validated.Valid -> v1.toValidatedNel()
 *   v1 is Validated.Invalid && v2 is Validated.Invalid -> Validated.Invalid(NonEmptyList(v1.e, listOf(v2.e)))
 *   else -> throw IllegalStateException("Not possible value")
 *  }
 * }
 * ```
 *
 * Kotlin says that our match is not exhaustive and we have to add `else`.
 *
 * When no errors are present in the configuration, we get a `ConnectionParams` wrapped in a `Valid` instance.
 *
 * ```kotlin:ank:playground
 * import arrow.core.*
 *
 * data class ConnectionParams(val url: String, val port: Int)
 * 
 * abstract class Read<A> {
 *  abstract fun read(s: String): Option<A>
 *
 *  companion object {
 * 
 *   val stringRead: Read<String> =
 *    object : Read<String>() {
 *     override fun read(s: String): Option<String> = Option(s)
 *    }
 * 
 *   val intRead: Read<Int> =
 *    object : Read<Int>() {
 *     override fun read(s: String): Option<Int> =
 *      if (s.matches(Regex("-?[0-9]+"))) Option(s.toInt()) else None
 *    }
 *  }
 * }
 * 
 * sealed class ConfigError {
 *  data class MissingConfig(val field: String) : ConfigError()
 *  data class ParseConfig(val field: String) : ConfigError()
 * }
 * 
 * data class Config(val map: Map<String, String>) {
 *  fun <A> parse(read: Read<A>, key: String): Validated<ConfigError, A> {
 *   val v = Option.fromNullable(map[key])
 *   return when (v) {
 *    is Some ->
 *     when (val s = read.read(v.t)) {
 *      is Some -> s.t.valid()
 *      is None -> ConfigError.ParseConfig(key).invalid()
 *     }
 *    is None -> Validated.Invalid(ConfigError.MissingConfig(key))
 *   }
 *  }
 * }
 * 
 * fun <E, A, B, C> parallelValidate(v1: Validated<E, A>, v2: Validated<E, B>, f: (A, B) -> C): Validated<E, C> {
 *  return when {
 *   v1 is Validated.Valid && v2 is Validated.Valid -> Validated.Valid(f(v1.a, v2.a))
 *   v1 is Validated.Valid && v2 is Validated.Invalid -> v2
 *   v1 is Validated.Invalid && v2 is Validated.Valid -> v1
 *   v1 is Validated.Invalid && v2 is Validated.Invalid -> TODO()
 *   else -> TODO()
 *  }
 * }
 * 
 * fun main() {
 * //sampleStart
 *  val config = Config(mapOf("url" to "127.0.0.1", "port" to "1337"))
 * 
 *  val valid = parallelValidate(
 *  config.parse(Read.stringRead, "url"),
 *  config.parse(Read.intRead, "port")
 *  ) { url, port ->
 *   ConnectionParams(url, port)
 * }
 * //sampleEnd
 *  println(valid)
 * }
 * ```
 *
 * But what happens when having one or more errors? They are accumulated in a `NonEmptyList` wrapped in
 * an `Invalid` instance.
 *
 * ```kotlin:ank:playground
 * import arrow.core.*
 *
 * data class ConnectionParams(val url: String, val port: Int)
 *
 * abstract class Read<A> {
 *  abstract fun read(s: String): Option<A>
 *
 *  companion object {
 *
 *   val stringRead: Read<String> =
 *    object : Read<String>() {
 *     override fun read(s: String): Option<String> = Option(s)
 *    }
 *
 *   val intRead: Read<Int> =
 *    object : Read<Int>() {
 *     override fun read(s: String): Option<Int> =
 *      if (s.matches(Regex("-?[0-9]+"))) Option(s.toInt()) else None
 *    }
 *  }
 * }
 *
 * sealed class ConfigError {
 *  data class MissingConfig(val field: String) : ConfigError()
 *  data class ParseConfig(val field: String) : ConfigError()
 * }
 *
 * data class Config(val map: Map<String, String>) {
 *  fun <A> parse(read: Read<A>, key: String): Validated<ConfigError, A> {
 *   val v = Option.fromNullable(map[key])
 *   return when (v) {
 *    is Some ->
 *     when (val s = read.read(v.t)) {
 *      is Some -> s.t.valid()
 *      is None -> ConfigError.ParseConfig(key).invalid()
 *     }
 *    is None -> Validated.Invalid(ConfigError.MissingConfig(key))
 *   }
 *  }
 * }
 *
 * fun <E, A, B, C> parallelValidate(v1: Validated<E, A>, v2: Validated<E, B>, f: (A, B) -> C): Validated<E, C> {
 *  return when {
 *   v1 is Validated.Valid && v2 is Validated.Valid -> Validated.Valid(f(v1.a, v2.a))
 *   v1 is Validated.Valid && v2 is Validated.Invalid -> v2
 *   v1 is Validated.Invalid && v2 is Validated.Valid -> v1
 *   v1 is Validated.Invalid && v2 is Validated.Invalid -> TODO()
 *   else -> TODO()
 *  }
 * }
 *
 * fun main() {
 * //sampleStart
 * val config = Config(mapOf("url" to "127.0.0.1", "port" to "not a number"))
 *
 * val valid = parallelValidate(
 *  config.parse(Read.stringRead, "url"),
 *  config.parse(Read.intRead, "port")
 *  ) { url, port ->
 *   ConnectionParams(url, port)
 *  }
 *  //sampleEnd
 *  println(valid)
 * }
 * ```
 *
 * ## Sequential Validation
 *
 * If you do want error accumulation but occasionally run into places where sequential validation is needed,
 * then Validated provides `withEither` method to allow you to temporarily turn a Validated
 * instance into an Either instance and apply it to a function.
 *
 * ```kotlin:ank:playground
 * import arrow.core.*
 *
 * abstract class Read<A> {
 *  abstract fun read(s: String): Option<A>
 *
 *  companion object {
 *
 *   val stringRead: Read<String> =
 *    object : Read<String>() {
 *     override fun read(s: String): Option<String> = Option(s)
 *    }
 *
 *   val intRead: Read<Int> =
 *    object : Read<Int>() {
 *     override fun read(s: String): Option<Int> =
 *      if (s.matches(Regex("-?[0-9]+"))) Option(s.toInt()) else None
 *    }
 *  }
 * }
 *
 * data class Config(val map: Map<String, String>) {
 *  fun <A> parse(read: Read<A>, key: String): Validated<ConfigError, A> {
 *   val v = Option.fromNullable(map[key])
 *   return when (v) {
 *    is Some ->
 *     when (val s = read.read(v.t)) {
 *      is Some -> s.t.valid()
 *      is None -> ConfigError.ParseConfig(key).invalid()
 *     }
 *    is None -> Validated.Invalid(ConfigError.MissingConfig(key))
 *   }
 *  }
 * }
 * sealed class ConfigError {
 *  data class MissingConfig(val field: String) : ConfigError()
 *  data class ParseConfig(val field: String) : ConfigError()
 * }
 *
 * fun positive(field: String, i: Int): Either<ConfigError, Int> {
 *  return if (i >= 0) i.right()
 *  else ConfigError.ParseConfig(field).left()
 * }
 *
 * fun main() {
 *  //sampleStart
 *  val config = Config(mapOf("house_number" to "-42"))
 *
 *
 *  val houseNumber = config.parse(Read.intRead, "house_number").withEither { either ->
 *   either.flatMap { positive("house_number", it) }
 *  }
 *  //sampleEnd
 *  println(houseNumber)
 * }
 *
 * ```
 *
 * ## Alternative validation strategies to Validated: using `ApplicativeError`
 *
 * We may use `ApplicativeError` instead of `Validated` to abstract away validation strategies and raising errors in the context we are computing in.
 *
 * ```kotlin:ank
 * import arrow.*
 * import arrow.core.*
 * import arrow.typeclasses.*
 * import arrow.core.extensions.validated.applicativeError.*
 * import arrow.core.extensions.either.applicativeError.*
 * import arrow.core.extensions.nonemptylist.semigroup.*
 *
 * //sampleStart
 * sealed class ValidationError(val msg: String) {
 *  data class DoesNotContain(val value: String) : ValidationError("Did not contain $value")
 *  data class MaxLength(val value: Int) : ValidationError("Exceeded length of $value")
 *  data class NotAnEmail(val reasons: Nel<ValidationError>) : ValidationError("Not a valid email")
 * }
 *
 * data class FormField(val label: String, val value: String)
 * data class Email(val value: String)
 *
 * sealed class Rules<F>(A: ApplicativeError<F, Nel<ValidationError>>) : ApplicativeError<F, Nel<ValidationError>> by A {
 *
 *  private fun FormField.contains(needle: String): Kind<F, FormField> =
 *   if (value.contains(needle, false)) just(this)
 *   else raiseError(ValidationError.DoesNotContain(needle).nel())
 *
 *  private fun FormField.maxLength(maxLength: Int): Kind<F, FormField> =
 *   if (value.length <= maxLength) just(this)
 *   else raiseError(ValidationError.MaxLength(maxLength).nel())
 *
 *  fun FormField.validateEmail(): Kind<F, Email> =
 *   map(contains("@"), maxLength(250), {
 *    Email(value)
 *   }).handleErrorWith { raiseError(ValidationError.NotAnEmail(it).nel()) }
 *
 *  object ErrorAccumulationStrategy :
 *    Rules<ValidatedPartialOf<Nel<ValidationError>>>(Validated.applicativeError(NonEmptyList.semigroup()))
 *
 *  object FailFastStrategy :
 *   Rules<EitherPartialOf<Nel<ValidationError>>>(Either.applicativeError())
 *
 *  companion object {
 *   infix fun <A> failFast(f: FailFastStrategy.() -> A): A = f(FailFastStrategy)
 *   infix fun <A> accumulateErrors(f: ErrorAccumulationStrategy.() -> A): A = f(ErrorAccumulationStrategy)
 *  }
 * }
 * //sampleEnd
 * ```
 *
 * `Rules` defines abstract behaviors that can be composed and have access to the scope of `ApplicativeError` where we can invoke `just` to lift values in to the positive result and `raiseError` into the error context.
 *
 * Once we have such abstract algebra defined we can simply materialize it to data types that support different error strategies:
 *
 *  *Error accumulation*
 *
 * ```kotlin:ank:playground
 * import arrow.*
 * import arrow.core.*
 * import arrow.typeclasses.*
 * import arrow.core.extensions.validated.applicativeError.*
 * import arrow.core.extensions.either.applicativeError.*
 * import arrow.core.extensions.nonemptylist.semigroup.*
 *
 * sealed class ValidationError(val msg: String) {
 *  data class DoesNotContain(val value: String) : ValidationError("Did not contain $value")
 *  data class MaxLength(val value: Int) : ValidationError("Exceeded length of $value")
 *  data class NotAnEmail(val reasons: Nel<ValidationError>) : ValidationError("Not a valid email")
 * }
 *
 * data class FormField(val label: String, val value: String)
 * data class Email(val value: String)
 *
 * sealed class Rules<F>(A: ApplicativeError<F, Nel<ValidationError>>) : ApplicativeError<F, Nel<ValidationError>> by A {
 *
 *  private fun FormField.contains(needle: String): Kind<F, FormField> =
 *   if (value.contains(needle, false)) just(this)
 *   else raiseError(ValidationError.DoesNotContain(needle).nel())
 *
 *  private fun FormField.maxLength(maxLength: Int): Kind<F, FormField> =
 *   if (value.length <= maxLength) just(this)
 *   else raiseError(ValidationError.MaxLength(maxLength).nel())
 *
 *  fun FormField.validateEmail(): Kind<F, Email> =
 *   map(contains("@"), maxLength(250), {
 *    Email(value)
 *   }).handleErrorWith { raiseError(ValidationError.NotAnEmail(it).nel()) }
 *
 *  object ErrorAccumulationStrategy :
 *    Rules<ValidatedPartialOf<Nel<ValidationError>>>(Validated.applicativeError(NonEmptyList.semigroup()))
 *
 *  object FailFastStrategy :
 *   Rules<EitherPartialOf<Nel<ValidationError>>>(Either.applicativeError())
 *
 *  companion object {
 *   infix fun <A> failFast(f: FailFastStrategy.() -> A): A = f(FailFastStrategy)
 *   infix fun <A> accumulateErrors(f: ErrorAccumulationStrategy.() -> A): A = f(ErrorAccumulationStrategy)
 *  }
 * }
 * fun main() {
 *  val value =
 * //sampleStart
 *  Rules accumulateErrors {
 *   listOf(
 *    FormField("Invalid Email Domain Label", "nowhere.com"),
 *    FormField("Too Long Email Label", "nowheretoolong${(0..251).map { "g" }}"), //this accumulates N errors
 *    FormField("Valid Email Label", "getlost@nowhere.com")
 *   ).map { it.validateEmail() }
 *  }
 *  //sampleEnd
 *  println(value)
 * }
 * ```
 *  *Fail Fast*
 *
 * ```kotlin:ank:playground
 * import arrow.*
 * import arrow.core.*
 * import arrow.typeclasses.*
 * import arrow.core.extensions.validated.applicativeError.*
 * import arrow.core.extensions.either.applicativeError.*
 * import arrow.core.extensions.nonemptylist.semigroup.*
 *
 * sealed class ValidationError(val msg: String) {
 *  data class DoesNotContain(val value: String) : ValidationError("Did not contain $value")
 *  data class MaxLength(val value: Int) : ValidationError("Exceeded length of $value")
 *  data class NotAnEmail(val reasons: Nel<ValidationError>) : ValidationError("Not a valid email")
 * }
 *
 * data class FormField(val label: String, val value: String)
 * data class Email(val value: String)
 *
 * sealed class Rules<F>(A: ApplicativeError<F, Nel<ValidationError>>) : ApplicativeError<F, Nel<ValidationError>> by A {
 *
 *  private fun FormField.contains(needle: String): Kind<F, FormField> =
 *   if (value.contains(needle, false)) just(this)
 *   else raiseError(ValidationError.DoesNotContain(needle).nel())
 *
 *  private fun FormField.maxLength(maxLength: Int): Kind<F, FormField> =
 *   if (value.length <= maxLength) just(this)
 *   else raiseError(ValidationError.MaxLength(maxLength).nel())
 *
 *  fun FormField.validateEmail(): Kind<F, Email> =
 *   map(contains("@"), maxLength(250), {
 *    Email(value)
 *   }).handleErrorWith { raiseError(ValidationError.NotAnEmail(it).nel()) }
 *
 *  object ErrorAccumulationStrategy :
 *    Rules<ValidatedPartialOf<Nel<ValidationError>>>(Validated.applicativeError(NonEmptyList.semigroup()))
 *
 *  object FailFastStrategy :
 *   Rules<EitherPartialOf<Nel<ValidationError>>>(Either.applicativeError())
 *
 *  companion object {
 *   infix fun <A> failFast(f: FailFastStrategy.() -> A): A = f(FailFastStrategy)
 *   infix fun <A> accumulateErrors(f: ErrorAccumulationStrategy.() -> A): A = f(ErrorAccumulationStrategy)
 *  }
 * }
 * fun main() {
 *  val value =
 * //sampleStart
 *  Rules failFast {
 *   listOf(
 *    FormField("Invalid Email Domain Label", "nowhere.com"),
 *    FormField("Too Long Email Label", "nowheretoolong${(0..251).map { "g" }}"), //this fails fast
 *    FormField("Valid Email Label", "getlost@nowhere.com")
 *   ).map { it.validateEmail() }
 *  }
 * //sampleEnd
 * println(value)
 * }
 * ```
 *
 * ### Supported type classes
 *
 * ```kotlin:ank:replace
 * import arrow.reflect.*
 * import arrow.core.*
 *
 * DataType(Validated::class).tcMarkdownList()
 * ```
 */
@higherkind
sealed class Validated<out E, out A> : ValidatedOf<E, A> {

  companion object {

    fun <E, A> invalidNel(e: E): ValidatedNel<E, A> = Invalid(NonEmptyList(e, listOf()))

    fun <E, A> validNel(a: A): ValidatedNel<E, A> = Valid(a)

    /**
     * Converts a `Try<A>` to a `Validated<Throwable, A>`.
     */
    fun <A> fromTry(t: Try<A>): Validated<Throwable, A> = t.fold({ Invalid(it) }, { Valid(it) })

    /**
     * Converts an `Either<A, B>` to an `Validated<A, B>`.
     */
    fun <E, A> fromEither(e: Either<E, A>): Validated<E, A> = e.fold({ Invalid(it) }, { Valid(it) })

    /**
     * Converts an `Option<B>` to an `Validated<A, B>`, where the provided `ifNone` values is returned on
     * the invalid of the `Validated` when the specified `Option` is `None`.
     */
    fun <E, A> fromOption(o: Option<A>, ifNone: () -> E): Validated<E, A> =
      o.fold(
        { Invalid(ifNone()) },
        { Valid(it) }
      )
  }

  data class Valid<out A>(val a: A) : Validated<Nothing, A>()

  data class Invalid<out E>(val e: E) : Validated<E, Nothing>()

  inline fun <B> fold(fe: (E) -> B, fa: (A) -> B): B =
    when (this) {
      is Valid -> fa(a)
      is Invalid -> (fe(e))
    }

  val isValid =
    fold({ false }, { true })
  val isInvalid =
    fold({ true }, { false })

  /**
   * Is this Valid and matching the given predicate
   */
  fun exist(predicate: (A) -> Boolean): Boolean = fold({ false }, { predicate(it) })

  /**
   * Converts the value to an Either<E, A>
   */
  fun toEither(): Either<E, A> = fold({ Left(it) }, { Right(it) })

  /**
   * Returns Valid values wrapped in Some, and None for Invalid values
   */
  fun toOption(): Option<A> = fold({ None }, { Some(it) })

  /**
   * Convert this value to a single element List if it is Valid,
   * otherwise return an empty List
   */
  fun toList(): List<A> = fold({ listOf() }, { listOf(it) })

  /** Lift the Invalid value into a NonEmptyList. */
  fun toValidatedNel(): ValidatedNel<E, A> =
    fold(
      { invalidNel(it) },
      { Valid(it) }
    )

  /**
   * Convert to an Either, apply a function, convert back. This is handy
   * when you want to use the Monadic properties of the Either type.
   */
  fun <EE, B> withEither(f: (Either<E, A>) -> Either<EE, B>): Validated<EE, B> = fromEither(f(toEither()))

  /**
   * Validated is a [functor.Bifunctor], this method applies one of the
   * given functions.
   */
  fun <EE, AA> bimap(fe: (E) -> EE, fa: (A) -> AA): Validated<EE, AA> = fold({ Invalid(fe(it)) }, { Valid(fa(it)) })

  /**
   * Apply a function to a Valid value, returning a new Valid value
   */
  fun <B> map(f: (A) -> B): Validated<E, B> = bimap(::identity) { f(it) }

  /**
   * Apply a function to an Invalid value, returning a new Invalid value.
   * Or, if the original valid was Valid, return it.
   */
  fun <EE> leftMap(f: (E) -> EE): Validated<EE, A> = bimap({ f(it) }, ::identity)

  /**
   * apply the given function to the value with the given B when
   * valid, otherwise return the given B
   */
  fun <B> foldLeft(b: B, f: (B, A) -> B): B = fold({ b }, { f(b, it) })

  fun <B> foldRight(lb: Eval<B>, f: (A, Eval<B>) -> Eval<B>): Eval<B> =
    when (this) {
      is Valid -> f(this.a, lb)
      is Invalid -> lb
    }

  fun swap(): Validated<A, E> = fold({ Valid(it) }, { Invalid(it) })
}

/**
 * Return the Valid value, or the default if Invalid
 */
fun <E, B> ValidatedOf<E, B>.getOrElse(default: () -> B): B =
  fix().fold({ default() }, ::identity)

/**
 * Return the Valid value, or null if Invalid
 */
fun <E, B> ValidatedOf<E, B>.orNull(): B? =
  getOrElse { null }

/**
 * Return the Valid value, or the result of f if Invalid
 */
fun <E, B> ValidatedOf<E, B>.valueOr(f: (E) -> B): B =
  fix().fold({ f(it) }, ::identity)

/**
 * If `this` is valid return `this`, otherwise if `that` is valid return `that`, otherwise combine the failures.
 * This is similar to [orElse] except that here failures are accumulated.
 */
fun <E, A> ValidatedOf<E, A>.findValid(SE: Semigroup<E>, that: () -> Validated<E, A>): Validated<E, A> =
  fix().fold(

    { e ->
      that().fold(
        { ee -> Invalid(SE.run { e.combine(ee) }) },
        { Valid(it) }
      )
    },
    { Valid(it) }
  )

/**
 * Return this if it is Valid, or else fall back to the given default.
 * The functionality is similar to that of [findValid] except for failure accumulation,
 * where here only the error on the right is preserved and the error on the left is ignored.
 */
fun <E, A> ValidatedOf<E, A>.orElse(default: () -> Validated<E, A>): Validated<E, A> =
  fix().fold(
    { default() },
    { Valid(it) }
  )

/**
 * From Apply:
 * if both the function and this value are Valid, apply the function
 */
fun <E, A, B> ValidatedOf<E, A>.ap(SE: Semigroup<E>, f: Validated<E, (A) -> B>): Validated<E, B> =
  fix().fold(
    { e -> f.fold({ Invalid(SE.run { it.combine(e) }) }, { Invalid(e) }) },
    { a -> f.fold(::Invalid) { Valid(it(a)) } }
  )

fun <E, A> ValidatedOf<E, A>.handleLeftWith(f: (E) -> ValidatedOf<E, A>): Validated<E, A> =
  fix().fold({ f(it).fix() }, ::Valid)

fun <G, E, A, B> ValidatedOf<E, A>.traverse(GA: Applicative<G>, f: (A) -> Kind<G, B>): Kind<G, Validated<E, B>> = GA.run {
  fix().fold({ e -> just(Invalid(e)) }, { a -> f(a).map(::Valid) })
}

fun <G, E, A> ValidatedOf<E, Kind<G, A>>.sequence(GA: Applicative<G>): Kind<G, Validated<E, A>> =
  fix().traverse(GA, ::identity)

fun <E, A> ValidatedOf<E, A>.combine(
  SE: Semigroup<E>,
  SA: Semigroup<A>,
  y: ValidatedOf<E, A>
): Validated<E, A> =
  y.fix().let { that ->
    when {
      this is Valid && that is Valid -> Valid(SA.run { a.combine(that.a) })
      this is Invalid && that is Invalid -> Invalid(SE.run { e.combine(that.e) })
      this is Invalid -> this
      else -> that
    }
  }

fun <E, A> ValidatedOf<E, A>.combineK(SE: Semigroup<E>, y: ValidatedOf<E, A>): Validated<E, A> {
  val xev = fix()
  val yev = y.fix()
  return when (xev) {
    is Valid -> xev
    is Invalid -> when (yev) {
      is Invalid -> Invalid(SE.run { xev.e.combine(yev.e) })
      is Valid -> yev
    }
  }
}

/**
 * Converts the value to an Ior<E, A>
 */
fun <E, A> ValidatedOf<E, A>.toIor(): Ior<E, A> =
  fix().fold({ Ior.Left(it) }, { Ior.Right(it) })

fun <A> A.valid(): Validated<Nothing, A> =
  Valid(this)

fun <E> E.invalid(): Validated<E, Nothing> =
  Invalid(this)

fun <A> A.validNel(): ValidatedNel<Nothing, A> =
  Validated.validNel(this)

fun <E> E.invalidNel(): ValidatedNel<E, Nothing> =
  Validated.invalidNel(this)
