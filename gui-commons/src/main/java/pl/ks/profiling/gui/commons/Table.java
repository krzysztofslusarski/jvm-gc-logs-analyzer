package pl.ks.profiling.gui.commons;

import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.Value;

@Getter
@Value
@Builder
public class Table implements PageContent {
    private List<String> header;
    private List<String> footer;
    private List<List<String>> table;
    private String title;
    private String screenWidth;
    private String info;

    @Override
    public ContentType getType() {
        return ContentType.TABLE;
    }

    public String getScreenWidth() {
        if (screenWidth == null) {
            return "100%";
        }
        return screenWidth;
    }
}
