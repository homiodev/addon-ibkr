package org.homio.addon.ibkr;

import lombok.extern.log4j.Log4j2;
import org.homio.api.AddonEntrypoint;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.net.URL;

@Log4j2
@Component
public class IBKREntrypoint implements AddonEntrypoint {

    @Override
    public void init() {
    }

    @Override
    public @NotNull URL getAddonImageURL() {
        return getResource("images/ibkr.png");
    }

}
