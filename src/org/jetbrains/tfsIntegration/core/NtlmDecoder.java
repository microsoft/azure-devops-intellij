package org.jetbrains.tfsIntegration.core;

import jcifs.ntlmssp.NtlmFlags;
import jcifs.ntlmssp.NtlmMessage;
import jcifs.ntlmssp.Type1Message;
import jcifs.ntlmssp.Type2Message;
import jcifs.util.Base64;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.Charset;

/**
 * @author ksafonov
 */
@SuppressWarnings({"UseOfSystemOutOrSystemErr", "CallToPrintStackTrace"})
public class NtlmDecoder {

  public static void main(String[] args) {
    if (args.length != 2) {
      System.out.println("Usage: NtlmDecoder message_type message_text");
    }

    int messageType = Integer.parseInt(args[0]);
    switch (messageType) {
      case 1:
        processMessage1(args[1]);
        break;
      case 2:
        processMessage2(args[1]);
        break;
      default:
        System.out.println("Unknown messsage type, should be 1 or 2");
    }
  }

  private static void processMessage1(String arg) {
    System.out.println("Message 1");
    try {
      Type1Message message = new Type1Message(Base64.decode(arg));
      printFlags(message);
      System.out.println("Domain: " + message.getSuppliedDomain());
      System.out.println("Workstation: " + message.getSuppliedWorkstation());
    }
    catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static void processMessage2(String arg) {
    System.out.println("Message 2");
    try {
      Type2Message message = new Type2Message(Base64.decode(arg));
      printFlags(message);
      byte[] targetInformation = message.getTargetInformation();
      DataInputStream ds = new DataInputStream(new ByteArrayInputStream(targetInformation));
      loop:
      while (true) {
        short type = readShortLittleEndian(ds);
        short length = readShortLittleEndian(ds);
        byte[] contentBuffer = new byte[length];
        ds.readFully(contentBuffer, 0, length);
        // little-endian
        for (int i = 0; i < contentBuffer.length / 2; i++) {
          byte b = contentBuffer[i * 2];
          contentBuffer[i * 2] = contentBuffer[i * 2 + 1];
          contentBuffer[i * 2 + 1] = b;
        }

        String value = new String(contentBuffer, Charset.forName("UTF-16"));
        switch (type) {
          case 1:
            System.out.print("Server name: ");
            break;
          case 2:
            System.out.print("Domain name: ");
            break;
          case 3:
            System.out.print("DNS host name: ");
            break;
          case 4:
            System.out.print("DNS domain name: ");
            break;
          case 0:
            break loop;
          default:
            System.out.print("Unknown (" + type + "): ");
        }
        System.out.println(value);
      }
    }
    catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static void printFlags(NtlmMessage message) {
    int flags = message.getFlags();
    Field[] fields = NtlmFlags.class.getDeclaredFields();
    System.out.println("Flags: 0x" + Integer.toHexString(flags));
    for (Field field : fields) {
      try {
        int flag = field.getInt(NtlmFlags.class);
        if ((flags & flag) != 0) {
          System.out.println(field.getName());
          flags &= ~flag;
        }
      }
      catch (IllegalAccessException e) {
        e.printStackTrace();
      }
    }
    if (flags != 0) {
      System.out.println("Unknown flags: 0x" + Integer.toHexString(flags));
    }
  }

  private static short readShortLittleEndian(DataInputStream d) throws IOException {
    byte[] w = new byte[2];
    d.readFully(w, 0, 2);
    return (short)((w[1] & 0xff) << 8 | (w[0] & 0xff));
  }
}
