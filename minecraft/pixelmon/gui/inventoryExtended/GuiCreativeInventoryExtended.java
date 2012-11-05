package pixelmon.gui.inventoryExtended;

import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import cpw.mods.fml.common.network.PacketDispatcher;
import pixelmon.ServerStorageDisplay;
import pixelmon.comm.EnumPackets;
import pixelmon.comm.PacketCreator;
import pixelmon.comm.PixelmonDataPacket;
import pixelmon.config.PixelmonItems;
import pixelmon.items.ItemHeld;
import pixelmon.storage.PlayerStorage;
import net.minecraft.client.Minecraft;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.GuiButton;
import net.minecraft.src.GuiContainerCreative;
import net.minecraft.src.InventoryPlayer;
import net.minecraft.src.ItemStack;
import net.minecraft.src.RenderHelper;
import net.minecraft.src.ScaledResolution;
import net.minecraft.src.StatCollector;
import net.minecraft.src.Tessellator;

public class GuiCreativeInventoryExtended extends GuiContainerCreative {
	public SlotInventoryPixelmon[] pixelmonSlots;
	
	boolean pixelmonMenuOpen;
	GuiButton pMenuButton;
	PixelmonDataPacket selected;

	float xSize_lo, ySize_lo;

	public GuiCreativeInventoryExtended(EntityPlayer par1EntityPlayer) {
		super(par1EntityPlayer);
		pixelmonMenuOpen = false;
		pixelmonSlots = new SlotInventoryPixelmon[6];
	}

	@Override
	public void initGui() {
		super.initGui();
		ScaledResolution var5 = new ScaledResolution(Minecraft.getMinecraft().gameSettings, Minecraft.getMinecraft().displayWidth,
				Minecraft.getMinecraft().displayHeight);
		int width = var5.getScaledWidth();
		int height = var5.getScaledHeight();
		for (int i = 0; i < pixelmonSlots.length; i++) {
			pixelmonSlots[i] = null;
		}
		for (PixelmonDataPacket p : ServerStorageDisplay.pokemon) {
			int offset = 0;
			if (p != null) {
				int i = p.order;
				int x = width / 2 - 121;
				int y = height / 2 + i * 18 - 65;
				pixelmonSlots[i] = new SlotInventoryPixelmon(x, y, p);
			}
		}
	}

	@Override
	public void drawScreen(int par1, int par2, float par3) {
		super.drawScreen(par1, par2, par3);
		this.xSize_lo = (float) par1;
		this.ySize_lo = (float) par2;
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float par1, int par2, int par3) {
		int var4 = this.mc.renderEngine.getTexture("/gui/inventory.png");
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		this.mc.renderEngine.bindTexture(var4);
		this.drawTexturedModalRect(this.guiLeft, this.guiTop, 0, 0, this.xSize, this.ySize);
		GL11.glEnable(GL12.GL_RESCALE_NORMAL);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, this.mc.renderEngine.getTexture("/pixelmon/gui/pixelmonOverlayExtended2.png"));
		this.drawTexturedModalRect(width / 2 - 130, height / 2 - 83, 0, 0, 46, 167);

		ScaledResolution var5 = new ScaledResolution(Minecraft.getMinecraft().gameSettings, Minecraft.getMinecraft().displayWidth,
				Minecraft.getMinecraft().displayHeight);
		int var6 = var5.getScaledWidth();
		int var7 = var5.getScaledHeight();
		int textureIndex;
		GL11.glDisable(GL11.GL_LIGHTING);
		GL11.glDisable(GL11.GL_FOG);
		Tessellator var2 = Tessellator.instance;
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		Minecraft.getMinecraft().entityRenderer.setupOverlayRendering();

		fontRenderer.setUnicodeFlag(true);

		for (SlotInventoryPixelmon slot : pixelmonSlots) {
			if (slot == null) {
				continue;
			}
			PixelmonDataPacket p = slot.pokemonData;
			int offset = 0;
			if (p != null) {
				String numString = "";
				if (p.nationalPokedexNumber < 10)
					numString = "00" + p.nationalPokedexNumber;
				else if (p.nationalPokedexNumber < 100)
					numString = "0" + p.nationalPokedexNumber;
				else
					numString = "" + p.nationalPokedexNumber;
				int spriteIndex;

				if (p.isShiny)
					spriteIndex = Minecraft.getMinecraft().renderEngine.getTexture("/pixelmon/shinysprites/" + numString + ".png");
				else
					spriteIndex = Minecraft.getMinecraft().renderEngine.getTexture("/pixelmon/sprites/" + numString + ".png");
				drawImageQuad(spriteIndex, slot.x, slot.y, 16f, 16f, 0f, 0f, 1f, 1f);

				if (p.heldItemId != -1) {
					ItemHeld heldItem = (ItemHeld) PixelmonItems.getHeldItem(p.heldItemId);
					if (heldItem != null) {
						spriteIndex = Minecraft.getMinecraft().renderEngine.getTexture("/pixelmon/image/pitems.png");
						int iconIndex = heldItem.getIconIndex(new ItemStack(heldItem));
						int yindex = (int) Math.floor(((double) iconIndex) / 16.0);
						int xindex = iconIndex - yindex * 16;
						drawImageQuad(spriteIndex, slot.heldItemX, slot.heldItemY, 16f, 16f, 16f * xindex / 256f, 16f * yindex / 256f,
								(16f * (xindex + 1)) / 256f, 16f * (yindex + 1) / 256f);
					}
				} else {
					spriteIndex = Minecraft.getMinecraft().renderEngine.getTexture("/pixelmon/image/helditem.png");
					drawImageQuad(spriteIndex, slot.heldItemX + 3, slot.heldItemY + 3, 10f, 10f, 0f, 0f, 1f, 1f);
				}
			}
		}
		int guiIndex = mc.renderEngine.getTexture("/pixelmon/gui/pixelmonOverlayExtended2.png");

		int mouseX = Mouse.getX() * this.width / this.mc.displayWidth;
		int mouseY = this.height - Mouse.getEventY() * this.height / this.mc.displayHeight - 1;
		if (!pixelmonMenuOpen) {
			for (SlotInventoryPixelmon s : pixelmonSlots) {
				if (s != null) {
					if (s.getBounds().contains(mouseX, mouseY)) {
						drawPokemonInfo(mouseX, mouseY, s);
					}
					if (s.getHeldItemBounds().contains(mouseX, mouseY) && heldItemQualifies(s)) {
						drawImageQuad(guiIndex, s.heldItemX - 2, s.heldItemY - 2, 20, 20, 58f / 256f, 185f / 256f, 78f / 256f, 205f / 256f);
					}
				}
			}
		}
		this.fontRenderer.drawString(StatCollector.translateToLocal(PlayerStorage.getCurrency() + ""), -29, 154, 0xFFFFFF);

		fontRenderer.setUnicodeFlag(false);
		RenderHelper.disableStandardItemLighting();

		GL11.glDisable(GL11.GL_LIGHTING);
		GL11.glDepthMask(true);
		GL11.glEnable(GL11.GL_DEPTH_TEST);

	}

	private boolean heldItemQualifies(SlotInventoryPixelmon s) {
		InventoryPlayer inv = mc.thePlayer.inventory;
		if (inv.getItemStack() == null && s.pokemonData.heldItemId != -1)
			return true;
		else if (inv.getItemStack() == null)
			return false;
		if (PixelmonItems.getHeldItem(inv.getItemStack().itemID) != null)
			return true;
		return false;
	}

	private void drawPokemonInfo(int x, int y, SlotInventoryPixelmon s) {
		if (s == null)
			return;
		this.drawGradientRect(s.x - 84, s.y - 2, s.x - 4, s.y + 20, -13158600, -13158600);
		PixelmonDataPacket p = s.pokemonData;
		String displayName = p.name;
		if (!p.nickname.equals(""))
			displayName = p.nickname;
		int var4 = Minecraft.getMinecraft().renderEngine.getTexture("/pixelmon/gui/pixelmonOverlay.png");
		fontRenderer.drawString(displayName, s.x - 82, s.y, 0xFFFFFF);
		Minecraft.getMinecraft().renderEngine.bindTexture(var4);
		if (p.isMale)
			this.drawTexturedModalRect(fontRenderer.getStringWidth(displayName) + s.x - 81, s.y, 33, 208, 5, 9);
		else
			this.drawTexturedModalRect(fontRenderer.getStringWidth(displayName) + s.x - 81, s.y, 33, 218, 5, 9);
		fontRenderer.drawString("Lvl " + p.lvl, s.x + -81, s.y + fontRenderer.FONT_HEIGHT + 1, 0xFFFFFF);
		if (p.health <= 0) {
			fontRenderer.drawString("Fainted", s.x - 77 + fontRenderer.getStringWidth("Lvl " + p.lvl), s.y + fontRenderer.FONT_HEIGHT + 1, 0xFFFFFF);
		} else {
			fontRenderer.drawString("HP " + p.health + "/" + p.hp, s.x - 77 + fontRenderer.getStringWidth("Lvl " + p.lvl), s.y + fontRenderer.FONT_HEIGHT + 1,
					0xFFFFFF);
		}

	}

	private void drawImageQuad(int textureHandle, int x, int y, float w, float h, float us, float vs, float ue, float ve) {
		// activate the specified texture
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureHandle);

		float var7 = 0.00390625F;
		float var8 = 0.00390625F;
		Tessellator var9 = Tessellator.instance;
		var9.startDrawingQuads();
		var9.addVertexWithUV((double) (x + 0), (double) (y + h), (double) this.zLevel, (double) ((float) us), (double) ((float) ve));
		var9.addVertexWithUV((double) (x + w), (double) (y + h), (double) this.zLevel, (double) ((float) ue), (double) ((float) ve));
		var9.addVertexWithUV((double) (x + w), (double) (y + 0), (double) this.zLevel, (double) ((float) ue), (double) ((float) vs));
		var9.addVertexWithUV((double) (x + 0), (double) (y + 0), (double) this.zLevel, (double) ((float) us), (double) ((float) vs));
		var9.draw();
	}

	@Override
	protected void actionPerformed(GuiButton par1GuiButton) {
		super.actionPerformed(par1GuiButton);
		if (par1GuiButton.id == 3) {
			GuiScreenPokeCheckerInv poke = new GuiScreenPokeCheckerInv(selected, this);
			mc.displayGuiScreen(poke);
		}
	}

	@Override
	protected void mouseClicked(int x, int y, int par3) {
		if (par3 == 0) {
			if (pixelmonMenuOpen) {
				controlList.remove(pMenuButton);
				pMenuButton = null;
				pixelmonMenuOpen = false;
				selected = null;
			}
		}
		for (SlotInventoryPixelmon s : pixelmonSlots) {
			if (s != null) {
				if (s.getBounds().contains(x, y)) { // click on a pokemon sprite
					if (par3 == 1) {
						if (pixelmonMenuOpen) {
							controlList.remove(pMenuButton);
							pMenuButton = null;
							pixelmonMenuOpen = false;
							selected = null;
						}
						pMenuButton = new GuiButton(3, x - 50, y, 50, 20, "Summary");
						controlList.add(pMenuButton);
						pixelmonMenuOpen = true;
						selected = s.pokemonData;
						return;
					} else if (par3 == 0) {

					}
				}
				if (s.getHeldItemBounds().contains(x, y) && heldItemQualifies(s)) {
					InventoryPlayer inventory = mc.thePlayer.inventory;
					ItemStack itemStack = inventory.getItemStack();
					if (itemStack != null)
						itemStack.stackSize--;
					if (s.pokemonData.heldItemId == -1) {
						s.pokemonData.heldItemId = itemStack.itemID;
						if (itemStack == null || itemStack.stackSize == 0)
							inventory.setItemStack(null);
						else
							inventory.setItemStack(itemStack);
					} else {

						if (itemStack == null) {
							inventory.setItemStack(new ItemStack(PixelmonItems.getHeldItem(s.pokemonData.heldItemId)));
							s.pokemonData.heldItemId = -1;
						} else if (itemStack.itemID != s.pokemonData.heldItemId) {
							s.pokemonData.heldItemId = itemStack.itemID;
							inventory.setItemStack(itemStack);
						} else {
							s.pokemonData.heldItemId = itemStack.itemID;
							itemStack.stackSize++;
							inventory.setItemStack(itemStack);
						}
					}
					if (itemStack != null)
						PacketDispatcher.sendPacketToServer(PacketCreator.createPacket(EnumPackets.SetHeldItem, s.pokemonData.pokemonID, itemStack.itemID));
					else
						PacketDispatcher.sendPacketToServer(PacketCreator.createPacket(EnumPackets.SetHeldItem, s.pokemonData.pokemonID, -1));
					return;
				}
			}
		}
		if (x > width / 2 - 130 && x < width / 2 - 84 && y > height / 2 - 83 && y < height / 2 + 83)
			return;
		super.mouseClicked(x, y, par3);
	}
}