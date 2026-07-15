package com.warmthdawn.appliedpackaging.mixin.client;

import appeng.client.gui.me.common.MEStorageScreen;
import appeng.client.gui.me.common.Repo;
import appeng.client.gui.widgets.AETextField;
import appeng.client.gui.widgets.Scrollbar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = MEStorageScreen.class, remap = false)
public interface MEStorageScreenAccessor {
    @Accessor("repo")
    Repo appliedpackaging$getRepo();

    @Accessor("searchField")
    AETextField appliedpackaging$getSearchField();

    @Accessor("scrollbar")
    Scrollbar appliedpackaging$getNetworkScrollbar();
}
