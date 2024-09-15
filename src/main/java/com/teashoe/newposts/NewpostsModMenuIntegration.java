package com.teashoe.newposts;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.autoconfig.AutoConfig;

public class NewpostsModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        // ModMenu에서 설정 화면을 열도록 설정
        return parent -> AutoConfig.getConfigScreen(ModConfig.class, parent).get();
    }
}
