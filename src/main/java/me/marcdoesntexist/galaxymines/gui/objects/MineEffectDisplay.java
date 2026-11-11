package me.marcdoesntexist.galaxymines.gui.objects;

import me.marcdoesntexist.galaxymines.api.events.inventory.InventoryObjectClickEvent;
import me.marcdoesntexist.galaxymines.api.utils.inventory.objects.BasicClickableInventoryObject;
import me.marcdoesntexist.galaxymines.api.utils.inventory.objects.interfaces.InventoryObjectClickListener;
import me.marcdoesntexist.galaxymines.api.utils.items.ItemUtils;
import me.marcdoesntexist.galaxymines.compat.Potions;
import me.marcdoesntexist.galaxymines.gui.GUIResourcesUtils;
import me.marcdoesntexist.galaxymines.gui.menus.MineMenu;
import me.marcdoesntexist.galaxymines.messages.Messages;
import me.marcdoesntexist.galaxymines.mines.Mine;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class MineEffectDisplay extends BasicClickableInventoryObject {
    private final Mine mine;
    protected PotionEffect potionEffect;

    public MineEffectDisplay(Mine m, final MineMenu menu) {
        super(GUIResourcesUtils.EMPTY_EFFECT, menu);
        this.mine = m;
        this.interactListener = new InventoryObjectClickListener() {

            @Override
            public void onInteract(InventoryObjectClickEvent e) {
                if (MineEffectDisplay.this.potionEffect == null) {
                    final ItemStack itemInHand = e.getItemInHand();
                    final ItemMeta itemMeta =
                            itemInHand == null ? null : itemInHand.getItemMeta();
                    if (itemMeta instanceof PotionMeta) {
                        final PotionMeta potionMeta = (PotionMeta) itemMeta;
                        PotionEffect effect = null;
                        if (potionMeta.hasCustomEffects()
                                && !potionMeta.getCustomEffects().isEmpty()) {
                            effect = potionMeta.getCustomEffects().get(0);
                        }
                        if (effect != null) {
                            MineEffectDisplay.this.potionEffect =
                                    new PotionEffect(effect.getType(), 21, effect.getAmplifier());
                            MineEffectDisplay.this.mine
                                    .addPotionEffect(MineEffectDisplay.this.potionEffect);
                            menu.update();
                        }
                    }
                } else if (e.getClickType()
                        == InventoryObjectClickEvent.InteractionType.INVENTORY_CLICK_LEFT) {
                    PotionEffect newEffect;
                    PotionEffectType type = MineEffectDisplay.this.potionEffect.getType();
                    int amplifier = MineEffectDisplay.this.potionEffect.getAmplifier();
                    MineEffectDisplay.this.potionEffect = newEffect = amplifier >= 10 ?
                            new PotionEffect(type, MineEffectDisplay.this.potionEffect.getDuration(),
                                    0) :
                            new PotionEffect(type, MineEffectDisplay.this.potionEffect.getDuration(),
                                    amplifier + 1);
                    MineEffectDisplay.this.mine
                            .addPotionEffect(MineEffectDisplay.this.potionEffect);
                    menu.update();
                } else if (e.getClickType()
                        == InventoryObjectClickEvent.InteractionType.INVENTORY_CLICK_RIGHT) {
                    MineEffectDisplay.this.mine
                            .removePotionEffect(MineEffectDisplay.this.potionEffect.getType());
                    MineEffectDisplay.this.potionEffect = null;
                    menu.update();
                }
            }
        };
    }

    private static ItemStack generateIcon(PotionEffect e) {
        ItemStack createItem = ItemUtils.createItem(Material.POTION,
                Messages.get("messages.gui.effectName", "%effect%", "%effect%",
                        Potions.name(e.getType())));
        createItem = ItemUtils.addToLore(createItem,
                Messages.get("messages.gui.effectAmplifier", "Amplifier: %amplifier%",
                        "%amplifier%", e.getAmplifier()));
        return createItem;
    }

    public PotionEffect getPotionEffect() {
        return this.potionEffect;
    }

    public void setPotionEffect(PotionEffect potionEffect) {
        this.potionEffect = potionEffect;
        this.update();
    }

    public Mine getMine() {
        return this.mine;
    }

    @Override
    public void update() {
        this.icon = this.potionEffect != null ?
                MineEffectDisplay.generateIcon(this.potionEffect) :
                GUIResourcesUtils.EMPTY_EFFECT;
    }

}

