package cn.xgpjun.xgpequipmentslot.database;

import cn.xgpjun.xgpequipmentslot.XgpEquipmentSlot;
import cn.xgpjun.xgpequipmentslot.database.hikariCP.DatabaseManager;
import cn.xgpjun.xgpequipmentslot.database.jedis.RedisManager;
import cn.xgpjun.xgpequipmentslot.equipmentSlot.PlayerSlotInfo;
import cn.xgpjun.xgpequipmentslot.utils.ConfigSetting;
import org.bukkit.Bukkit;
import redis.clients.jedis.Jedis;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class DataManager {
    private static DatabaseManager databaseManager;
    private static RedisManager redisManager;

    public static final Map<UUID,Map<String,PlayerSlotInfo>> temp = new HashMap<>();
    public static void init(){
        databaseManager = new DatabaseManager();
        if(ConfigSetting.redis){
            redisManager = new RedisManager();
        }
    }
    public static void savePlayerSlotInfo(PlayerSlotInfo playerSlotInfo){
        if(temp.containsKey(playerSlotInfo.getPlayer())&&Bukkit.getOfflinePlayer(playerSlotInfo.getPlayer()).isOnline()){
            temp.get(playerSlotInfo.getPlayer()).put(playerSlotInfo.getName(),playerSlotInfo);
            return;
        }
        saveToDatabase(playerSlotInfo);
    }

    public static void saveToDatabase(PlayerSlotInfo playerSlotInfo){
        if (ConfigSetting.redis) {
            String playerSlotKey = new KeyConverter(playerSlotInfo.getPlayer(), playerSlotInfo.getName()).toKey();
            try (Jedis redis = redisManager.getJedis()) {
                redis.set(playerSlotKey, playerSlotInfo.toJson());
                redis.expire(playerSlotKey,300L);
            }
            Bukkit.getScheduler().runTaskAsynchronously(XgpEquipmentSlot.getInstance(),
                    ()-> databaseManager.savePlayerSlotInfo(playerSlotInfo));
        }else {
            databaseManager.savePlayerSlotInfo(playerSlotInfo);
        }
    }

    public static PlayerSlotInfo loadPlayerSlotInfo(UUID player,String name){
        if (temp.containsKey(player)){
            if (temp.get(player)!=null){
                return temp.get(player).get(name);
            }
        }
        if (ConfigSetting.redis) {
            String playerSlotKey = new KeyConverter(player,name).toKey();
            try(Jedis redis = redisManager.getJedis()){
                if (redis.exists(playerSlotKey)) {
                    // 如果Redis中存在缓存数据，直接从Redis中获取数据并返回
                    String jsonData = redis.get(playerSlotKey);
                    if(jsonData!=null)
                        return PlayerSlotInfo.fromJson(jsonData, player, name);
                }
            }
        }
        PlayerSlotInfo playerSlotInfo;
        playerSlotInfo = databaseManager.loadPlayerSlotInfo(player, name);

        if (ConfigSetting.redis && playerSlotInfo != null) {
            // 将数据缓存到Redis中，以便之后查询时可以直接从缓存获取
            String playerSlotKey = new KeyConverter(player, name).toKey();
            try (Jedis redis = redisManager.getJedis()) {
                redis.set(playerSlotKey, playerSlotInfo.toJson());
                redis.expire(playerSlotKey,300L);
            }
        }
        if (playerSlotInfo==null){
            playerSlotInfo = new PlayerSlotInfo(name,player,new HashMap<>());
        }
        return playerSlotInfo;
    }

    public static void loadPlayerTemp(UUID uuid){
        temp.remove(uuid);

        Map<String,PlayerSlotInfo> t = new HashMap<>();
        XgpEquipmentSlot.getApi().getEquipmentSlotNames().forEach(s ->{
            t.put(s,loadPlayerSlotInfo(uuid,s));
        });
        temp.put(uuid,t);
    }

    public static void savePlayerTemp(UUID uuid){
        temp.get(uuid).forEach((s, playerSlotInfo) -> {
            saveToDatabase(playerSlotInfo);
        });
        temp.remove(uuid);
    }

    public static Set<UUID> getAllPlayer(){
        return databaseManager.getAllPlayer();
    }
    public static void dropPrimaryKey(){
        databaseManager.dropPrimaryKey();
    }

    public static void close(){
        if(databaseManager!=null)
            databaseManager.close();
        if(redisManager!=null)
            redisManager.close();
    }

}
