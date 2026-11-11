package me.marcdoesntexist.galaxymines.gui.objects;

import me.marcdoesntexist.galaxymines.api.utils.inventory.objects.BasicClickableInventoryObject;
import me.marcdoesntexist.galaxymines.api.utils.inventory.objects.BasicInventoryContainer;
import me.marcdoesntexist.galaxymines.config.MenuConfig;
import me.marcdoesntexist.galaxymines.gui.GUIResourcesUtils;
import me.marcdoesntexist.galaxymines.gui.actions.MenuActionButton;
import me.marcdoesntexist.galaxymines.gui.menus.MineMenu;
import me.marcdoesntexist.galaxymines.mines.Mine;
import org.bukkit.Material;
import org.bukkit.potion.PotionEffect;

import java.util.ArrayList;
import java.util.List;

public class GMinesController extends BasicInventoryContainer {
    protected final Mine mine;
    protected final BasicClickableInventoryObject toggle;
    protected boolean showingMaterial = true;
    protected List<MineMaterialDisplay> materialsDisplays = new ArrayList<MineMaterialDisplay>();
    protected List<MineEffectDisplay> effectsDisplays = new ArrayList<MineEffectDisplay>();

    public GMinesController(MineMenu menu, Mine mine, int pos1, int pos2) {
        super(menu, pos1, pos2);
        this.toggle = new MenuActionButton(
                GUIResourcesUtils.TOGGLE_MATERIALS_EFFECTS_SELECTION,
                MenuConfig.action("mine", "toggle", "toggle_composition"), menu, mine, this,
                MenuConfig.actionArgument("mine", "toggle", null));
        this.mine = mine;
        for (int i = 0; i < 8; ++i) {
            MineMaterialDisplay mmd = new MineMaterialDisplay(mine, menu);
            this.materialsDisplays.add(mmd);
            MineEffectDisplay med = new MineEffectDisplay(mine, menu);
            this.effectsDisplays.add(med);
        }
    }

    public Mine getMine() {
        return this.mine;
    }

    public void toggleDisplay() {
        this.showingMaterial = !this.showingMaterial;
        this.update();
    }

    public boolean isShowingMaterial() {
        return this.showingMaterial;
    }

    @Override
    public void update() {
        this.clear();
        if (this.showingMaterial) {
            List<Material> materials = this.mine.getMaterials();
            int i = 0;
            for (MineMaterialDisplay m : this.materialsDisplays) {
                int index = this.materialsDisplays.indexOf(m);
                if (materials.size() > index) {
                    m.setMaterial(materials.get(index));
                } else {
                    m.setMaterial(null);
                }
                this.setObject(i, m);
                ++i;
            }
        } else {
            List<PotionEffect> effects = this.mine.getEffects();
            int i = 0;
            for (MineEffectDisplay m : this.effectsDisplays) {
                int index = this.effectsDisplays.indexOf(m);
                if (effects.size() > index) {
                    m.setPotionEffect(effects.get(index));
                } else {
                    m.setPotionEffect(null);
                }
                this.setObject(i, m);
                ++i;
            }
        }
        this.setObject(8, this.toggle);
        super.update();
    }

}

