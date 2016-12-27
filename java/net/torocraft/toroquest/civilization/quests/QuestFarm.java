package net.torocraft.toroquest.civilization.quests;

import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.BlockEvent.BreakEvent;
import net.minecraftforge.event.world.BlockEvent.PlaceEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.torocraft.toroquest.civilization.CivilizationUtil;
import net.torocraft.toroquest.civilization.Province;
import net.torocraft.toroquest.civilization.player.PlayerCivilizationCapability;
import net.torocraft.toroquest.civilization.player.PlayerCivilizationCapabilityImpl;
import net.torocraft.toroquest.civilization.quests.util.Quest;
import net.torocraft.toroquest.civilization.quests.util.QuestData;
import net.torocraft.toroquest.civilization.quests.util.Quests;

public class QuestFarm implements Quest {

	private static final Block[] CROP_TYPES = { Blocks.CARROTS, Blocks.POTATOES, Blocks.WHEAT, Blocks.MELON_STEM, Blocks.PUMPKIN_STEM, Blocks.BEETROOTS };

	public static QuestFarm INSTANCE;

	public static int ID;

	public static void init(int id) {
		INSTANCE = new QuestFarm();
		Quests.registerQuest(id, INSTANCE);
		MinecraftForge.EVENT_BUS.register(INSTANCE);
		ID = id;
	}

	@SubscribeEvent
	public void onFarm(PlaceEvent event) {
		if (event.getPlayer() == null) {
			return;
		}

		Province provinceFarmedIn = loadProvice(event.getPlayer().worldObj, event.getBlockSnapshot().getPos());

		if (provinceFarmedIn == null || provinceFarmedIn.civilization == null) {
			return;
		}

		handleFarmQuest(event.getPlayer(), provinceFarmedIn, event.getPlacedBlock().getBlock(), true);
	}

	@SubscribeEvent
	public void onHarvest(BreakEvent event) {
		if (event.getPlayer() == null) {
			return;
		}

		Province provinceFarmedIn = loadProvice(event.getPlayer().worldObj, event.getPos());

		if (provinceFarmedIn == null || provinceFarmedIn.civilization == null) {
			return;
		}

		handleFarmQuest(event.getPlayer(), provinceFarmedIn, event.getState().getBlock(), false);
	}

	protected Province loadProvice(World world, BlockPos pos) {
		return CivilizationUtil.getProvinceAt(world, pos.getX() / 16, pos.getZ() / 16);
	}

	private void handleFarmQuest(EntityPlayer player, Province provinceFarmedIn, Block crop, boolean plant) {
		Set<QuestData> quests = PlayerCivilizationCapabilityImpl.get(player).getCurrentQuests();
		DataWrapper quest = new DataWrapper();
		for (QuestData data : quests) {
			quest.setData(data);
			quest.farmedCrop = crop;
			quest.provinceFarmedIn = provinceFarmedIn;
			if (perform(quest, crop, plant)) {
				return;
			}
		}
	}

	public boolean perform(DataWrapper quest, Block farmedCrop, boolean plant) {
		if (quest.getData().getPlayer().worldObj.isRemote) {
			return false;
		}

		if (!quest.isApplicable()) {
			return false;
		}

		if (plant) {
			quest.setCurrentAmount(quest.getCurrentAmount() + 1);
		} else {
			quest.setCurrentAmount(quest.getCurrentAmount() - 1);
		}

		if (quest.getCurrentAmount() >= quest.getTargetAmount()) {
			quest.data.setCompleted(true);
		}

		return true;
	}

	@Override
	public String getTitle(QuestData data) {
		if (data == null) {
			return "";
		}
		DataWrapper q = new DataWrapper().setData(data);
		return "Plant " + q.getTargetAmount() + " " + cropName(q.getCropType()) + " Plants";
	}

	private String cropName(Integer i) {
		if (i == null) {
			return "Crop";
		}
		Block crop = CROP_TYPES[i];
		if (crop == null) {
			System.out.println("invalid crop ID [" + i + "]");
			return "Crop";
		}
		return crop.getLocalizedName();
	}

	@Override
	public String getDescription(QuestData data) {
		if (data == null) {
			return "";
		}
		DataWrapper q = new DataWrapper().setData(data);
		StringBuilder s = new StringBuilder();
		s.append("- Plant ").append(q.getTargetAmount()).append(" ").append(cropName(q.getCropType())).append(" plants.\n");
		s.append("- You have planted ").append(q.getCurrentAmount()).append(" so far.\n");
		s.append("- Reward 2 emeralds\n");
		s.append("- Recieve ").append(q.getRewardRep()).append(" reputation");
		return s.toString();
	}

	@Override
	public QuestData generateQuestFor(EntityPlayer player, Province province) {

		Random rand = player.getEntityWorld().rand;

		DataWrapper q = new DataWrapper();
		q.data.setCiv(province.civilization);
		q.data.setPlayer(player);
		q.data.setProvinceId(province.id);
		q.data.setQuestId(UUID.randomUUID());
		q.data.setQuestType(ID);
		q.data.setCompleted(false);

		int roll = rand.nextInt(100);

		q.setCropType(rand.nextInt(CROP_TYPES.length));
		q.setCurrentAmount(0);
		q.setRewardRep(roll / 20);
		q.setTargetAmount(roll);

		return q.data;
	}

	@Override
	public void reject(QuestData data) {

	}

	@Override
	public List<ItemStack> accept(QuestData data, List<ItemStack> in) {
		return in;
	}

	@Override
	public List<ItemStack> complete(QuestData quest, List<ItemStack> items) {
		if (!quest.getCompleted() || !removeQuestFromPlayer(quest)) {
			return null;
		}

		Province province = loadProvice(quest.getPlayer().worldObj, quest.getPlayer().getPosition());

		if (province == null || !province.id.equals(quest.getProvinceId())) {
			return null;
		}

		PlayerCivilizationCapability playerCiv = PlayerCivilizationCapabilityImpl.get(quest.getPlayer());

		playerCiv.adjustPlayerReputation(quest.getCiv(), new DataWrapper().setData(quest).getRewardRep());

		if (playerCiv.getPlayerReputation(province.civilization) > 100 && quest.getPlayer().worldObj.rand.nextInt(10) > 8) {
			ItemStack hoe = new ItemStack(Items.GOLDEN_HOE);
			hoe.setStackDisplayName("Golden Hoe of " + province.name);
			items.add(hoe);
		}

		ItemStack emeralds = new ItemStack(Items.EMERALD, 2);
		items.add(emeralds);

		return items;
	}

	protected boolean removeQuestFromPlayer(QuestData quest) {
		return PlayerCivilizationCapabilityImpl.get(quest.getPlayer()).removeQuest(quest);
	}

	public static class DataWrapper {
		private QuestData data = new QuestData();
		private Province provinceFarmedIn;
		private Block farmedCrop;

		public QuestData getData() {
			return data;
		}

		public DataWrapper setData(QuestData data) {
			this.data = data;
			return this;
		}

		public Province getProvinceFarmedIn() {
			return provinceFarmedIn;
		}

		public void setProvinceFarmedIn(Province provinceFarmedIn) {
			this.provinceFarmedIn = provinceFarmedIn;
		}

		public Block getFarmedCrop() {
			return farmedCrop;
		}

		public void setFarmedCrop(Block farmedCrop) {
			this.farmedCrop = farmedCrop;
		}

		public Integer getCropType() {
			return i(data.getiData().get("type"));
		}

		public void setCropType(Integer cropType) {
			data.getiData().put("type", cropType);
		}

		public Integer getTargetAmount() {
			return i(data.getiData().get("target"));
		}

		public void setTargetAmount(Integer targetAmount) {
			data.getiData().put("target", targetAmount);
		}

		public Integer getCurrentAmount() {
			return i(data.getiData().get("amount"));
		}

		public void setCurrentAmount(Integer currentAmount) {
			data.getiData().put("amount", currentAmount);
		}

		public Integer getRewardRep() {
			return i(data.getiData().get("rep"));
		}

		public void setRewardRep(Integer rewardRep) {
			data.getiData().put("rep", rewardRep);
		}

		private Integer i(Object o) {
			try {
				return (Integer) o;
			} catch (Exception e) {
				return 0;
			}
		}

		private boolean isApplicable() {
			return isFarmQuest() && isInCorrectProvince() && isCorrectCrop();
		}

		private boolean isFarmQuest() {
			return data.getQuestType() == ID;
		}

		private boolean isInCorrectProvince() {
			return data.getProvinceId().equals(getProvinceFarmedIn().id);
		}

		private boolean isCorrectCrop() {
			return CROP_TYPES[getCropType()] == getFarmedCrop();
		}

	}
}