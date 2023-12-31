package cn.xgpjun;

import cn.xgpjun.xgpequipmentslot.XgpEquipmentSlot;
import cn.xgpjun.xgpequipmentslot.api.AttributeAPIWrapper;
import cn.xgpjun.xgpequipmentslot.api.XESAPI;
import cn.xgpjun.xgpequipmentslot.armorSet.ArmorSet;
import cn.xgpjun.xgpequipmentslot.database.DataManager;
import cn.xgpjun.xgpequipmentslot.equipmentSlot.AttributeManager;
import cn.xgpjun.xgpequipmentslot.equipmentSlot.EquipmentSlot;
import cn.xgpjun.xgpequipmentslot.equipmentSlot.PlayerSlotInfo;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.serverct.ersha.api.AttributeAPI;
import org.serverct.ersha.api.event.attribute.AttrUpdateRuntimeEvent;
import org.serverct.ersha.attribute.data.AttributeSource;

import java.util.ArrayList;
import java.util.List;

public class APAttributeAPIWrapper implements AttributeAPIWrapper, Listener {

    @Override
    public boolean isUse(LivingEntity entity, ItemStack item) {
        return true;
    }

    @Override
    public void updateAttribute(Player player) {
        for(String name: EquipmentSlot.equipmentSlots.keySet()){
            PlayerSlotInfo playerSlotInfo = DataManager.loadPlayerSlotInfo(player.getUniqueId(),name);
            AttributeSource attribute = AttributeAPI.getAttributeSource(new ArrayList<>(playerSlotInfo.getAllItemStacks()),player);
            attribute.merge(AttributeAPI.getAttributeSource(ArmorSet.getAttributes(playerSlotInfo)));
            AttributeAPI.addSourceAttribute(AttributeAPI.getAttrData(player),playerSlotInfo.getName(),attribute);
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
        AttributeSource attribute = AttributeAPI.getAttributeSource(tempItems,player);
        AttributeAPI.addSourceAttribute(AttributeAPI.getAttrData(player),"xes-temp",attribute);
        ArmorSet.addPotionEffect(player);
    }


    public void register() {
        AttributeManager.setAttributeAPI(this);
        XgpEquipmentSlot.log("设置AP为前置！");
    }

    @Override
    public String getAuthor() {
        return "xgpjun";
    }

    @Override
    public String getName() {
        return "AttributePlus";
    }

    @Override
    public String getVersion() {
        return "1.1";
    }

    @EventHandler
    public void apUpdate(AttrUpdateRuntimeEvent e){
        Entity entity = e.getAttributeData().sourceEntity;
        if (entity instanceof Player){
            Bukkit.getScheduler().runTaskAsynchronously(XgpEquipmentSlot.getInstance(),()->AttributeManager.getAttributeAPI().updateAttribute((Player) entity));
        }
    }
}
