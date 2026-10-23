package com.emr.nfcreader;

import android.app.PendingIntent;
import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.os.Parcelable;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Arrays;

public class MainActivity extends AppCompatActivity {

    private NfcAdapter nfcAdapter;
    private PendingIntent pendingIntent;
    private TextView txtNfcContent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txtNfcContent = findViewById(R.id.txtNfcContent);

        // NFC adaptörünü kontrol et
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (nfcAdapter == null) {
            Toast.makeText(this, "Bu cihaz NFC desteklemiyor.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        if (!nfcAdapter.isEnabled()) {
            Toast.makeText(this, "Lütfen ayarlardan NFC'yi açın.", Toast.LENGTH_LONG).show();
        }

        // Kart okutulduğunda aktivitenin tekrar tetiklenmesi için PendingIntent oluşturulur
        Intent intent = new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE;
        pendingIntent = PendingIntent.getActivity(this, 0, intent, flags);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (nfcAdapter != null) {
            // Ön plan dağıtımını etkinleştir
            nfcAdapter.enableForegroundDispatch(this, pendingIntent, null, null);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (nfcAdapter != null) {
            nfcAdapter.disableForegroundDispatch(this);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleNfcIntent(intent);
    }

    private void handleNfcIntent(Intent intent) {
        String action = intent.getAction();
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action) ||
                NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action) ||
                NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)) {

            // Kartın UID (Benzersiz Kimlik) numarasını al
            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            byte[] tagId = intent.getByteArrayExtra(NfcAdapter.EXTRA_ID);
            String cardUid = bytesToHex(tagId);

            StringBuilder infoBuilder = new StringBuilder("Kart UID: " + cardUid + "\n\n");

            // NDEF (Metin vb.) verilerini kontrol et
            Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            if (rawMsgs != null) {
                NdefMessage[] msgs = new NdefMessage[rawMsgs.length];
                for (int i = 0; i < rawMsgs.length; i++) {
                    msgs[i] = (NdefMessage) rawMsgs[i];
                }
                infoBuilder.append(parseNdefMessages(msgs));
            } else {
                infoBuilder.append("NDEF Verisi Bulunamadı (Sadece kart ID okundu).");
            }

            txtNfcContent.setText(infoBuilder.toString());
        }
    }

    private String parseNdefMessages(NdefMessage[] msgs) {
        if (msgs == null || msgs.length == 0) return "";
        StringBuilder textBuilder = new StringBuilder();

        for (NdefMessage message : msgs) {
            for (NdefRecord record : message.getRecords()) {
                if (record.getTnf() == NdefRecord.TNF_WELL_KNOWN && Arrays.equals(record.getType(), NdefRecord.RTD_TEXT)) {
                    try {
                        byte[] payload = record.getPayload();
                        String textEncoding = ((payload[0] & 128) == 0) ? "UTF-8" : "UTF-16";
                        int languageCodeLength = payload[0] & 63;
                        String text = new String(payload, languageCodeLength + 1, payload.length - languageCodeLength - 1, textEncoding);
                        textBuilder.append("Metin: ").append(text).append("\n");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return textBuilder.toString();
    }

    private String bytesToHex(byte[] bytes) {
        if (bytes == null) return "";
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }
}