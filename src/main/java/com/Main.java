package com;

import java.util.List;
import javax.smartcardio.*;

public class Main {
    private static final String mifare1kATR = "3b8f8001804f0ca000000306030001000000006a";
    private static final String mifare4kATR = "3b8f8001804f0ca00000030603ff2000000000b4";
    private Mifare1K k1 = new Mifare1K();
    private Mifare4K k4 = new Mifare4K();

    private final byte ACR_CLA = (byte) 0xFF,
            ACR_BYTE = (byte) 0xD4,
            ACR_BYTE_ACK = (byte) 0xD5,
            RAW_SEND = 0x42,
            RAW_SEND_ACK = 0x43,
            POLL_ACK = 0x4B,
            RESP_BYTE_OK = 0x00;

    private final byte[] ANTENNA_OFF = new byte[] { ACR_BYTE, 0x32, 0x01, 0x00 };
    private final byte[] READER_ID = new byte[] { ACR_CLA, 0x00, 0x48, 0x00, 0x00 };
    private final byte[] TIMEOUT_ONE = new byte[] { ACR_BYTE, 0x32, 0x05, 0x00, 0x00, 0x00 };
    private final byte[] LEVEL4_OFF = new byte[] { ACR_BYTE, 0x12, 0x24 };

    public Main() throws CardException{
        TerminalFactory tf = TerminalFactory.getDefault();
        List< CardTerminal> terminals = tf.terminals().list();
        if(terminals.size() < 1){
            System.err.println("Error: No terminal connected");
            return;
        }


        CardTerminal cardTerminal = terminals.get(0);




        System.out.println("Waiting for card...");
        boolean cardFound = cardTerminal.waitForCardPresent(0);

        Card card = cardTerminal.connect("T=1");
        init(card);
        card.disconnect(true);

        try{ Thread.sleep(2000); } catch (Exception e) { e.printStackTrace(); }

        if(cardFound)
            System.out.println("Card found, moving on...");
        else {
            System.err.println("Error: No card found");
            return;
        }

        cardTerminal.waitForCardPresent(0);
        card = cardTerminal.connect("*");

        String hString = Main.bytesToHex(card.getATR().getHistoricalBytes()).trim();
        System.out.println(hString);

        String atrString = Main.bytesToHex(card.getATR().getBytes()).trim();
        System.out.println(atrString);

        if(atrString.contains(mifare1kATR)){
            if(k1.loadKey(card)){
                if(k1.authenticate(card, 0)){
                    String cardValue = k1.readCard(card);
                    System.out.println(cardValue);

                } else System.err.println("Error: Failed to authenticate to block 0");
            } else System.err.println("Error: Can't load key to reader");
        } else if(atrString.contains(mifare4kATR)){
            if(k4.loadKey(card)){
                if(k4.authenticate(card, 3
                )){
                    String cardValue = k4.readCard(card);
                    System.out.println(cardValue);
                } else System.err.println("Error: Failed to authenticate to block 0");
            } else System.err.println("Card failed to load");
        } else System.err.println("Error: unsupported card");

        card.disconnect(true);
    }

    private void init(Card card){
        transmit(card, ACRcommand(ANTENNA_OFF));
        transmit(card, ACRcommand(TIMEOUT_ONE));
        transmit(card, ACRcommand(LEVEL4_OFF));
    }

    public static String bytesToHex (byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (int i=0; i< bytes.length; i++)
            sb.append(String.format("%02x", bytes[i]));
        return sb.toString();
    }

    private byte[] ACRcommand(byte[] command) {
        byte[] result = new byte[command.length + 5];
        result[0] = ACR_CLA;
        result[4] = (byte) command.length;
        System.arraycopy(command, 0, result, 5, command.length);
        return result;
    }

    public static void main(String[] args) throws CardException {
        new Main();
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
}