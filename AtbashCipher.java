public class AtbashCipher {

    /**
     * 입력된 문자열에 대해 아트배쉬 변환을 수행합니다.
     * 암호화와 복호화에 동일하게 사용될 수 있습니다.
     *
     * @param text 변환할 원본 문자열
     * @return 아트배쉬 변환이 적용된 문자열
     */
    public static String transform(String text) {
        if (text == null) {
            return null;
        }

        StringBuilder result = new StringBuilder();

        for (char character : text.toCharArray()) {
            if (character >= 'a' && character <= 'z') {
                // 소문자 알파벳인 경우
                char transformedChar = (char) ('z' - character + 'a');
                result.append(transformedChar);
            } else if (character >= 'A' && character <= 'Z') {
                // 대문자 알파벳인 경우
                char transformedChar = (char) ('Z' - character + 'A');
                result.append(transformedChar);
            } else if (character >= '가' && character <= '힣') {
                // 한글 음절인 경우 (U+AC00 ~ U+D7A3)
                char transformedChar = (char) ('힣' - character + '가');
                result.append(transformedChar);
            } else {
                // 알파벳이나 한글이 아닌 경우 (숫자, 공백, 특수문자 등) 그대로 추가
                result.append(character);
            }
        }

        return result.toString();
    }
}