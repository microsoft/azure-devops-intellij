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
      if (targetInformation != null) {
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
      else {
        System.out.println("No target information");
      }
    }
    catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static void printFlags(NtlmMessage message) {
    int flags = message.getFlags();
    // jcifs.ntlmssp.NtlmFlags
    Field[] fields = NtlmFlagsEx.class.getDeclaredFields();
    System.out.println("Flags: 0x" + Integer.toHexString(flags));
    for (Field field : fields) {
      try {
        int flag = field.getInt(NtlmFlags.class);
        if ((flags & flag) != 0) {
          System.out.println("  " + field.getName());
          flags &= ~flag;
        }
      }
      catch (IllegalAccessException e) {
        e.printStackTrace();
      }
    }
    if (flags != 0) {
      System.out.println("  Unknown flag: 0x" + Integer.toHexString(flags));
    }
  }

  private static short readShortLittleEndian(DataInputStream d) throws IOException {
    byte[] w = new byte[2];
    d.readFully(w, 0, 2);
    return (short)((w[1] & 0xff) << 8 | (w[0] & 0xff));
  }

  @SuppressWarnings("UnusedDeclaration")
  private interface NtlmFlagsEx {
    // from http://users.sosdg.org/~qiyong/lxr/source/fs/cifs/ntlmssp.h

    int NTLMSSP_NEGOTIATE_UNICODE = 0x01; /* Text strings are unicode */
    int NTLMSSP_NEGOTIATE_OEM = 0x02; /* Text strings are in OEM */
    int NTLMSSP_REQUEST_TARGET = 0x04; /* Srv returns its auth realm */
    /* define reserved9                       0x08 */
    int NTLMSSP_NEGOTIATE_SIGN = 0x0010; /* Request signing capability */
    int NTLMSSP_NEGOTIATE_SEAL = 0x0020; /* Request confidentiality */
    int NTLMSSP_NEGOTIATE_DGRAM = 0x0040;
    int NTLMSSP_NEGOTIATE_LM_KEY = 0x0080; /* Use LM session key */
    /* defined reserved 8                   0x0100 */
    int NTLMSSP_NEGOTIATE_NTLM = 0x0200; /* NTLM authentication */
    int NTLMSSP_NEGOTIATE_NT_ONLY = 0x0400; /* Lanman not allowed */
    int NTLMSSP_ANONYMOUS = 0x0800;
    int NTLMSSP_NEGOTIATE_DOMAIN_SUPPLIED = 0x1000; /* reserved6 */
    int NTLMSSP_NEGOTIATE_WORKSTATION_SUPPLIED = 0x2000;
    int NTLMSSP_NEGOTIATE_LOCAL_CALL = 0x4000; /* client/server same machine */
    int NTLMSSP_NEGOTIATE_ALWAYS_SIGN = 0x8000; /* Sign. All security levels  */
    int NTLMSSP_TARGET_TYPE_DOMAIN = 0x10000;
    int NTLMSSP_TARGET_TYPE_SERVER = 0x20000;
    int NTLMSSP_TARGET_TYPE_SHARE = 0x40000;
    int NTLMSSP_NEGOTIATE_EXTENDED_SEC = 0x80000; /* NB:not related to NTLMv2 pwd*/
    /* int NTLMSSP_REQUEST_INIT_RESP     0x100000 */
    int NTLMSSP_NEGOTIATE_IDENTIFY = 0x100000;
    int NTLMSSP_REQUEST_ACCEPT_RESP = 0x200000; /* reserved5 */
    int NTLMSSP_REQUEST_NON_NT_KEY = 0x400000;
    int NTLMSSP_NEGOTIATE_TARGET_INFO = 0x800000;
    /* int reserved4                 0x1000000 */
    int NTLMSSP_NEGOTIATE_VERSION = 0x2000000; /* we do not set */
    /* int reserved3                 0x4000000 */
    /* int reserved2                 0x8000000 */
    /* int reserved1                0x10000000 */
    int NTLMSSP_NEGOTIATE_128 = 0x20000000;
    int NTLMSSP_NEGOTIATE_KEY_XCH = 0x40000000;
    int NTLMSSP_NEGOTIATE_56 = 0x80000000;
  }
}
