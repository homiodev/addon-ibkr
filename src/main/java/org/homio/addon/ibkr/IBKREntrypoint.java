package org.homio.addon.ibkr;

import lombok.extern.log4j.Log4j2;
import org.homio.api.AddonConfiguration;
import org.homio.api.AddonEntrypoint;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.Objects;

@Log4j2
@Component
@AddonConfiguration
public class IBKREntrypoint implements AddonEntrypoint {

    @Override
    public void init() {
    }

    @Override
    public @NotNull URL getAddonImageURL() {
        return Objects.requireNonNull(getResource("images/ibkr.png"));
    }

}
