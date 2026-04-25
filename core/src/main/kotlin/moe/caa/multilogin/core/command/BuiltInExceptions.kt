package moe.caa.multilogin.core.command

import com.mojang.brigadier.LiteralMessage
import com.mojang.brigadier.exceptions.BuiltInExceptionProvider
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType
import moe.caa.multilogin.api.internal.util.Pair
import moe.caa.multilogin.core.main.MultiCore

class BuiltInExceptions(core: MultiCore) : BuiltInExceptionProvider {
    private val DOUBLE_TOO_SMALL: Dynamic2CommandExceptionType
    private val DOUBLE_TOO_BIG: Dynamic2CommandExceptionType
    private val FLOAT_TOO_SMALL: Dynamic2CommandExceptionType
    private val FLOAT_TOO_BIG: Dynamic2CommandExceptionType
    private val INTEGER_TOO_SMALL: Dynamic2CommandExceptionType
    private val INTEGER_TOO_BIG: Dynamic2CommandExceptionType
    private val LONG_TOO_SMALL: Dynamic2CommandExceptionType
    private val LONG_TOO_BIG: Dynamic2CommandExceptionType
    private val LITERAL_INCORRECT: DynamicCommandExceptionType
    private val READER_EXPECTED_START_OF_QUOTE: SimpleCommandExceptionType
    private val READER_EXPECTED_END_OF_QUOTE: SimpleCommandExceptionType
    private val READER_INVALID_ESCAPE: DynamicCommandExceptionType
    private val READER_INVALID_BOOL: DynamicCommandExceptionType
    private val READER_INVALID_INT: DynamicCommandExceptionType
    private val READER_EXPECTED_INT: SimpleCommandExceptionType
    private val READER_INVALID_LONG: DynamicCommandExceptionType
    private val READER_EXPECTED_LONG: SimpleCommandExceptionType
    private val READER_INVALID_DOUBLE: DynamicCommandExceptionType
    private val READER_EXPECTED_DOUBLE: SimpleCommandExceptionType
    private val READER_INVALID_FLOAT: DynamicCommandExceptionType
    private val READER_EXPECTED_FLOAT: SimpleCommandExceptionType
    private val READER_EXPECTED_BOOL: SimpleCommandExceptionType
    private val READER_EXPECTED_SYMBOL: DynamicCommandExceptionType
    private val DISPATCHER_UNKNOWN_COMMAND: SimpleCommandExceptionType
    private val DISPATCHER_UNKNOWN_ARGUMENT: SimpleCommandExceptionType
    private val DISPATCHER_EXPECTED_ARGUMENT_SEPARATOR: SimpleCommandExceptionType
    private val DISPATCHER_PARSE_EXCEPTION: DynamicCommandExceptionType

    // custom
    private val REQUIRE_PLAYER: SimpleCommandExceptionType
    private val PLAYER_NOT_ONLINE: DynamicCommandExceptionType

    private val CACHE_NOT_FOUND_SELF: SimpleCommandExceptionType
    private val NO_SELF: SimpleCommandExceptionType

    private val CACHE_NOT_FOUND_OTHER: Dynamic2CommandExceptionType

    init {
        fun msg(key: String, vararg args: Pair<Any?, Any?>) =
            LiteralMessage(core.languageHandler.getMessage(key, *args))
        DOUBLE_TOO_SMALL = Dynamic2CommandExceptionType { found, min ->
            msg("command_exception_double_too_small", Pair("found", found), Pair("min", min))
        }
        DOUBLE_TOO_BIG = Dynamic2CommandExceptionType { found, max ->
            msg("command_exception_double_too_big", Pair("found", found), Pair("max", max))
        }
        FLOAT_TOO_SMALL = Dynamic2CommandExceptionType { found, min ->
            msg("command_exception_float_too_small", Pair("found", found), Pair("min", min))
        }
        FLOAT_TOO_BIG = Dynamic2CommandExceptionType { found, max ->
            msg("command_exception_float_too_big", Pair("found", found), Pair("max", max))
        }
        INTEGER_TOO_SMALL = Dynamic2CommandExceptionType { found, min ->
            msg("command_exception_integer_too_small", Pair("found", found), Pair("min", min))
        }
        INTEGER_TOO_BIG = Dynamic2CommandExceptionType { found, max ->
            msg("command_exception_integer_too_big", Pair("found", found), Pair("max", max))
        }
        LONG_TOO_SMALL = Dynamic2CommandExceptionType { found, min ->
            msg("command_exception_long_too_small", Pair("found", found), Pair("min", min))
        }
        LONG_TOO_BIG = Dynamic2CommandExceptionType { found, max ->
            msg("command_exception_long_too_big", Pair("found", found), Pair("max", max))
        }
        LITERAL_INCORRECT = DynamicCommandExceptionType { expected ->
            msg("command_exception_literal_incorrect", Pair("expected", expected))
        }
        READER_EXPECTED_START_OF_QUOTE = SimpleCommandExceptionType(
            msg("command_exception_reader_expected_start_of_quote")
        )
        READER_EXPECTED_END_OF_QUOTE = SimpleCommandExceptionType(
            msg("command_exception_reader_expected_end_of_quote")
        )
        READER_INVALID_ESCAPE = DynamicCommandExceptionType { character ->
            msg("command_exception_reader_invalid_escape", Pair("character", character))
        }
        READER_INVALID_BOOL = DynamicCommandExceptionType { value ->
            msg("command_exception_reader_invalid_bool", Pair("value", value))
        }
        READER_INVALID_INT = DynamicCommandExceptionType { value ->
            msg("command_exception_reader_invalid_int", Pair("value", value))
        }
        READER_EXPECTED_INT = SimpleCommandExceptionType(msg("command_exception_reader_expected_int"))
        READER_INVALID_LONG = DynamicCommandExceptionType { value ->
            msg("command_exception_reader_invalid_long", Pair("value", value))
        }
        READER_EXPECTED_LONG = SimpleCommandExceptionType(msg("command_exception_reader_expected_long"))
        READER_INVALID_DOUBLE = DynamicCommandExceptionType { value ->
            msg("command_exception_reader_invalid_double", Pair("value", value))
        }
        READER_EXPECTED_DOUBLE = SimpleCommandExceptionType(msg("command_exception_reader_expected_double"))
        READER_INVALID_FLOAT = DynamicCommandExceptionType { value ->
            msg("command_exception_reader_invalid_float", Pair("value", value))
        }
        READER_EXPECTED_FLOAT = SimpleCommandExceptionType(msg("command_exception_reader_expected_float"))
        READER_EXPECTED_BOOL = SimpleCommandExceptionType(msg("command_exception_reader_expected_bool"))
        READER_EXPECTED_SYMBOL = DynamicCommandExceptionType { symbol ->
            msg("command_exception_reader_expected_symbol", Pair("symbol", symbol))
        }
        DISPATCHER_UNKNOWN_COMMAND = SimpleCommandExceptionType(msg("command_exception_dispatcher_unknown_command"))
        DISPATCHER_UNKNOWN_ARGUMENT = SimpleCommandExceptionType(msg("command_exception_dispatcher_unknown_argument"))
        DISPATCHER_EXPECTED_ARGUMENT_SEPARATOR = SimpleCommandExceptionType(
            msg("command_exception_dispatcher_exception_argument_separator")
        )
        DISPATCHER_PARSE_EXCEPTION = DynamicCommandExceptionType { message ->
            msg("command_exception_dispatcher_parse_exception", Pair("command", message))
        }

        REQUIRE_PLAYER = SimpleCommandExceptionType(msg("command_message_require_player"))
        PLAYER_NOT_ONLINE = DynamicCommandExceptionType { value ->
            msg("command_message_player_not_online", Pair("name", value))
        }
        CACHE_NOT_FOUND_SELF = SimpleCommandExceptionType(msg("command_message_cache_not_found_self"))
        NO_SELF = SimpleCommandExceptionType(msg("command_message_player_no_self"))
        CACHE_NOT_FOUND_OTHER = Dynamic2CommandExceptionType { uuid, name ->
            msg("command_message_cache_not_found_other", Pair("uuid", uuid), Pair("name", name))
        }
    }

    override fun doubleTooLow() = DOUBLE_TOO_SMALL
    override fun doubleTooHigh() = DOUBLE_TOO_BIG
    override fun floatTooLow() = FLOAT_TOO_SMALL
    override fun floatTooHigh() = FLOAT_TOO_BIG
    override fun integerTooLow() = INTEGER_TOO_SMALL
    override fun integerTooHigh() = INTEGER_TOO_BIG
    override fun longTooLow() = LONG_TOO_SMALL
    override fun longTooHigh() = LONG_TOO_BIG
    override fun literalIncorrect() = LITERAL_INCORRECT
    override fun readerExpectedStartOfQuote() = READER_EXPECTED_START_OF_QUOTE
    override fun readerExpectedEndOfQuote() = READER_EXPECTED_END_OF_QUOTE
    override fun readerInvalidEscape() = READER_INVALID_ESCAPE
    override fun readerInvalidBool() = READER_INVALID_BOOL
    override fun readerInvalidInt() = READER_INVALID_INT
    override fun readerExpectedInt() = READER_EXPECTED_INT
    override fun readerInvalidLong() = READER_INVALID_LONG
    override fun readerExpectedLong() = READER_EXPECTED_LONG
    override fun readerInvalidDouble() = READER_INVALID_DOUBLE
    override fun readerExpectedDouble() = READER_EXPECTED_DOUBLE
    override fun readerInvalidFloat() = READER_INVALID_FLOAT
    override fun readerExpectedFloat() = READER_EXPECTED_FLOAT
    override fun readerExpectedBool() = READER_EXPECTED_BOOL
    override fun readerExpectedSymbol() = READER_EXPECTED_SYMBOL
    override fun dispatcherUnknownCommand() = DISPATCHER_UNKNOWN_COMMAND
    override fun dispatcherUnknownArgument() = DISPATCHER_UNKNOWN_ARGUMENT
    override fun dispatcherExpectedArgumentSeparator() = DISPATCHER_EXPECTED_ARGUMENT_SEPARATOR
    override fun dispatcherParseException() = DISPATCHER_PARSE_EXCEPTION

    fun requirePlayer() = REQUIRE_PLAYER
    fun playerNotOnline() = PLAYER_NOT_ONLINE
    fun noSelf() = NO_SELF
    fun cacheNotFoundSelf() = CACHE_NOT_FOUND_SELF
    fun cacheNotFoundOther() = CACHE_NOT_FOUND_OTHER
}
