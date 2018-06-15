package com;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.smartcardio.ATR;
import javax.smartcardio.Card;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;

public class Mifare4K {

    private final byte[] authCommand = new byte[]{ (byte)0xFF, (byte)0x86, (byte)0x00, (byte)0x00, (byte)0x05, (byte)0x01, (byte)0x00, (byte)0x00, (byte)0x61, (byte)0x00};
    private final byte[] readCommand = new byte[]{ (byte)0xFF, (byte)0xB0, (byte)0x00, (byte)0x00, (byte)0x10};

    private final byte[] testCommand = new byte[]{ (byte)0xFF, (byte)0xCA, (byte)0x00, (byte)0x00, (byte)0x00};

    private final byte[] loadKeyCommand = new byte[]{ (byte)0xFF, (byte)0x82, (byte)0x00, (byte)0x00, (byte)0x06, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF};

    public boolean loadKey(Card card){
        ResponseAPDU response2 = transmit(card, loadKeyCommand);
        return isSuccess(response2);
    }

    public boolean authenticate(Card card, int block){
        byte[] auth = authCommand;
        auth[7] = (byte)block;
        ResponseAPDU response = transmit(card, auth);
        return isSuccess(response);
    }

    public String readCard(Card card){
        List<Byte> list = new ArrayList<>();
        byte[] auth = authCommand;
        int activeSector = 0;

        for(int sector = 0; sector < 16; sector++) {
            auth[7] = (byte)activeSector;
            transmit(card, auth);

            for (int i = activeSector; i < activeSector + 3; i++) {
                if(activeSector == 0 && i == 0)
                    continue;

                readCommand[3] = (byte) i;
                ResponseAPDU r = transmit(card, readCommand);

                byte[] bytes = r.getBytes();
                bytes = Arrays.copyOf(bytes, bytes.length-2);

                for (byte b : bytes)
                    list.add(b);
            }
            activeSector += 4;
        }

        byte[] result = new byte[list.size()];
        for(int i = 0; i < list.size(); i++)
            result[i] = list.get(i);

        String str = new String(result);

        int startIndex = str.indexOf("en") + "en".length();
        int endIndex = str.lastIndexOf("}") + 1;
        str = str.substring(startIndex, endIndex);

        return str;
    }

    private ResponseAPDU transmit(Card card, byte[] commandBytes){
        ResponseAPDU response = null;
        CommandAPDU command = new CommandAPDU(commandBytes);
        try{
            response = card.getBasicChannel().transmit(command);
        } catch (Exception e){
            e.printStackTrace();
        }
        return response;
    }

    private boolean isSuccess(ResponseAPDU r){
        if(r == null)
            return false;
        String responseString = Main.bytesToHex(r.getBytes()).trim();
        System.out.println(responseString);
        if(responseString.equals("9000"))
            return true;
        return false;
    }
}
