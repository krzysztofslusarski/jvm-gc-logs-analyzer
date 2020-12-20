/*
 * Copyright 2020 Artur Owczarek
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

package pl.ks.profiling.safepoint.analyzer.commons.shared;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;

public class PageUtils {
    private static final int ROW_FOR_HEADER = 1;

    public static <T> Object[][] toMatrix(Collection<T> elements, List<String> columnNames, List<Function<T, Object>> valueExtractors) {
        int numberOfColumns = columnNames.size();
        if(numberOfColumns != valueExtractors.size()) {
            throw new IllegalArgumentException("Number of columns(" + numberOfColumns + ") is different than number of extracting functions (" + valueExtractors.size() + ")");
        }

        Object[][] matrix = initiateMatrix(elements, numberOfColumns);
        setHeaders(matrix, columnNames);
        setRows(matrix, elements, valueExtractors);
        return matrix;
    }

    private static <T> Object[][] initiateMatrix(Collection<T> elements, int numberOfColumns) {
        return new Object[elements.size() + ROW_FOR_HEADER][numberOfColumns];
    }

    private static <T> void setRows(Object[][] stats, Collection<T> elements, List<Function<T, Object>> valueExtractors) {
        int itemIndex = 1;
        for (T status : elements) {
            setColumnsValues(stats, valueExtractors, status, itemIndex);
            itemIndex++;
        }
    }

    private static void setHeaders(Object[][] stats, List<String> columnNames) {
        int columnIndex = 0;
        for(String column: columnNames) {
            stats[0][columnIndex] = column;
            columnIndex++;
        }
    }

    private static <T> void setColumnsValues(Object[][]stats, List<Function<T, Object>> valueExtractors, T status, int itemIndex) {
        int columnIndex = 0;
        for (Function<T, Object> valueExtractor : valueExtractors) {
            stats[itemIndex][columnIndex] = valueExtractor.apply(status);
            columnIndex++;
        }
    }
}
