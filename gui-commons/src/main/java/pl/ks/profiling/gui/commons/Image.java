package pl.ks.profiling.gui.commons;

import lombok.Builder;
import lombok.Getter;
import lombok.Value;

@Getter
@Value
@Builder
public class Image implements PageContent {
    private String title;
    private String info;
    private String name;
    private boolean linkOnly;

    public ContentType getType() {
        return ContentType.IMAGE;
    }
}
