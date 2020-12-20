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
package pl.ks.profiling.safepoint.analyzer.commons.shared.tlab.parser;

import pl.ks.profiling.safepoint.analyzer.commons.shared.ParserUtils;

import java.math.BigDecimal;

class PositionalParser {
    private final String line;
    private int position = 0;

    public PositionalParser(String line) {
        this.line = line;
    }

    public void moveAfter(String marker) {
        skipMarker(marker);
        skipWhiteCharactersLeft();
    }

    public long readNumericValue(String parameter) {
        moveAfter(parameter);
        return ParserUtils.parseFirstNumber(this.line, this.position);
    }

    public BigDecimal readPercentValue(String parameter) {
        moveAfter(parameter);
        return ParserUtils.parseFirstBigDecimal(this.line, this.position);
    }

    public String readHexadecimalNumber(String parameter) {
        moveAfter(parameter);
        return ParserUtils.parseFirstHexadecimalNumber(this.line, this.position);
    }

    private void skipMarker(String marker) {
        int markerStart = this.line.indexOf(marker, position);
        this.position = markerStart + marker.length() + 1;
    }

    private void skipWhiteCharactersLeft() {
        int newPosition = this.position;
        while (isWhiteCharacter(this.line, newPosition)) {
            newPosition += 1;
        }
        this.position = newPosition;
    }

    private boolean isWhiteCharacter(String line, int position) {
        char character = line.charAt(position);
        return character <= ' '; // First 0x20 characters in UTF-16 are control characters
    }
}
