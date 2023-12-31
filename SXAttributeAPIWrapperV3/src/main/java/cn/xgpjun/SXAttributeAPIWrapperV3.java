package cn.xgpjun;

import cn.xgpjun.xgpequipmentslot.XgpEquipmentSlot;
import cn.xgpjun.xgpequipmentslot.api.AttributeAPIWrapper;
import cn.xgpjun.xgpequipmentslot.api.XESAPI;
import cn.xgpjun.xgpequipmentslot.armorSet.ArmorSet;
import cn.xgpjun.xgpequipmentslot.database.DataManager;
import cn.xgpjun.xgpequipmentslot.equipmentSlot.EquipmentSlot;
import cn.xgpjun.xgpequipmentslot.equipmentSlot.PlayerSlotInfo;
import github.saukiya.sxattribute.SXAttribute;
import github.saukiya.sxattribute.api.SXAPI;
import github.saukiya.sxattribute.data.PreLoadItem;
import github.saukiya.sxattribute.data.attribute.SXAttributeData;
import github.saukiya.zfrunes.data.StatsDataManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SXAttributeAPIWrapperV3 implements AttributeAPIWrapper {
    private final SXAPI sxapi;
    public SXAttributeAPIWrapperV3() {
        sxapi = SXAttribute.getApi();
        ZF_Ruins = Bukkit.getPluginManager().getPlugin("ZF-Runes")!=null;
    }
    protected boolean ZF_Ruins;
    protected SXAttributeData getAttribute(Player player,String name){
        PlayerSlotInfo playerSlotInfo = DataManager.loadPlayerSlotInfo(player.getUniqueId(),name);
        SXAttributeData data = new SXAttributeData();
        for (ItemStack itemStack: playerSlotInfo.getAllItemStacks()){
            try{
                if(!isUse(player,itemStack))
                    continue;
            }catch (Exception ignore){
            }
            data.add(loadItemData(player,itemStack));
            if(ZF_Ruins){
                data.add(StatsDataManager.getItemData(itemStack));
            }
        }
        data.add(loadListData(player,ArmorSet.getAttributes(playerSlotInfo)));
        return data;
    }
    protected SXAttributeData getTempAttribute(Player player){
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
        SXAttributeData data = new SXAttributeData();
        for (ItemStack itemStack: tempItems){
            try{
                if(!isUse(player,itemStack))
                    continue;
            }catch (Exception ignore){
            }
            data.add(loadItemData(player,itemStack));
            if(ZF_Ruins){
                data.add(StatsDataManager.getItemData(itemStack));
            }
        }
        return data;
    }

    public void removeEntityAPIData(UUID playerUUID) {
        sxapi.removeEntityAPIData(XgpEquipmentSlot.class, playerUUID);
    }


    public void setEntityAPIData(UUID playerUUID, SXAttributeData data) {
        sxapi.setEntityAPIData(XgpEquipmentSlot.class,playerUUID,data);
    }


    public void attributeUpdate(Player player) {
        sxapi.attributeUpdate(player);
    }

    @Override
    public boolean isUse(LivingEntity entity, ItemStack item){
        return sxapi.isUse(entity,new PreLoadItem(item),item.getItemMeta().getLore());
    }


    public SXAttributeData loadItemData(Player player, ItemStack item) {
        return sxapi.loadItemData(player,new PreLoadItem(item));
    }

    public SXAttributeData loadListData(Player player, List<String> list) {
        return sxapi.loadListData(list);
    }
    @Override
    public void updateAttribute(Player player) {
        removeEntityAPIData(player.getUniqueId());
        SXAttributeData data = new SXAttributeData();
        for(String name: EquipmentSlot.equipmentSlots.keySet()){
            data.add(getAttribute(player,name));
        }
        data.add(getTempAttribute(player));
        setEntityAPIData(player.getUniqueId(),data);
        attributeUpdate(player);
        ArmorSet.addPotionEffect(player);
    }
    @Override
    public void register() {
        XgpEquipmentSlot.getApi().setAttributeAPI(this);
        XgpEquipmentSlot.log("设置SX3.X为前置！");
    }
    @Override
    public String getAuthor() {
        return "xgpjun";
    }

    @Override
    public String getName() {
        return "SX(3.X)";
    }

    @Override
    public String getVersion() {
        return "1.0";
    }
}
