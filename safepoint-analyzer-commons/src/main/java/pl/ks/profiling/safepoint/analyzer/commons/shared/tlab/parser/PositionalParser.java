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
