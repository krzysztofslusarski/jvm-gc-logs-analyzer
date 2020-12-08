package pl.ks.profiling.gui.commons;

import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.Value;

@Getter
@Value
@Builder
public class TableWithLinks implements PageContent {
    private List<String> header;
    private List<String> footer;
    private List<List<Link>> table;
    private Integer filteredColumn;
    private String title;
    private String screenWidth;
    private String info;

    @Override
    public ContentType getType() {
        return ContentType.TABLE_WITH_LINKS;
    }

    public String getScreenWidth() {
        if (screenWidth == null) {
            return "100%";
        }
        return screenWidth;
    }

    @Builder
    @Getter
    public static class Link {
        private String href;
        private String description;
        private LinkColor linkColor;

        public static Link of(String description) {
            return Link.builder()
                    .description(description)
                    .build();
        }

        public static Link of(String description, LinkColor linkColor) {
            return Link.builder()
                    .description(description)
                    .linkColor(linkColor)
                    .build();
        }

        public static Link of(String href, String description) {
            return Link.builder()
                    .href(href)
                    .description(description)
                    .build();
        }

        public static Link of(String href, String description, LinkColor linkColor) {
            return Link.builder()
                    .href(href)
                    .description(description)
                    .linkColor(linkColor)
                    .build();
        }
    }

    public enum LinkColor {
        RED,
        GREEN,
    }
}
