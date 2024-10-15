package com.teashoe.newposts;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;

@Config(name = "newposts")
public class ModConfig implements ConfigData {
    public String galleryId = "steve"; // 기본값
    public boolean useSystemChat = false;
    public boolean showIpAddress = true;

    public static ModConfig get() {
        return AutoConfig.getConfigHolder(ModConfig.class).getConfig();
    }
}
