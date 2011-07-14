package org.jetbrains.tfsIntegration.core;

import jcifs.ntlmssp.NtlmFlags;
import jcifs.ntlmssp.Type1Message;
import jcifs.util.Base64;

import java.io.IOException;
import java.lang.reflect.Field;

/**
 * @author ksafonov
 */
@SuppressWarnings({"UseOfSystemOutOrSystemErr"})
public class NtlmDecoder {

  public static void main(String[] args) {
    if (args.length != 1) {
      System.out.println("Usage: NtlmDecoder type1_message_text");
    }

    try {
      Type1Message message = new Type1Message(Base64.decode(args[0]));
      Field[] fields = NtlmFlags.class.getDeclaredFields();
      System.out.print("Flags:");
      for (Field field : fields) {
        if (message.getFlag(field.getInt(NtlmFlags.class))) {
          System.out.print(" " + field.getName());
        }
      }
      System.out.println();
      System.out.println("Domain: " + message.getSuppliedDomain());
      System.out.println("Workstation: " + message.getSuppliedWorkstation());
    }
    catch (IOException e) {
      e.printStackTrace();
    }
    catch (IllegalAccessException e) {
      e.printStackTrace();
    }
  }

  private static void printFlag(Type1Message message, int flag, String text) {
    System.out.println(text + ": " + message.getFlag(flag));
  }
}
