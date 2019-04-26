package com.dreamsforge;

import java.util.HashMap;
import java.util.Locale;

/**
 * Encodes arbitrary byte arrays as case-insensitive base-32 strings.
 * <p>
 * The implementation is slightly different than in RFC 4648. During encoding,
 * padding is not added, and during decoding the last incomplete chunk is not
 * taken into account. The result is that multiple strings decode to the same
 * byte array, for example, string of sixteen 7s ("7...7") and seventeen 7s both
 * decode to the same byte array.
 * TODO(sarvar): Revisit this encoding and whether this ambiguity needs fixing.
 *
 * @author sweis@google.com (Steve Weis)
 * @author Neal Gafter
 */
public class Base32String {
  // singleton

  private static final Base32String INSTANCE =
    new Base32String("ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"); // RFC 4648/3548

  private static Base32String getInstance() {
    return INSTANCE;
  }

  private int MASK;
  private int SHIFT;
  private HashMap<Character, Integer> CHAR_MAP;

  private static final String SEPARATOR = "-";

  private Base32String(String alphabet) {
    // 32 alpha-numeric characters.
    char[] DIGITS = alphabet.toCharArray();
    MASK = DIGITS.length - 1;
    SHIFT = Integer.numberOfTrailingZeros(DIGITS.length);
    CHAR_MAP = new HashMap<>();
    for (int i = 0; i < DIGITS.length; i++) {
      CHAR_MAP.put(DIGITS[i], i);
    }
  }

  static byte[] decode(String encoded) throws DecodingException {
    return getInstance().decodeInternal(encoded);
  }

  private byte[] decodeInternal(String encoded) throws DecodingException {
    // Remove whitespace and separators
    encoded = encoded.trim().replaceAll(SEPARATOR, "").replaceAll(" ", "");

    // Remove padding. Note: the padding is used as hint to determine how many
    // bits to decode from the last incomplete chunk (which is commented out
    // below, so this may have been wrong to start with).
    encoded = encoded.replaceFirst("[=]*$", "");

    // Canonicalize to all upper case
    encoded = encoded.toUpperCase(Locale.US);
    if (encoded.length() == 0) {
      return new byte[0];
    }
    int encodedLength = encoded.length();
    int outLength = encodedLength * SHIFT / 8;
    byte[] result = new byte[outLength];
    int buffer = 0;
    int next = 0;
    int bitsLeft = 0;
    for (char c : encoded.toCharArray()) {
      if (!CHAR_MAP.containsKey(c)) {
        throw new DecodingException("Illegal character: " + c);
      }
      buffer <<= SHIFT;
      buffer |= CHAR_MAP.get(c) & MASK;
      bitsLeft += SHIFT;
      if (bitsLeft >= 8) {
        result[next++] = (byte) (buffer >> (bitsLeft - 8));
        bitsLeft -= 8;
      }
    }
    // We'll ignore leftover bits for now.
    //
    // if (next != outLength || bitsLeft >= SHIFT) {
    //  throw new DecodingException("Bits left: " + bitsLeft);
    // }
    return result;
  }

  @Override
  // enforce that this class is a singleton
  public Object clone() throws CloneNotSupportedException {
    throw new CloneNotSupportedException();
  }

  static class DecodingException extends Exception {
    DecodingException(String message) {
      super(message);
    }
  }
}
