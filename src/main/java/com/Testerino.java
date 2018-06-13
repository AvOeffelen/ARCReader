package com;

import com.sun.deploy.cache.Cache;
import com.sun.deploy.util.ArrayUtil;
import sun.swing.BakedArrayList;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import javax.smartcardio.*;
import javax.xml.bind.DatatypeConverter;

public class Testerino {

//    private final byte[] authBytes = {0xFF, 0x82, 0x00, 0x00, 0x06, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF};
//
    public static void main(String[] args) throws CardException {

        TerminalFactory tf = TerminalFactory.getDefault();
        List< CardTerminal> terminals = tf.terminals().list();
        System.out.println("Available Readers:");
        System.out.println(terminals + "\n");

        Scanner scanner = new Scanner(System.in);

        CardTerminal cardTerminal = (CardTerminal) terminals.get(0);
        Card card = cardTerminal.connect("*");
        ATR atr = card.getATR();
        System.out.println("ATR: "+ atr.toString());

        System.out.println(card);
        CardChannel channel = card.getBasicChannel();


        byte[] authBytes = new byte[]{
                (byte)0xFF,
                (byte)0x82,
                (byte)0x00,
                (byte)0x00,
                (byte)0x06,
                (byte)0xFF,
                (byte)0xFF,
                (byte)0xFF,
                (byte)0xFF,
                (byte)0xFF,
                (byte)0xFF};

        byte[] authCommand = new byte[]{
                (byte)0xFF,
                (byte)0x86,
                (byte)0x00,
                (byte)0x00,
                (byte)0x05,

                (byte)0x01,
                (byte)0x00,
                (byte)0x00,
                (byte)0x61,
                (byte)0x00};

        byte[] readCommand = new byte[]{
                (byte)0xFF,
                (byte)0xB0,
                (byte)0x00,
                (byte)0x00,
                (byte)0x10};

        CommandAPDU command = new CommandAPDU(authBytes);
        channel.transmit(command);
        System.out.println("Authenticating..");

        List<Byte> list = new ArrayList<>();

        int activeSector = 0;

        System.out.println("Reading sectors...");
        outerloop:
        for(int sector = 0; sector < 16; sector++) {
            authCommand[7] = (byte)activeSector;

            CommandAPDU command2 = new CommandAPDU(authCommand);
            ResponseAPDU response2 = channel.transmit(command2);

            for (int i = activeSector; i < activeSector + 3; i++) {
                if(activeSector == 0 && i == 0)
                    continue;


                readCommand[3] = (byte) i;
                CommandAPDU c = new CommandAPDU(readCommand);
                ResponseAPDU r = channel.transmit(c);

                byte[] bytes = r.getBytes();
                bytes = Arrays.copyOf(bytes, bytes.length-2);

                for (byte b : bytes)
                    list.add(b);
            }

            activeSector += 4;
        }

        byte[] result = new byte[list.size()];
        for(int i = 0; i < list.size(); i++) {
            result[i] = list.get(i);
        }

        String str = new String(result);

        int startIndex = str.indexOf("en") + "en".length();
        int endIndex = str.lastIndexOf("}") + 1;

        str = str.substring(startIndex, endIndex);

        System.out.println(str);

        card.disconnect(true);

    }

    public static byte[] concat(byte[] first,byte[] second) {
        byte[] result = Arrays.copyOf(first, first.length + second.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }
}