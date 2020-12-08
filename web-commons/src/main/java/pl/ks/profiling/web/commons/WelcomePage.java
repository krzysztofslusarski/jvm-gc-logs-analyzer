package pl.ks.profiling.web.commons;

import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.Value;
import pl.ks.profiling.gui.commons.Page;

@Value
@Builder
@Getter
public class WelcomePage {
    private List<Page> pages;
}
