package io.github.mdsimmo.bomberman.commands

import io.github.mdsimmo.bomberman.game.Game
import io.github.mdsimmo.bomberman.game.GameRegistry
import org.bukkit.command.CommandSender

abstract class GameCommand(parent: Cmd) : Cmd(parent) {
    override fun options(sender: CommandSender, args: List<String>): List<String> {
        return if (args.size <= 1) {
            GameRegistry.allGames().map{ it.name }
        } else {
            gameOptions(args.drop(1))
        }
    }

    abstract fun gameOptions(args: List<String>): List<String>

    override fun run(sender: CommandSender, args: List<String>): Boolean {
        return if (args.isEmpty()) {
            false
        } else {
            GameRegistry.byName(args[0]) ?.let {
                gameRun(sender, args.drop(1), it)
                true
            } ?: false
        }
    }

    abstract fun gameRun(sender: CommandSender, args: List<String>, game: Game): Boolean
}