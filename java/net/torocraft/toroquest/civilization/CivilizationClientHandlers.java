package net.torocraft.toroquest.civilization;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.torocraft.toroquest.gui.VillageLordGuiContainer;

public class CivilizationClientHandlers {

	private CivilizationOverlayHandler hud;

	@SubscribeEvent
	public void onPostRenderOverlay(RenderGameOverlayEvent.Pre event) {
		if (event.getType() != ElementType.HOTBAR) {
			return;
		}
		ScaledResolution resolution = event.getResolution();

		if (hud == null) {
			hud = new CivilizationOverlayHandler(Minecraft.getMinecraft());
		}

		hud.render(resolution.getScaledWidth(), resolution.getScaledHeight());
	}

	@SubscribeEvent
	public void onVillageLordInteract(CivilizationEvent.VillageLordInteract event) {
		FMLCommonHandler.instance().showGuiScreen(new VillageLordGuiContainer(event.getEntityPlayer(),
				event.getVillageLord().getInventory(event.getEntityPlayer().getUniqueID()),
				event.getEntityPlayer().getEntityWorld()));
	}
}
