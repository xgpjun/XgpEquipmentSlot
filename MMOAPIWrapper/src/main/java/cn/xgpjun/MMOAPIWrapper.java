package cn.xgpjun;

import cn.xgpjun.xgpequipmentslot.XgpEquipmentSlot;
import cn.xgpjun.xgpequipmentslot.api.AttributeAPIWrapper;
import cn.xgpjun.xgpequipmentslot.api.XESAPI;
import cn.xgpjun.xgpequipmentslot.armorSet.ArmorSet;
import cn.xgpjun.xgpequipmentslot.database.DataManager;
import cn.xgpjun.xgpequipmentslot.equipmentSlot.AttributeManager;
import cn.xgpjun.xgpequipmentslot.equipmentSlot.EquipmentSlot;
import cn.xgpjun.xgpequipmentslot.equipmentSlot.PlayerSlotInfo;
import io.lumine.mythic.lib.MythicLib;
import io.lumine.mythic.lib.api.item.NBTItem;
import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.api.interaction.UseItem;
import net.Indyuce.mmoitems.api.item.mmoitem.MMOItem;
import net.Indyuce.mmoitems.api.player.PlayerData;
import net.Indyuce.mmoitems.api.player.inventory.EquippedItem;
import net.Indyuce.mmoitems.comp.inventory.PlayerInventory;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class MMOAPIWrapper implements AttributeAPIWrapper, PlayerInventory {
    public MMOAPIWrapper(){
        MMOItems.plugin.getInventory().register(this);
    }
    @Override
    public boolean isUse(LivingEntity entity, ItemStack item) {
        //是否为MMOItems的物品
        if(!NBTItem.get(item).hasType()){
            return false;
        }
        NBTItem nbt = MythicLib.plugin.getVersion().getWrapper().getNBTItem(item);
        UseItem useItem = UseItem.getItem((Player) entity,nbt,nbt.getType());
        return useItem.checkItemRequirements();
    }

    @Override
    public void updateAttribute(Player player) {
        Bukkit.getScheduler().runTask(XgpEquipmentSlot.getInstance(),()-> PlayerData.get(player).updateInventory());
        ArmorSet.addPotionEffect(player);
    }

    @Override
    public List<EquippedItem> getInventory(Player player) {
        List<ItemStack> list = new ArrayList<>();
        for(String name: EquipmentSlot.equipmentSlots.keySet()){
            PlayerSlotInfo playerSlotInfo = DataManager.loadPlayerSlotInfo(player.getUniqueId(),name);
            list.addAll(playerSlotInfo.getAllItemStacks());
            for(String item: ArmorSet.getAttributes(playerSlotInfo)){
                //套装效果
                String[] strings = item.split(":",2);
                MMOItem mmoitem = MMOItems.plugin.getMMOItem(MMOItems.plugin.getTypes().get(strings[0]), strings[1]);
                if (mmoitem != null) {
                    list.add(mmoitem.newBuilder().build());
                }
            }
        }
        List<ItemStack> tempItems = new ArrayList<>();
        XESAPI.getTempItems().forEach((s, uuidListMap) -> {
            if(uuidListMap!=null){
                uuidListMap.forEach((uuid, itemStacks) -> {
                    if (player.getUniqueId().equals(uuid)&&itemStacks!=null){
                        tempItems.addAll(itemStacks);
                    }
                });
            }
        });
        list.addAll(tempItems);
        List<EquippedItem> result = new ArrayList<>();
        list.forEach(i-> result.add(new CustomEquippedItem(i)));
        return result;
    }
    public static class CustomEquippedItem extends EquippedItem {
        public CustomEquippedItem(ItemStack itemStack){
            super(itemStack, io.lumine.mythic.lib.api.player.EquipmentSlot.OTHER);
        }

        @Override
        public void setItem(ItemStack itemStack) {
            final ItemStack ref = getNBT().getItem();
            ref.setType(itemStack.getType());
            ref.setAmount(itemStack.getAmount());
            ref.setItemMeta(ref.getItemMeta());
        }
    }

    @Override
    public void register() {
        AttributeManager.setAttributeAPI(this);
        XgpEquipmentSlot.log("load MMOItemsAPI");
    }


    public String getAuthor() {
        return "xgpjun";
    }


    public String getName() {
        return "MMOItems";
    }

    public String getVersion() {
        return "1.0";
    }
}
