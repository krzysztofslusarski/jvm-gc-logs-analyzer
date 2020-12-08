package pl.ks.profiling.gui.commons;

import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.Value;

@Getter
@Value
@Builder
public class Page {
    private String menuName;
    private String fullName;
    private Icon icon;
    private String info;
    List<PageContent> pageContents;

    public enum Icon {
        CHART,
        STATS,
        HOME
    }
}
