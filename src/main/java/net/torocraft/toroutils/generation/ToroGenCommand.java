package net.torocraft.toroutils.generation;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.torocraft.torobasemod.entities.EntityMage;
import net.torocraft.torobasemod.generation.MageTowerGenerator;

public class ToroGenCommand extends CommandBase {

	@Override
	public String getCommandName() {
		return "torogen";
	}

	@Override
	public String getCommandUsage(ICommandSender sender) {
		return "commands.torogen.usage";
	}

	@Override
	public int getRequiredPermissionLevel() {
		return 2;
	}

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
		// if(args[0].equals("verdererscabin")){
		// new VerderersCabin(sender.getEntityWorld(),
		// sender.getPosition()).generate();
		// }

		spawnTower(sender);

		// spawnMage(server, sender);
	}

	protected void spawnTower(ICommandSender sender) {
		new MageTowerGenerator().generate(sender.getEntityWorld(), sender.getEntityWorld().rand, sender.getPosition().add(2, -1, 0));
	}

	protected void spawnMage(MinecraftServer server, ICommandSender sender) {
		EntityMage e = new EntityMage(server.getEntityWorld());

		int x = sender.getPosition().getX();
		int y = sender.getPosition().getY();
		int z = sender.getPosition().getZ();

		e.setPosition(x, y, z);
		server.getEntityWorld().spawnEntityInWorld(e);
	}

}
