/*
 * Copyright 2020 Krzysztof Slusarski
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package pl.ks.profiling.gui.commons;

import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.Value;

@Getter
@Value
@Builder
public class Table implements PageContent {
    List<String> header;
    List<String> footer;
    List<List<String>> table;
    String title;
    String screenWidth;
    String info;

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
