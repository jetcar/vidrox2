package com.jetcar.vidrox.remote.core

private const val PROTOCOL_PREFIX = "VIDROX/1"
private const val MAX_LINE_LENGTH = 4096
private val pairingCodePattern = Regex("\\d{6}")

enum class RemoteAction(val wireName: String, val requiresArgument: Boolean = false) {
    OPEN("OPEN", true),
    PLAY_PAUSE("PLAY_PAUSE"),
    SEEK_BACK("SEEK_BACK"),
    SEEK_FORWARD("SEEK_FORWARD"),
    BACK("BACK"),
    UP("UP"),
    DOWN("DOWN"),
    LEFT("LEFT"),
    RIGHT("RIGHT"),
    SELECT("SELECT");

    companion object {
        fun fromWireName(value: String): RemoteAction? = entries.firstOrNull { it.wireName == value }
    }
}

data class RemoteCommand(
    val pairingCode: String,
    val action: RemoteAction,
    val argument: String = "",
)

class RemoteProtocolException(message: String) : IllegalArgumentException(message)

object RemoteProtocol {
    const val SERVICE_TYPE = "_vidrox._tcp."
    const val OK_RESPONSE = "OK"

    fun encode(command: RemoteCommand): String {
        validate(command)
        return listOf(PROTOCOL_PREFIX, command.pairingCode, command.action.wireName, command.argument)
            .joinToString("\t")
    }

    fun decode(line: String): RemoteCommand {
        if (line.length > MAX_LINE_LENGTH) throw RemoteProtocolException("command_too_long")
        if ('\n' in line || '\r' in line) throw RemoteProtocolException("invalid_line_break")

        val fields = line.split('\t', limit = 4)
        if (fields.size != 4 || fields[0] != PROTOCOL_PREFIX) {
            throw RemoteProtocolException("unsupported_protocol")
        }

        val action = RemoteAction.fromWireName(fields[2])
            ?: throw RemoteProtocolException("unknown_action")
        return RemoteCommand(fields[1], action, fields[3]).also(::validate)
    }

    fun errorResponse(reason: String): String {
        val safeReason = reason.replace(Regex("[^a-z0-9_]+"), "_").take(80)
        return "ERROR\t${safeReason.ifBlank { "unknown" }}"
    }

    fun isValidPairingCode(code: String): Boolean = pairingCodePattern.matches(code)

    private fun validate(command: RemoteCommand) {
        if (!isValidPairingCode(command.pairingCode)) {
            throw RemoteProtocolException("invalid_pairing_code")
        }
        if ('\t' in command.argument || '\n' in command.argument || '\r' in command.argument) {
            throw RemoteProtocolException("invalid_argument")
        }
        if (command.action.requiresArgument && command.argument.isBlank()) {
            throw RemoteProtocolException("missing_argument")
        }
        if (!command.action.requiresArgument && command.argument.isNotEmpty()) {
            throw RemoteProtocolException("unexpected_argument")
        }
        if (command.action == RemoteAction.OPEN) {
            YouTubeTarget.canonicalWatchUrl(command.argument)
        }
    }
}
