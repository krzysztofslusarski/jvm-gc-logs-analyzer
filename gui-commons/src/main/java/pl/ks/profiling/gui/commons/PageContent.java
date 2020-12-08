package pl.ks.profiling.gui.commons;

public interface PageContent {
    ContentType getType();
    String getTitle();
    String getInfo();

    enum ContentType {
        CHART,
        TABLE,
        TABLE_WITH_LINKS,
        IMAGE,
    }
}
