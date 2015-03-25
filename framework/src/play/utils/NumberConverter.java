package play.utils;

import java.math.BigInteger;

/**
 * Created by aliev on 25/03/15
 */
public class NumberConverter {

  public static String toStringExt(BigInteger i) {
    return toStringExt(i, digits.length - 1);
  }

  public static String toStringExt(long i) {
    return toStringExt(i, digits.length - 1);
  }

  public static String toStringExt(int i) {
    return toStringExt(i, digits.length - 1);
  }

  public static String toStringExt(long i, long radix) {
    if (radix >= digits.length) throw new IllegalArgumentException("The radix is larger that number of digits.");
    if (i < 0) throw new IllegalArgumentException("Negative number is not supported.");

    char buf[] = new char[33];
    int charPos = 32;
    i = -i;
    while (i <= -radix) {
      buf[charPos--] = digits[(int) -(i % radix)];
      i = i / radix;
    }
    buf[charPos] = digits[(int) -i];

    return new String(buf, charPos, (33 - charPos));
  }

  public static String toStringExt(BigInteger i, long radix) {
    if (radix >= digits.length) throw new IllegalArgumentException("The radix is larger that number of digits.");
    if (i.signum() == -1) throw new IllegalArgumentException("Negative number is not supported.");

    char buf[] = new char[33];
    int charPos = 32;
    i = i.negate();
    while (i.compareTo(new BigInteger(-radix + "")) != 1) {
      BigInteger[] nextStep = i.divideAndRemainder(new BigInteger(radix + ""));
      buf[charPos--] = digits[(int) -(nextStep[1].intValue())];
      i = nextStep[0];
    }
    buf[charPos] = digits[(int) -i.intValue()];

    return new String(buf, charPos, (33 - charPos));
  }

  public static String toStringExt(int i, int radix) {
    if (radix >= digits.length) throw new IllegalArgumentException("The radix is larger that number of digits.");
    if (i < 0) throw new IllegalArgumentException("Negative number is not supported.");

    char buf[] = new char[33];
    int charPos = 32;
    i = -i;
    while (i <= -radix) {
      buf[charPos--] = digits[-(i % radix)];
      i = i / radix;
    }
    buf[charPos] = digits[-i];

    return new String(buf, charPos, (33 - charPos));
  }

  final static char[] digits = {
      '0', '1', '2', '3', '4', '5',
      '6', '7', '8', '9', 'a', 'b',
      'c', 'd', 'e', 'f', 'g', 'h',
      'i', 'j', 'k', 'l', 'm', 'n',
      'o', 'p', 'q', 'r', 's', 't',
      'u', 'v', 'w', 'x', 'y', 'z',

      'A', 'B', 'C', 'D', 'E', 'F',
      'G', 'H', 'I', 'J', 'K', 'M',
      'N', 'O', 'P', 'Q', 'R', 'S',
      'T', 'U', 'V', 'W', 'X', 'Y',
      'Z'

  };

  public static void main(String[] args) {
    System.out.println(toStringExt(Integer.MAX_VALUE, digits.length - 1));
    System.out.println(toStringExt(Long.MAX_VALUE, digits.length - 1));
    System.out.println(toStringExt(Long.MAX_VALUE-1234123, Character.MAX_RADIX));
    System.out.println(Long.MAX_VALUE + "");
    System.out.println(toStringExt(new BigInteger(Long.MAX_VALUE-1234123 + ""), Character.MAX_RADIX));
  }
}
